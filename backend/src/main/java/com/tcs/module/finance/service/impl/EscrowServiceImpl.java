package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private static final String PRIVATE_REF_PREFIX = "ESCROW-A";
    private static final String CENTER_REF_PREFIX = "ESCROW-CS";
    private static final String RELEASE_REF_PREFIX = "ESCROW_RELEASE-";
    private static final String REFUND_REF_PREFIX = "REFUND-ESCROW-";

    private final WalletService walletService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final UserRepository userRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final PaymentNotificationService paymentNotificationService;

    @Override
    @Transactional
    public EscrowTransaction lock(EscrowLockCommand command) {
        validateCommand(command);
        if (command.assignmentId() != null) {
            return lockPrivateAssignment(command);
        }
        return lockCenterEnrollment(command);
    }

    @Override
    @Transactional
    public void apply(ReleaseInstruction instruction) {
        validateReleaseInstruction(instruction);

        EscrowTransaction escrow = escrowTransactionRepository.findById(instruction.escrowId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));

        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            log.warn("[Escrow] Escrow id={} đã tất toán, bỏ qua", escrow.getEscrowId());
            return;
        }
        if (!isSettleable(escrow.getStatus())) {
            throw new BusinessException("Chỉ escrow đã khóa, tạm giữ hoặc tranh chấp mới có thể tất toán");
        }

        BigDecimal releaseAmount = amountOrZero(instruction.releaseToBeneficiary());
        BigDecimal refundAmount = amountOrZero(instruction.refundToPayer());
        BigDecimal totalSettlement = releaseAmount.add(refundAmount);
        if (totalSettlement.compareTo(escrow.getAmount()) != 0) {
            throw new BusinessException("Tổng tiền giải ngân/hoàn phải bằng số tiền escrow");
        }

        Long payerUserId = payerUserId(escrow);
        if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            releaseToBeneficiary(escrow, payerUserId, releaseAmount, instruction.reason());
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundToPayer(escrow, payerUserId, refundAmount, instruction.reason(), instruction.refundPayoutInfo());
        }

        escrow.setStatus(releaseAmount.compareTo(BigDecimal.ZERO) > 0
                ? EscrowStatus.RELEASED
                : EscrowStatus.REFUNDED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrowTransactionRepository.save(escrow);

        log.info("[Escrow] Đã tất toán escrow id={} release={} refund={} reason={}",
                escrow.getEscrowId(), releaseAmount, refundAmount, instruction.reason());
    }

    @Override
    @Transactional
    public EscrowTransaction refund(Long escrowId, String reason) {
        if (escrowId == null) {
            throw new BusinessException("Thiếu escrow cần hoàn tiền");
        }

        EscrowTransaction escrow = escrowTransactionRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));
        if (escrow.getStatus() == EscrowStatus.REFUNDED) {
            return escrow;
        }
        if (escrow.getStatus() == EscrowStatus.RELEASED) {
            throw new BusinessException("Escrow đã giải ngân nên không thể hoàn tiền");
        }
        if (escrow.getStatus() != EscrowStatus.FUNDED) {
            throw new BusinessException("Chỉ escrow đã được khóa tiền mới có thể hoàn tiền");
        }
        if (escrow.getAmount() == null || escrow.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền escrow không hợp lệ");
        }

        refundToPayer(escrow, payerUserId(escrow), escrow.getAmount(), reason, null);
        escrow.setStatus(EscrowStatus.REFUNDED);
        escrow.setReleasedAt(LocalDateTime.now());
        return escrowTransactionRepository.save(escrow);
    }

    @Override
    @Transactional
    public EscrowTransaction holdForDispute(Long escrowId, String reason) {
        if (escrowId == null) {
            throw new BusinessException("Thiếu escrow cần tạm giữ");
        }

        EscrowTransaction escrow = escrowTransactionRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));

        if (escrow.getStatus() == EscrowStatus.DISPUTED) {
            return escrow;
        }
        if (escrow.getStatus() == EscrowStatus.RELEASED || escrow.getStatus() == EscrowStatus.REFUNDED) {
            throw new BusinessException("Escrow đã tất toán nên không thể chuyển sang tranh chấp");
        }
        if (escrow.getStatus() != EscrowStatus.FUNDED && escrow.getStatus() != EscrowStatus.ON_HOLD) {
            throw new BusinessException("Chỉ escrow đã khóa tiền mới có thể tạm giữ khi có tranh chấp");
        }

        escrow.setStatus(EscrowStatus.DISPUTED);
        EscrowTransaction saved = escrowTransactionRepository.save(escrow);
        log.info("[Escrow] Đã tạm giữ escrow id={} do tranh chấp. reason={}",
                escrow.getEscrowId(), buildLogReason(reason));
        return saved;
    }

    private EscrowTransaction lockPrivateAssignment(EscrowLockCommand command) {
        return escrowTransactionRepository.findByAssignment_AssignmentId(command.assignmentId())
                .orElseGet(() -> {
                    ClassAssignment assignment = classAssignmentRepository.findById(command.assignmentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));
                    String reference = PRIVATE_REF_PREFIX + command.assignmentId();
                    PaymentTransaction payment = createEscrowPayment(command, reference);

                    EscrowTransaction escrow = new EscrowTransaction();
                    escrow.setPayment(payment);
                    escrow.setAssignment(assignment);
                    escrow.setAmount(command.amount());
                    escrow.setStatus(EscrowStatus.PENDING);
                    EscrowTransaction saved = escrowTransactionRepository.save(escrow);
                    log.info("[Escrow] Đã tạo lệnh thanh toán escrow cho assignment={} amount={}",
                            command.assignmentId(), command.amount());
                    return saved;
                });
    }

    private EscrowTransaction lockCenterEnrollment(EscrowLockCommand command) {
        return escrowTransactionRepository.findByClassStudent_ClassStudentId(command.classStudentId())
                .orElseGet(() -> {
                    ClassStudent classStudent = classStudentRepository.findById(command.classStudentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh học viên"));
                    String reference = CENTER_REF_PREFIX + command.classStudentId();
                    PaymentTransaction payment = createEscrowPayment(command, reference);

                    EscrowTransaction escrow = new EscrowTransaction();
                    escrow.setPayment(payment);
                    escrow.setClassStudent(classStudent);
                    escrow.setAmount(command.amount());
                    escrow.setStatus(EscrowStatus.PENDING);
                    EscrowTransaction saved = escrowTransactionRepository.save(escrow);
                    log.info("[Escrow] Đã tạo lệnh thanh toán escrow cho ghi danh={} amount={}",
                            command.classStudentId(), command.amount());
                    return saved;
                });
    }

    private PaymentTransaction createEscrowPayment(EscrowLockCommand command, String reference) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(walletService.getSystemEscrowWallet());
        tx.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(command.amount());
        tx.setDescription("Chờ client chuyển khoản học phí vào escrow");
        tx.setReferenceCode(reference);
        return paymentTransactionRepository.save(tx);
    }

    private void releaseToBeneficiary(
            EscrowTransaction escrow,
            Long payerUserId,
            BigDecimal amount,
            String reason) {

        String reference = RELEASE_REF_PREFIX + escrow.getEscrowId();
        if (paymentUsesPayerWallet(escrow, payerUserId)) {
            walletService.releaseLockedFunds(payerUserId, amount, reference);
        }

        Long beneficiaryUserId = beneficiaryUserId(escrow);
        walletService.credit(beneficiaryUserId, amount, reference);
        Wallet beneficiaryWallet = walletService.getOrCreate(beneficiaryUserId);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(beneficiaryWallet);
        tx.setType(PaymentTransactionType.ESCROW_RELEASE);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(amount);
        tx.setDescription(buildSettlementDescription("Giải ngân escrow", reason));
        tx.setReferenceCode(reference);
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);
        paymentNotificationService.notifyPayment(
                beneficiaryUserId,
                "Đã nhận tiền từ escrow",
                "Ví của bạn đã được cộng " + formatAmount(amount) + " từ tất toán escrow #" + escrow.getEscrowId() + ".",
                "ESCROW",
                escrow.getEscrowId());
    }

    private void refundToPayer(
            EscrowTransaction escrow,
            Long payerUserId,
            BigDecimal amount,
            String reason,
            RefundPayoutInfo payoutInfo) {

        String reference = REFUND_REF_PREFIX + escrow.getEscrowId();
        if (paymentUsesPayerWallet(escrow, payerUserId)) {
            Wallet payerWallet = walletService.refundLockedFunds(payerUserId, amount, reference);

            PaymentTransaction tx = new PaymentTransaction();
            tx.setWallet(payerWallet);
            tx.setType(PaymentTransactionType.REFUND);
            tx.setStatus(PaymentTransactionStatus.SUCCESS);
            tx.setAmount(amount);
            tx.setDescription(buildSettlementDescription("Hoàn tiền escrow", reason));
            tx.setReferenceCode(reference);
            tx.setProcessedAt(LocalDateTime.now());
            paymentTransactionRepository.save(tx);
        } else {
            PaymentTransaction tx = new PaymentTransaction();
            tx.setWallet(walletService.getSystemEscrowWallet());
            tx.setType(PaymentTransactionType.REFUND);
            tx.setStatus(PaymentTransactionStatus.PENDING);
            tx.setAmount(amount);
            tx.setDescription(buildSettlementDescription(
                    "Chờ chuyển khoản hoàn tiền escrow cho người thanh toán",
                    reason));
            tx.setReferenceCode(reference);
            paymentTransactionRepository.save(tx);
            ensureRefundTransferRequest(escrow, payerUserId, amount, reason, reference, payoutInfo);
        }
        paymentNotificationService.notifyPayment(
                payerUserId,
                "Hoàn tiền escrow đang xử lý",
                "Yêu cầu hoàn " + formatAmount(amount) + " từ escrow #" + escrow.getEscrowId()
                        + " đã được ghi nhận. TCS sẽ chuyển khoản về tài khoản nhận tiền của bạn.",
                "ESCROW",
                escrow.getEscrowId());
    }

    private void ensureRefundTransferRequest(
            EscrowTransaction escrow,
            Long payerUserId,
            BigDecimal amount,
            String reason,
            String reference,
            RefundPayoutInfo payoutInfo) {

        refundRequestRepository
                .findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(escrow.getEscrowId())
                .filter(request -> request.getStatus() != RefundRequestStatus.REJECTED)
                .ifPresentOrElse(
                        request -> syncApprovedRefundTransferRequest(request, amount, reference, payoutInfo),
                        () -> createApprovedRefundTransferRequest(escrow, payerUserId, amount, reason, reference, payoutInfo));
    }

    private void syncApprovedRefundTransferRequest(
            RefundRequest request,
            BigDecimal amount,
            String reference,
            RefundPayoutInfo payoutInfo) {
        RefundPayoutInfo resolvedPayoutInfo = resolveRefundPayoutInfo(request, payoutInfo);
        if (!RefundPayoutInfoCodec.hasCompletePayout(resolvedPayoutInfo)) {
            throw new BusinessException("Thiếu thông tin tài khoản nhận hoàn tiền");
        }
        if (request.getAmount() == null) {
            request.setAmount(amount);
        }
        request.setBankName(RefundPayoutInfoCodec.normalize(resolvedPayoutInfo.bankName()));
        request.setAccountNo(RefundPayoutInfoCodec.normalizeAccountNo(resolvedPayoutInfo.accountNo()));
        if (isBlank(request.getRefundReferenceCode())) {
            request.setRefundReferenceCode(reference);
        }
        if (isBlank(request.getTransferStatus()) || !"SUCCESS".equalsIgnoreCase(request.getTransferStatus())) {
            request.setTransferStatus("PENDING");
        }
        request.setReason(RefundPayoutInfoCodec.appendToReason(request.getReason(), resolvedPayoutInfo));
        if (request.getStatus() == RefundRequestStatus.PENDING) {
            request.setStatus(RefundRequestStatus.APPROVED);
        }
        if (request.getProcessedAt() == null) {
            request.setProcessedAt(LocalDateTime.now());
        }
        refundRequestRepository.save(request);
    }

    private void createApprovedRefundTransferRequest(
            EscrowTransaction escrow,
            Long payerUserId,
            BigDecimal amount,
            String reason,
            String reference,
            RefundPayoutInfo payoutInfo) {

        LocalDateTime now = LocalDateTime.now();
        RefundPayoutInfo resolvedPayoutInfo = resolveRefundPayoutInfo(null, payoutInfo);
        if (!RefundPayoutInfoCodec.hasCompletePayout(resolvedPayoutInfo)) {
            throw new BusinessException("Thiếu thông tin tài khoản nhận hoàn tiền");
        }
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setEscrowTransaction(escrow);
        refundRequest.setRequestedBy(resolvePayerUser(escrow, payerUserId));
        refundRequest.setAmount(amount);
        refundRequest.setBankName(RefundPayoutInfoCodec.normalize(resolvedPayoutInfo.bankName()));
        refundRequest.setAccountNo(RefundPayoutInfoCodec.normalizeAccountNo(resolvedPayoutInfo.accountNo()));
        refundRequest.setReason(RefundPayoutInfoCodec.appendToReason(
                buildSettlementDescription("Hoàn tiền tự động từ tất toán escrow", reason),
                resolvedPayoutInfo));
        refundRequest.setRefundReferenceCode(reference);
        refundRequest.setTransferStatus("PENDING");
        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setRequestedAt(now);
        refundRequest.setProcessedAt(now);
        refundRequestRepository.save(refundRequest);
    }

    private RefundPayoutInfo resolveRefundPayoutInfo(RefundRequest request, RefundPayoutInfo payoutInfo) {
        RefundPayoutInfo resolved = payoutInfo != null && RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)
                ? new RefundPayoutInfo(
                        RefundPayoutInfoCodec.normalize(payoutInfo.bankName()),
                        RefundPayoutInfoCodec.normalizeAccountNo(payoutInfo.accountNo()),
                        RefundPayoutInfoCodec.normalize(payoutInfo.accountHolderName()))
                : null;
        if (RefundPayoutInfoCodec.hasCompletePayout(resolved)) {
            return resolved;
        }
        if (request != null) {
            RefundPayoutInfo parsed = RefundPayoutInfoCodec.parseFromReason(request.getReason());
            if (RefundPayoutInfoCodec.hasCompletePayout(parsed)) {
                return parsed;
            }
            if (!isBlank(request.getBankName()) && !isBlank(request.getAccountNo())) {
                return new RefundPayoutInfo(
                        RefundPayoutInfoCodec.normalize(request.getBankName()),
                        RefundPayoutInfoCodec.normalizeAccountNo(request.getAccountNo()),
                        parsed != null ? RefundPayoutInfoCodec.normalize(parsed.accountHolderName()) : null);
            }
        }
        return resolved;
    }

    private User resolvePayerUser(EscrowTransaction escrow, Long payerUserId) {
        ClassStudent classStudent = escrow.getClassStudent();
        if (classStudent != null && classStudent.getEnrolledByUser() != null) {
            return classStudent.getEnrolledByUser();
        }

        ClassAssignment assignment = escrow.getAssignment();
        TutorApplication application = assignment != null ? assignment.getApplication() : null;
        TutoringClass tutoringClass = application != null ? application.getTutoringClass() : null;
        if (tutoringClass != null && tutoringClass.getCreator() != null) {
            return tutoringClass.getCreator();
        }

        if (escrow.getPayment() != null
                && escrow.getPayment().getWallet() != null
                && escrow.getPayment().getWallet().getUser() != null) {
            return escrow.getPayment().getWallet().getUser();
        }

        return userRepository.findById(payerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người thanh toán escrow"));
    }

    private Long payerUserId(EscrowTransaction escrow) {
        ClassStudent classStudent = escrow.getClassStudent();
        if (classStudent != null && classStudent.getEnrolledByUser() != null) {
            return classStudent.getEnrolledByUser().getUserId();
        }

        ClassAssignment assignment = escrow.getAssignment();
        TutoringClass tutoringClass = assignment != null
                && assignment.getApplication() != null
                ? assignment.getApplication().getTutoringClass()
                : null;
        if (tutoringClass != null && tutoringClass.getCreator() != null) {
            return tutoringClass.getCreator().getUserId();
        }

        if (escrow.getPayment() != null && escrow.getPayment().getWallet() != null) {
            Wallet wallet = escrow.getPayment().getWallet();
            if (wallet.getUser() != null && wallet.getUser().getUserId() != null) {
                return wallet.getUser().getUserId();
            }
            if (wallet.getWalletId() != null) {
                return wallet.getWalletId();
            }
        }
        throw new BusinessException("Escrow không xác định được người thanh toán");
    }

    private boolean paymentUsesPayerWallet(EscrowTransaction escrow, Long payerUserId) {
        if (escrow == null || escrow.getPayment() == null || escrow.getPayment().getWallet() == null
                || payerUserId == null) {
            return false;
        }
        Wallet wallet = escrow.getPayment().getWallet();
        return (wallet.getUser() != null && payerUserId.equals(wallet.getUser().getUserId()))
                || payerUserId.equals(wallet.getWalletId());
    }

    private boolean isSettleable(EscrowStatus status) {
        return status == EscrowStatus.FUNDED
                || status == EscrowStatus.ON_HOLD
                || status == EscrowStatus.DISPUTED;
    }

    private Long beneficiaryUserId(EscrowTransaction escrow) {
        if (escrow.getAssignment() != null
                && escrow.getAssignment().getTutor() != null
                && escrow.getAssignment().getTutor().getUser() != null) {
            return escrow.getAssignment().getTutor().getUser().getUserId();
        }

        ClassStudent classStudent = escrow.getClassStudent();
        TutoringClass tutoringClass = classStudent != null ? classStudent.getTutoringClass() : null;
        if (tutoringClass != null
                && tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null) {
            return tutoringClass.getCenter().getUser().getUserId();
        }
        if (tutoringClass != null && tutoringClass.getCreator() != null) {
            return tutoringClass.getCreator().getUserId();
        }

        throw new BusinessException("Không xác định được người nhận giải ngân escrow");
    }

    private String buildSettlementDescription(String prefix, String reason) {
        if (reason == null || reason.isBlank()) {
            return prefix;
        }
        return prefix + ": " + reason.trim();
    }

    private String buildLogReason(String reason) {
        return reason == null || reason.isBlank() ? "N/A" : reason.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        return amount.setScale(0, RoundingMode.DOWN).toPlainString() + " đ";
    }

    private void validateCommand(EscrowLockCommand command) {
        if (command == null) {
            throw new BusinessException("Thiếu thông tin khóa escrow");
        }
        if (command.payerUserId() == null) {
            throw new BusinessException("Thiếu người thanh toán escrow");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền escrow phải lớn hơn 0");
        }
        boolean hasAssignment = command.assignmentId() != null;
        boolean hasClassStudent = command.classStudentId() != null;
        if (hasAssignment == hasClassStudent) {
            throw new BusinessException("Escrow phải gắn đúng một trong assignmentId hoặc classStudentId");
        }
    }

    private void validateReleaseInstruction(ReleaseInstruction instruction) {
        if (instruction == null) {
            throw new BusinessException("Thiếu thông tin giải ngân escrow");
        }
        if (instruction.escrowId() == null) {
            throw new BusinessException("Thiếu escrow cần giải ngân");
        }
        BigDecimal releaseAmount = amountOrZero(instruction.releaseToBeneficiary());
        BigDecimal refundAmount = amountOrZero(instruction.refundToPayer());
        if (releaseAmount.compareTo(BigDecimal.ZERO) < 0 || refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Số tiền giải ngân/hoàn không được âm");
        }
        if (releaseAmount.add(refundAmount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Cần có số tiền giải ngân hoặc hoàn tiền");
        }
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
