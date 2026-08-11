package com.tcs.module.finance.service.impl;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CenterRequestFeeServiceImpl implements CenterRequestFeeService {

    private static final String BANK_NAME = "TPBank";
    private static final String BANK_BIN = "970423";
    private static final String ACCOUNT_NUMBER = "02660559201";
    private static final String ACCOUNT_NAME = "TUTOR CONNECT SYSTEM";
    private static final BigDecimal FEE_RATE = new BigDecimal("0.02");

    private final CenterRequestFeeHoldRepository feeHoldRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final WalletService walletService;
    private final ClassRequestStore classRequestStore;
    private final UserRepository userRepository;
    private final com.tcs.module.marketplace.repository.ClassStudentRepository classStudentRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PaymentNotificationService paymentNotificationService;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public CenterRequestFeePaymentResponse createPayment(
            String requestId,
            Long clientUserId,
            Long centerUserId,
            String centerName,
            BigDecimal projectedEscrowAmount,
            RefundPayoutInfo payoutInfo) {

        if (!StringUtils.hasText(requestId) || clientUserId == null || centerUserId == null) {
            throw new BusinessException("Không xác định được thông tin yêu cầu/trung tâm để tạo phí xử lý");
        }

        CenterRequestFeeHold existing = feeHoldRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        RefundPayoutInfo resolvedPayoutInfo = normalizePayoutInfo(payoutInfo);
        BigDecimal baseAmount = amountOrZero(projectedEscrowAmount);
        BigDecimal feeAmount = baseAmount.multiply(FEE_RATE).setScale(0, RoundingMode.HALF_UP);
        if (feeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            feeAmount = BigDecimal.ONE;
        }

        String referenceCode = "CENTERREQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PaymentTransaction payment = new PaymentTransaction();
        payment.setWallet(walletService.getSystemEscrowWallet());
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.PENDING);
        payment.setAmount(feeAmount);
        payment.setDescription("Chờ thanh toán phí xử lý yêu cầu trung tâm");
        payment.setReferenceCode(referenceCode);
        payment = paymentTransactionRepository.save(payment);

        CenterRequestFeeHold hold = new CenterRequestFeeHold();
        hold.setRequestId(requestId);
        hold.setClientUserId(clientUserId);
        hold.setCenterUserId(centerUserId);
        hold.setCenterName(centerName);
        hold.setPaymentTransaction(payment);
        hold.setProjectedEscrowAmount(baseAmount);
        hold.setAmount(feeAmount);
        hold.setReferenceCode(referenceCode);
        hold.setPayoutBankName(resolvedPayoutInfo.bankName());
        hold.setPayoutAccountNo(resolvedPayoutInfo.accountNo());
        hold.setPayoutAccountHolderName(resolvedPayoutInfo.accountHolderName());
        hold.setStatus(CenterRequestFeeStatus.PENDING_PAYMENT);
        hold = feeHoldRepository.save(hold);

        notifyClientPaymentReady(hold);
        return toResponse(hold);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CenterRequestFeePaymentResponse> getPayment(String requestId) {
        return feeHoldRepository.findByRequestId(requestId).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCenterRequestFeePayment(PaymentTransaction tx) {
        return tx != null
                && tx.getType() == PaymentTransactionType.ESCROW_DEPOSIT
                && feeHoldRepository.findByPaymentTransaction_TransactionId(tx.getTransactionId()).isPresent();
    }

    @Override
    @Transactional
    public CenterRequestFeePaymentResponse completeIncomingPayment(
            PaymentTransaction tx, String externalTransactionId) {
        CenterRequestFeeHold hold = feeHoldRepository.findByPaymentTransaction_TransactionId(tx.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phí xử lý yêu cầu trung tâm"));
        if (tx.getStatus() == PaymentTransactionStatus.SUCCESS
                && hold.getStatus() == CenterRequestFeeStatus.HELD) {
            return toResponse(hold);
        }
        if (tx.getStatus() != PaymentTransactionStatus.PENDING
                || hold.getStatus() != CenterRequestFeeStatus.PENDING_PAYMENT) {
            throw new BusinessException("Giao dịch phí trung tâm không còn ở trạng thái chờ thanh toán");
        }

        LocalDateTime now = LocalDateTime.now();
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setExternalTransactionId(externalTransactionId);
        tx.setProcessedAt(now);
        tx.setDescription("Đã ghi nhận phí xử lý yêu cầu trung tâm qua SePay");
        paymentTransactionRepository.save(tx);

        hold.setStatus(CenterRequestFeeStatus.HELD);
        hold.setPaidAt(now);
        feeHoldRepository.save(hold);

        classRequestStore.find(hold.getRequestId()).ifPresent(data ->
                classRequestStore.save(classRequestStore.withStatus(
                        data,
                        ClassRequestStore.STATUS_PENDING,
                        null)));

        notifyClientPaymentSuccess(hold);
        notifyCenterPaymentSuccess(hold);
        return toResponse(hold);
    }

    @Override
    @Transactional
    public void linkFulfilledAssignment(String requestId, Long classId, Long assignmentId) {
        CenterRequestFeeHold hold = requireHold(requestId);
        hold.setClassId(classId);
        hold.setAssignmentId(assignmentId);
        feeHoldRepository.save(hold);
    }

    @Override
    @Transactional
    public void releaseForRequest(String requestId, String reason) {
        CenterRequestFeeHold hold = requireHold(requestId);
        if (hold.getStatus() == CenterRequestFeeStatus.RELEASED
                || hold.getStatus() == CenterRequestFeeStatus.REFUNDED) {
            return;
        }
        if (hold.getStatus() != CenterRequestFeeStatus.HELD) {
            throw new BusinessException("Phí xử lý yêu cầu chưa sẵn sàng để giải ngân");
        }
        releaseInternal(hold, reason);
    }

    @Override
    @Transactional
    public void releaseForFulfilledAssignment(Long assignmentId, String reason) {
        CenterRequestFeeHold hold = feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phí xử lý yêu cầu trung tâm"));
        if (hold.getStatus() == CenterRequestFeeStatus.RELEASED
                || hold.getStatus() == CenterRequestFeeStatus.REFUNDED) {
            return;
        }
        if (hold.getStatus() != CenterRequestFeeStatus.HELD) {
            throw new BusinessException("Phí xử lý yêu cầu chưa sẵn sàng để giải ngân");
        }
        releaseInternal(hold, reason);
    }

    @Override
    @Transactional
    public void requestRefund(String requestId, String reason) {
        CenterRequestFeeHold hold = requireHold(requestId);
        if (hold.getStatus() == CenterRequestFeeStatus.PENDING_PAYMENT) {
            cancelUnpaid(requestId);
            return;
        }
        if (hold.getStatus() == CenterRequestFeeStatus.RELEASED
                || hold.getStatus() == CenterRequestFeeStatus.REFUNDED) {
            return;
        }
        if (refundRequestRepository.findFirstByCenterRequestFeeHold_FeeHoldIdOrderByRequestedAtDesc(
                hold.getFeeHoldId())
                .filter(r -> r.getStatus() == RefundRequestStatus.PENDING)
                .isPresent()) {
            return;
        }

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setCenterRequestFeeHold(hold);
        refundRequest.setRequestedBy(resolveCenterUser(hold.getCenterUserId()));
        refundRequest.setReason(RefundPayoutInfoCodec.appendToReason(
                buildReason(reason),
                new RefundPayoutInfo(
                        hold.getPayoutBankName(),
                        hold.getPayoutAccountNo(),
                        hold.getPayoutAccountHolderName())));
        refundRequest.setAmount(hold.getAmount());
        refundRequest.setBankName(hold.getPayoutBankName());
        refundRequest.setAccountNo(hold.getPayoutAccountNo());
        refundRequest.setRefundReferenceCode("REFUND-CREQFEE-" + hold.getFeeHoldId());
        refundRequest.setTransferStatus("PENDING");
        refundRequest.setStatus(RefundRequestStatus.PENDING);
        refundRequest.setRequestedAt(LocalDateTime.now());
        refundRequestRepository.save(refundRequest);

        hold.setStatus(CenterRequestFeeStatus.REFUND_REQUESTED);
        feeHoldRepository.save(hold);

        notifyRefundAdmins(hold, refundRequest);
        notifyClientRefundRequested(hold, refundRequest);
    }

    @Override
    @Transactional
    public void cancelUnpaid(String requestId) {
        CenterRequestFeeHold hold = requireHold(requestId);
        if (hold.getStatus() != CenterRequestFeeStatus.PENDING_PAYMENT) {
            throw new BusinessException("Chỉ có thể hủy khi phí trung tâm chưa thanh toán");
        }
        PaymentTransaction tx = hold.getPaymentTransaction();
        if (tx != null && tx.getStatus() == PaymentTransactionStatus.PENDING) {
            tx.setStatus(PaymentTransactionStatus.CANCELLED);
            tx.setProcessedAt(LocalDateTime.now());
            paymentTransactionRepository.save(tx);
        }
        hold.setStatus(CenterRequestFeeStatus.CANCELLED);
        feeHoldRepository.save(hold);
        classRequestStore.delete(requestId);
    }

    private CenterRequestFeeHold requireHold(String requestId) {
        return feeHoldRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phí xử lý yêu cầu trung tâm"));
    }

    private void releaseInternal(CenterRequestFeeHold hold, String reason) {
        LocalDateTime now = LocalDateTime.now();
        Wallet centerWallet = walletService.getOrCreate(hold.getCenterUserId());
        walletService.credit(
                hold.getCenterUserId(),
                hold.getAmount(),
                "CENTERREQ_RELEASE-" + hold.getFeeHoldId());

        PaymentTransaction releaseTx = new PaymentTransaction();
        releaseTx.setWallet(centerWallet);
        releaseTx.setType(PaymentTransactionType.ESCROW_RELEASE);
        releaseTx.setStatus(PaymentTransactionStatus.SUCCESS);
        releaseTx.setAmount(hold.getAmount());
        releaseTx.setDescription(buildReason(reason));
        releaseTx.setReferenceCode("CENTERREQ_RELEASE-" + hold.getFeeHoldId());
        releaseTx.setProcessedAt(now);
        paymentTransactionRepository.save(releaseTx);

        hold.setStatus(CenterRequestFeeStatus.RELEASED);
        hold.setReleasedAt(now);
        feeHoldRepository.save(hold);

        notifyClientReleased(hold);
        notifyCenterReleased(hold);
    }

    private RefundPayoutInfo normalizePayoutInfo(RefundPayoutInfo payoutInfo) {
        RefundPayoutInfo info = payoutInfo != null && RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)
                ? new RefundPayoutInfo(
                        RefundPayoutInfoCodec.normalize(payoutInfo.bankName()),
                        RefundPayoutInfoCodec.normalizeAccountNo(payoutInfo.accountNo()),
                        RefundPayoutInfoCodec.normalize(payoutInfo.accountHolderName()))
                : null;
        if (!RefundPayoutInfoCodec.hasCompletePayout(info)) {
            throw new BusinessException("Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản");
        }
        return info;
    }

    private CenterRequestFeePaymentResponse toResponse(CenterRequestFeeHold hold) {
        if (hold == null) {
            return null;
        }
        PaymentTransaction payment = hold.getPaymentTransaction();
        BigDecimal amount = hold.getAmount();
        String reference = hold.getReferenceCode();
        return CenterRequestFeePaymentResponse.builder()
                .requestId(hold.getRequestId())
                .feeHoldId(hold.getFeeHoldId())
                .status(hold.getStatus())
                .amount(amount)
                .referenceCode(reference)
                .bankName(BANK_NAME)
                .bankBin(BANK_BIN)
                .accountNumber(ACCOUNT_NUMBER)
                .accountName(ACCOUNT_NAME)
                .transferContent(reference)
                .qrUrl(buildQrUrl(amount, reference))
                .classId(hold.getClassId())
                .assignmentId(hold.getAssignmentId())
                .payoutBankName(hold.getPayoutBankName())
                .payoutAccountNoMasked(RefundPayoutInfoCodec.maskAccountNo(hold.getPayoutAccountNo()))
                .payoutAccountHolderName(hold.getPayoutAccountHolderName())
                .paidAt(hold.getPaidAt())
                .releasedAt(hold.getReleasedAt())
                .refundedAt(hold.getRefundedAt())
                .build();
    }

    private String buildQrUrl(BigDecimal amount, String transferContent) {
        return "https://img.vietqr.io/image/"
                + BANK_BIN
                + "-"
                + ACCOUNT_NUMBER
                + "-compact2.png"
                + "?amount="
                + amount.setScale(0, RoundingMode.DOWN).toPlainString()
                + "&addInfo="
                + java.net.URLEncoder.encode(transferContent, java.nio.charset.StandardCharsets.UTF_8)
                + "&accountName="
                + java.net.URLEncoder.encode(ACCOUNT_NAME, java.nio.charset.StandardCharsets.UTF_8);
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private User resolveCenterUser(Long centerUserId) {
        return userRepository.findById(centerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản trung tâm"));
    }

    private void notifyClientPaymentReady(CenterRequestFeeHold hold) {
        paymentNotificationService.notifyPayment(
                hold.getClientUserId(),
                "Phí xử lý yêu cầu đã sẵn sàng",
                "Vui lòng chuyển " + hold.getAmount().setScale(0, RoundingMode.DOWN).toPlainString()
                        + " đ để gửi yêu cầu tới trung tâm.",
                "CLASS_REQUEST_FEE",
                hold.getFeeHoldId());
    }

    private void notifyClientPaymentSuccess(CenterRequestFeeHold hold) {
        paymentNotificationService.notifyPayment(
                hold.getClientUserId(),
                "Thanh toán phí yêu cầu thành công",
                "Yêu cầu của bạn đã được ghi nhận và đang chờ trung tâm xử lý.",
                "CLASS_REQUEST_FEE",
                hold.getFeeHoldId());
    }

    private void notifyCenterPaymentSuccess(CenterRequestFeeHold hold) {
        paymentNotificationService.notifyPayment(
                hold.getCenterUserId(),
                "Có yêu cầu mới đã thanh toán phí",
                "Yêu cầu #" + hold.getRequestId() + " đã được khách hàng thanh toán phí xử lý.",
                "CLASS_REQUEST_FEE",
                hold.getFeeHoldId());
    }

    private void notifyRefundAdmins(CenterRequestFeeHold hold, RefundRequest refundRequest) {
        for (var admin : platformAdminRepository.findAll()) {
            if (admin == null || admin.getUser() == null) {
                continue;
            }
            paymentNotificationService.notifyPayment(
                    admin.getUser(),
                    "Có yêu cầu hoàn phí trung tâm mới",
                    "Yêu cầu hoàn " + hold.getAmount().setScale(0, RoundingMode.DOWN).toPlainString()
                            + " đ cho yêu cầu #" + hold.getRequestId() + " đang chờ xử lý.",
                    "REFUND_REQUEST",
                    refundRequest.getRefundId());
        }
    }

    private void notifyClientRefundRequested(CenterRequestFeeHold hold, RefundRequest refundRequest) {
        paymentNotificationService.notifyPayment(
                hold.getClientUserId(),
                "Đã tạo yêu cầu hoàn phí",
                "Trung tâm đã yêu cầu hoàn phí cho yêu cầu #" + hold.getRequestId()
                        + ". Hệ thống sẽ xử lý theo quy trình hoàn tiền.",
                "REFUND_REQUEST",
                refundRequest.getRefundId());
    }

    private void notifyClientReleased(CenterRequestFeeHold hold) {
        paymentNotificationService.notifyPayment(
                hold.getClientUserId(),
                "Yêu cầu đã được xử lý",
                "Phí xử lý yêu cầu #" + hold.getRequestId() + " đã được giải ngân cho trung tâm.",
                "CLASS_REQUEST_FEE",
                hold.getFeeHoldId());
    }

    private void notifyCenterReleased(CenterRequestFeeHold hold) {
        paymentNotificationService.notifyPayment(
                hold.getCenterUserId(),
                "Đã nhận phí xử lý yêu cầu",
                "Phí xử lý yêu cầu #" + hold.getRequestId() + " đã được chuyển vào ví của bạn.",
                "CLASS_REQUEST_FEE",
                hold.getFeeHoldId());
    }

    private String buildReason(String reason) {
        return StringUtils.hasText(reason) ? reason.trim() : "Phí xử lý yêu cầu trung tâm";
    }

    @EventListener
    @Transactional
    public void onContractSigned(com.tcs.common.event.ContractSigned event) {
        if (event == null || event.assignmentId() == null) {
            if (event == null || event.classStudentId() == null) {
                return;
            }
            classStudentRepository.findById(event.classStudentId())
                    .map(ClassStudent::getTutoringClass)
                    .map(tutoringClass -> tutoringClass != null ? tutoringClass.getClassId() : null)
                    .flatMap(this::findHoldByClassId)
                    .filter(hold -> hold.getStatus() == CenterRequestFeeStatus.HELD)
                    .ifPresent(hold -> releaseInternal(hold, "Client đã ký hợp đồng"));
            return;
        }
        feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(event.assignmentId())
                .filter(hold -> hold.getStatus() == CenterRequestFeeStatus.HELD)
                .ifPresent(hold -> releaseInternal(hold, "Client đã ký hợp đồng"));
    }

    private Optional<CenterRequestFeeHold> findHoldByClassId(Long classId) {
        if (classId == null) {
            return Optional.empty();
        }
        return feeHoldRepository.findFirstByClassIdOrderByCreatedAtDesc(classId);
    }
}
