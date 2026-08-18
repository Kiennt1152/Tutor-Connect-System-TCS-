package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.request.CreateRefundRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.PaymentMethodRequest;
import com.tcs.module.finance.dto.request.RefundDecisionRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.request.WithdrawalDecisionRequest;
import com.tcs.module.finance.dto.response.AdminWithdrawalPageResponse;
import com.tcs.module.finance.dto.response.AdminWithdrawalResponse;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.RefundRequestResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.TopupStatusResponse;
import com.tcs.module.finance.dto.response.TransactionResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.PaymentMethod;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.FinanceService;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int TOPUP_TTL_MINUTES = 15;
    private static final int WITHDRAWAL_MATCH_WINDOW_MINUTES = 5;
    private static final int CENTER_PAYOUT_COOLDOWN_MINUTES = 5;
    private static final int PAYOUT_CHANGE_REVIEW_HOURS = 72;
    private static final int FAST_TOPUP_WITHDRAWAL_HOURS = 24;
    private static final int INFLOW_AGGREGATION_REVIEW_HOURS = 72;
    private static final int ROUND_AMOUNT_REVIEW_DAYS = 7;
    private static final int ROUND_AMOUNT_REPEAT_THRESHOLD = 3;
    private static final int ACTIVE_PAYOUT_METHOD_REVIEW_THRESHOLD = 3;
    private static final int RECENT_PAYOUT_METHOD_REVIEW_THRESHOLD = 2;
    private static final BigDecimal ROUND_AMOUNT_UNIT = new BigDecimal("100000");
    private static final BigDecimal AGGREGATED_INFLOW_RATIO = new BigDecimal("0.8");
    private static final String TOPUP_BANK_NAME = "TPBank";
    private static final String TOPUP_BANK_BIN = "970423";
    private static final String TOPUP_ACCOUNT_NUMBER = "02660559201";
    private static final String TOPUP_ACCOUNT_NAME = "TUTOR CONNECT SYSTEM";
    private static final String BANK_TRANSFER_TYPE = "BANK_TRANSFER";
    private static final String PAYMENT_METHOD_ACTIVE = "ACTIVE";
    private static final String PAYMENT_METHOD_INACTIVE = "INACTIVE";
    private static final Pattern WITHDRAWAL_REQUEST_ALIAS_PATTERN =
            Pattern.compile("(?i)\\bWITHDRAW\\s*-\\s*(\\d+)\\b");

    private final AuthHelper authHelper;
    private final WalletService walletService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final CenterRequestFeeHoldRepository centerRequestFeeHoldRepository;
    private final DisputeRepository disputeRepository;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final EscrowService escrowService;
    private final CenterRequestFeeService centerRequestFeeService;
    private final PaymentNotificationService paymentNotificationService;
    private final PenaltyAccessService penaltyAccessService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final ClassRequestStore classRequestStore;

    @Value("${finance.withdrawal.auto-approval-threshold:0}")
    private BigDecimal withdrawalAutoApprovalThreshold;

    @Value("${finance.dev-direct-deposit-enabled:false}")
    private boolean directDepositEnabled;

    @Value("${finance.dev-simulate-topup-enabled:false}")
    private boolean simulateTopupEnabled;

    @Override
    @Transactional
    public WalletResponse getMyWallet() {
        return toWalletResponse(currentWallet());
    }

    @Override
    @Transactional
    public WalletResponse createMyWallet() {
        Long userId = requireEarningWalletUserId();
        penaltyAccessService.requireFeature(userId, "WITHDRAWAL");
        return toWalletResponse(walletService.create(userId));
    }

    @Override
    @Transactional
    public WalletResponse deposit(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        if (!directDepositEnabled) {
            throw new BusinessException("Nạp tiền trực tiếp đã tắt. Vui lòng nạp tiền qua cổng thanh toán.");
        }
        Long userId = requireCenterWalletUserId();
        Wallet wallet = walletService.getRequired(userId);
        String referenceCode = "TOPUP-" + UUID.randomUUID();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription() != null ? request.getDescription() : "Nạp tiền ví trung tâm");
        tx.setReferenceCode(referenceCode);
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);

        walletService.credit(userId, request.getAmount(), referenceCode);
        return toWalletResponse(walletService.getRequired(userId));
    }

    @Override
    @Transactional
    public TopupSessionResponse createTopup(DepositRequest request) {
        validateTopupAmount(request);
        Long userId = requireCenterWalletUserId();
        Wallet wallet = walletService.getRequired(userId);
        String reference = createTopupReference();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOPUP_TTL_MINUTES);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Nạp tiền ví trung tâm qua mã QR chuyển khoản");
        tx.setReferenceCode(reference);
        paymentTransactionRepository.save(tx);

        return toTopupSessionResponse(reference, request.getAmount(), expiresAt);
    }

    @Override
    @Transactional
    public TopupStatusResponse getTopupStatus(String reference) {
        requireCenterWalletUserId();
        PaymentTransaction tx = requireOwnedTopup(reference);
        return toTopupStatusResponse(tx);
    }

    @Override
    @Transactional
    public TopupStatusResponse simulateTopupSuccess(String reference) {
        requireCenterWalletUserId();
        if (!simulateTopupEnabled) {
            throw new BusinessException("Xác nhận nạp tiền demo đã tắt.");
        }
        PaymentTransaction tx = requireOwnedTopup(reference);
        if (isTopupExpired(tx)) {
            return TopupStatusResponse.builder()
                    .reference(reference)
                    .status("EXPIRED")
                    .message("QR đã hết hạn. Vui lòng tạo phiên nạp mới.")
                    .build();
        }
        return completeTopup(tx, "SIMULATED-" + reference, "Đã giả lập giao dịch thành công.");
    }

    @Override
    @Transactional
    public PaymentWebhookResponse handleSepayWebhook(SepayWebhookRequest request) {
        if ("out".equalsIgnoreCase(request != null ? request.getTransferType() : null)) {
            return handleSepayOutgoingWebhook(request);
        }
        return handleSepayIncomingWebhook(request);
    }

    @Override
    @Transactional
    public PaymentWebhookResponse handleSepayIncomingWebhook(SepayWebhookRequest request) {
        if (isInvalidWebhookRequest(request)) {
            return PaymentWebhookResponse.builder()
                    .status("error")
                    .message("Thiếu id, transferAmount hoặc nội dung giao dịch")
                    .build();
        }

        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return PaymentWebhookResponse.builder()
                    .status("ignored")
                    .message("Giao dịch không phải tiền vào")
                    .build();
        }

        String externalTransactionId = String.valueOf(request.getId());
        Optional<PaymentTransaction> duplicatePayment =
                paymentTransactionRepository.findByExternalTransactionId(externalTransactionId);
        if (duplicatePayment.isPresent()) {
            completeDuplicateEscrowPaymentIfNeeded(duplicatePayment.get());
            return PaymentWebhookResponse.builder()
                    .status("success")
                    .message("Webhook đã được xử lý trước đó")
                    .build();
        }

        PaymentTransaction matchedEscrow = findMatchingEscrowPayment(request);
        if (matchedEscrow != null) {
            boolean centerRequestFeePayment = centerRequestFeeService.isCenterRequestFeePayment(matchedEscrow);
            if (centerRequestFeePayment) {
                centerRequestFeeService.completeIncomingPayment(matchedEscrow, externalTransactionId);
            } else {
                completeEscrowPayment(matchedEscrow, externalTransactionId, "Đã ghi nhận học phí SePay vào escrow.");
            }
            return PaymentWebhookResponse.builder()
                    .status("success")
                    .message(centerRequestFeePayment
                            ? "Đã ghi nhận phí xử lý yêu cầu trung tâm"
                            : "Đã ghi nhận học phí SePay vào escrow")
                    .reference(matchedEscrow.getReferenceCode())
                    .build();
        }

        PaymentTransaction matched = findMatchingTopup(request);
        if (matched == null) {
            return PaymentWebhookResponse.builder()
                    .status("ignored")
                    .message("Không tìm thấy giao dịch nạp tiền/escrow khớp số tiền, nội dung và tài khoản")
                    .build();
        }

        completeTopup(matched, externalTransactionId, "Đã ghi nhận giao dịch SePay thành công.");
        return PaymentWebhookResponse.builder()
                .status("success")
                .message("Đã ghi nhận giao dịch SePay thành công")
                .reference(matched.getReferenceCode())
                .build();
    }

    @Override
    @Transactional
    public PaymentWebhookResponse handleSepayOutgoingWebhook(SepayWebhookRequest request) {
        if (isInvalidWebhookRequest(request)) {
            return PaymentWebhookResponse.builder()
                    .status("error")
                    .message("Thiếu id, transferAmount hoặc nội dung giao dịch")
                    .build();
        }

        if (!"out".equalsIgnoreCase(request.getTransferType())) {
            return PaymentWebhookResponse.builder()
                    .status("ignored")
                    .message("Giao dịch không phải tiền ra")
                    .build();
        }

        String externalTransactionId = sepayOutgoingExternalId(request.getId());
        PaymentTransaction duplicate = paymentTransactionRepository
                .findByExternalTransactionId(externalTransactionId)
                .orElse(null);
        if (duplicate != null) {
            return PaymentWebhookResponse.builder()
                    .status("success")
                    .message("Webhook đã được xử lý trước đó")
                    .reference(duplicate.getReferenceCode())
                    .build();
        }

        WithdrawalMatch match = findMatchingWithdrawal(request);
        PaymentTransaction tx = match != null ? match.tx() : null;
        WithdrawalRequest withdrawal = match != null ? match.withdrawal() : null;
        if (withdrawal == null) {
            PaymentTransaction refundTx = findMatchingOutgoingRefund(request);
            if (refundTx != null) {
                completeOutgoingRefund(
                        refundTx,
                        externalTransactionId,
                        "Hoàn tiền escrow đã được xác nhận qua SePay");
                return PaymentWebhookResponse.builder()
                        .status("success")
                        .message("Đã xác nhận giao dịch hoàn tiền từ SePay")
                        .reference(refundTx.getReferenceCode())
                        .build();
            }
            return PaymentWebhookResponse.builder()
                    .status("ignored")
                    .message("Không tìm thấy yêu cầu rút/hoàn tiền khớp số tiền, nội dung và tài khoản")
                    .reference(tx != null ? tx.getReferenceCode() : null)
                    .build();
        }

        completeWithdrawal(
                withdrawal,
                tx,
                externalTransactionId,
                "Yêu cầu rút tiền đã được xác nhận qua SePay");
        return PaymentWebhookResponse.builder()
                .status("success")
                .message("Đã xác nhận giao dịch rút tiền từ SePay")
                .reference(tx.getReferenceCode())
                .build();
    }

    @Override
    @Transactional
    public List<PaymentMethodResponse> getPaymentMethods() {
        Wallet wallet = currentWallet();
        List<PaymentMethod> methods = activePaymentMethods(wallet);
        Long defaultMethodId = defaultPaymentMethodId(methods);
        return methods.stream()
                .map(method -> toPaymentMethodResponse(method, Objects.equals(method.getPaymentMethodId(), defaultMethodId)))
                .toList();
    }

    @Override
    @Transactional
    public PaymentMethodResponse createPaymentMethod(PaymentMethodRequest request) {
        PaymentMethodData data = validatePaymentMethodRequest(request);
        Wallet wallet = currentWallet();
        List<PaymentMethod> activeMethods = activePaymentMethods(wallet);
        boolean shouldSetDefault = activeMethods.isEmpty();

        PaymentMethod method = paymentMethodRepository
                .findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
                        wallet.getWalletId(),
                        data.bankName(),
                        data.accountNo(),
                        PAYMENT_METHOD_ACTIVE)
                .orElseGet(() -> {
                    PaymentMethod paymentMethod = new PaymentMethod();
                    paymentMethod.setWallet(wallet);
                    paymentMethod.setType(BANK_TRANSFER_TYPE);
                    paymentMethod.setBankName(data.bankName());
                    paymentMethod.setAccountNo(data.accountNo());
                    paymentMethod.setAccountHolderName(data.accountHolderName());
                    paymentMethod.setVerifiedAt(LocalDateTime.now());
                    applyPayoutCooldown(paymentMethod);
                    paymentMethod.setStatus(PAYMENT_METHOD_ACTIVE);
                    if (shouldSetDefault) {
                        paymentMethod.setLastUsedAt(LocalDateTime.now());
                    }
                    return paymentMethodRepository.save(paymentMethod);
                });

        if (shouldSetDefault && method.getLastUsedAt() == null) {
            method.setLastUsedAt(LocalDateTime.now());
            method = paymentMethodRepository.save(method);
        }
        maybeWarnAnomalousPaymentMethod(method);
        return toPaymentMethodResponse(method, shouldSetDefault || isDefaultPaymentMethod(wallet, method));
    }

    @Override
    @Transactional
    public PaymentMethodResponse updatePaymentMethod(Long paymentMethodId, PaymentMethodRequest request) {
        PaymentMethodData data = validatePaymentMethodRequest(request);
        Wallet wallet = currentWallet();
        PaymentMethod method = requireOwnedActivePaymentMethod(wallet, paymentMethodId);

        paymentMethodRepository
                .findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
                        wallet.getWalletId(),
                        data.bankName(),
                        data.accountNo(),
                        PAYMENT_METHOD_ACTIVE)
                .filter(existing -> !Objects.equals(existing.getPaymentMethodId(), method.getPaymentMethodId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tài khoản nhận tiền này đã tồn tại");
                });

        method.setBankName(data.bankName());
        method.setAccountNo(data.accountNo());
        method.setAccountHolderName(data.accountHolderName());
        method.setVerifiedAt(LocalDateTime.now());
        applyPayoutCooldown(method);
        PaymentMethod saved = paymentMethodRepository.save(method);
        maybeWarnAnomalousPaymentMethod(saved);
        return toPaymentMethodResponse(saved, isDefaultPaymentMethod(wallet, saved));
    }

    @Override
    @Transactional
    public PaymentMethodResponse setDefaultPaymentMethod(Long paymentMethodId) {
        Wallet wallet = currentWallet();
        PaymentMethod method = requireOwnedActivePaymentMethod(wallet, paymentMethodId);
        method.setLastUsedAt(LocalDateTime.now());
        PaymentMethod saved = paymentMethodRepository.save(method);
        return toPaymentMethodResponse(saved, true);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(Long paymentMethodId) {
        Wallet wallet = currentWallet();
        PaymentMethod method = requireOwnedActivePaymentMethod(wallet, paymentMethodId);
        method.setStatus(PAYMENT_METHOD_INACTIVE);
        paymentMethodRepository.save(method);
    }

    @Override
    @Transactional
    public WithdrawalResponse createWithdrawal(CreateWithdrawalRequest request) {
        validateWithdrawalRequest(request);

        Long userId = requireEarningWalletUserId();
        Wallet wallet = walletService.getRequired(userId);
        PaymentMethod paymentMethod = resolveWithdrawalMethod(wallet, request);
        ensurePaymentMethodCanBeUsed(paymentMethod);
        String referenceCode = "WITHDRAW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String accountHolderName = resolveWithdrawalAccountHolderName(request, paymentMethod);
        LocalDateTime requestedAt = LocalDateTime.now();
        List<String> riskFlags = detectWithdrawalRiskFlags(wallet, paymentMethod, request.getAmount(), requestedAt);

        Wallet updatedWallet = walletService.lockFunds(userId, request.getAmount(), referenceCode);
        WithdrawalRequestStatus initialStatus = initialWithdrawalStatus(request.getAmount(), riskFlags);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(updatedWallet);
        tx.setPaymentMethod(paymentMethod);
        tx.setType(PaymentTransactionType.WITHDRAWAL);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(request.getAmount());
        tx.setDescription(initialStatus == WithdrawalRequestStatus.APPROVED
                ? "Yêu cầu rút tiền đã được tự động duyệt, chờ đối soát SePay"
                : "Yêu cầu rút tiền đang chờ quản trị viên duyệt");
        tx.setReferenceCode(referenceCode);
        paymentTransactionRepository.save(tx);

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWallet(updatedWallet);
        withdrawal.setPaymentMethod(paymentMethod);
        withdrawal.setAmount(request.getAmount());
        withdrawal.setBankName(paymentMethod.getBankName());
        withdrawal.setAccountNo(paymentMethod.getAccountNo());
        withdrawal.setAccountHolderName(accountHolderName);
        withdrawal.setStatus(initialStatus);
        withdrawal.setRequestedAt(requestedAt);
        WithdrawalRequest savedWithdrawal = withdrawalRequestRepository.save(withdrawal);
        notifyWithdrawalAdmins(savedWithdrawal);
        maybeWarnSuspiciousWithdrawal(savedWithdrawal, paymentMethod);
        notifyWithdrawalRiskReview(savedWithdrawal, riskFlags);
        paymentNotificationService.notifyPayment(
                userId,
                initialStatus == WithdrawalRequestStatus.APPROVED
                        ? "Yêu cầu rút tiền đã được duyệt"
                        : "Đã tạo yêu cầu rút tiền",
                initialStatus == WithdrawalRequestStatus.APPROVED
                        ? "Yêu cầu rút " + formatAmount(request.getAmount()) + " đã được tự động duyệt và đang chờ chuyển khoản."
                        : "Yêu cầu rút " + formatAmount(request.getAmount()) + " đang chờ quản trị viên duyệt.",
                "WITHDRAWAL_REQUEST",
                savedWithdrawal.getWithdrawalId());

        return toWithdrawalResponse(savedWithdrawal, tx, updatedWallet);
    }

    @Override
    @Transactional
    public WithdrawalResponse acceptWithdrawal(Long withdrawalId) {
        return approveWithdrawal(withdrawalId);
    }

    @Override
    @Transactional
    public WithdrawalResponse approveWithdrawal(Long withdrawalId) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);

        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu rút tiền"));
        if (withdrawal.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ yêu cầu rút tiền đang chờ duyệt mới có thể được duyệt");
        }

        PaymentTransaction tx = findSafeWithdrawalTransaction(withdrawal);
        if (tx == null) {
            throw new IllegalArgumentException("Không xác định được giao dịch rút tiền tương ứng");
        }

        tx.setDescription("Yêu cầu rút tiền đã được duyệt, chờ đối soát SePay");
        paymentTransactionRepository.save(tx);

        withdrawal.setStatus(WithdrawalRequestStatus.APPROVED);
        WithdrawalRequest savedWithdrawal = withdrawalRequestRepository.save(withdrawal);
        paymentNotificationService.notifyPayment(
                withdrawal.getWallet().getWalletId(),
                "Yêu cầu rút tiền đã được duyệt",
                "Yêu cầu rút " + formatAmount(withdrawal.getAmount()) + " đã được duyệt và đang chờ chuyển khoản.",
                "WITHDRAWAL_REQUEST",
                withdrawal.getWithdrawalId());

        return toWithdrawalResponse(savedWithdrawal, tx, withdrawal.getWallet());
    }

    @Override
    @Transactional
    public WithdrawalResponse rejectWithdrawal(Long withdrawalId, WithdrawalDecisionRequest request) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        return reverseWithdrawal(
                withdrawalId,
                request,
                PaymentTransactionStatus.CANCELLED,
                "Yêu cầu rút tiền bị từ chối",
                "Yêu cầu rút tiền bị từ chối");
    }

    @Override
    @Transactional
    public WithdrawalResponse markWithdrawalTransferFailed(Long withdrawalId, WithdrawalDecisionRequest request) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);
        return reverseWithdrawal(
                withdrawalId,
                request,
                PaymentTransactionStatus.FAILED,
                "Chuyển khoản rút tiền thất bại",
                "Chuyển khoản ngân hàng thất bại, hệ thống đã hoàn lại số dư khả dụng");
    }

    private WithdrawalResponse reverseWithdrawal(
            Long withdrawalId,
            WithdrawalDecisionRequest request,
            PaymentTransactionStatus transactionStatus,
            String notificationTitle,
            String defaultReason) {

        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu rút tiền"));
        if (!isWithdrawalAwaitingTransfer(withdrawal.getStatus())) {
            throw new IllegalArgumentException("Chỉ yêu cầu rút tiền đang chờ mới có thể hoàn lại");
        }

        PaymentTransaction tx = findSafeWithdrawalTransaction(withdrawal);
        if (tx == null) {
            throw new IllegalArgumentException("Không xác định được giao dịch rút tiền tương ứng");
        }

        String reason = decisionReason(request, defaultReason);
        Wallet wallet = walletService.refundLockedFunds(
                withdrawal.getWallet().getWalletId(),
                withdrawal.getAmount(),
                tx.getReferenceCode());
        LocalDateTime now = LocalDateTime.now();

        tx.setStatus(transactionStatus);
        tx.setDescription(reason);
        tx.setProcessedAt(now);
        tx.setFailureReason(reason);
        paymentTransactionRepository.save(tx);

        withdrawal.setStatus(WithdrawalRequestStatus.REJECTED);
        withdrawal.setProcessedAt(now);
        withdrawal.setFailureReason(reason);
        WithdrawalRequest savedWithdrawal = withdrawalRequestRepository.save(withdrawal);

        paymentNotificationService.notifyPayment(
                withdrawal.getWallet().getWalletId(),
                notificationTitle,
                reason + ". Số tiền " + formatAmount(withdrawal.getAmount()) + " đã được hoàn về số dư khả dụng.",
                "WITHDRAWAL_REQUEST",
                withdrawal.getWithdrawalId());

        return toWithdrawalResponse(savedWithdrawal, tx, wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminWithdrawalPageResponse getAdminWithdrawals(int page, int size, String status) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);

        WithdrawalRequestStatus statusFilter = parseWithdrawalStatus(status);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        List<AdminWithdrawalResponse> allRequests = new ArrayList<>();
        allRequests.addAll(withdrawalRequestRepository.findAdminList(statusFilter).stream()
                .map(withdrawal -> toAdminWithdrawalResponse(withdrawal, findWithdrawalTransaction(withdrawal)))
                .toList());
        allRequests.addAll(refundRequestRepository.findAllByOrderByRequestedAtDesc().stream()
                .filter(this::isRefundTransferRequest)
                .map(this::toAdminRefundTransferResponse)
                .filter(refund -> statusFilter == null || refund.getStatus() == statusFilter)
                .toList());

        allRequests.sort((left, right) -> {
            LocalDateTime leftTime = left.getRequestedAt();
            LocalDateTime rightTime = right.getRequestedAt();
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });

        int total = allRequests.size();
        int fromIndex = Math.min(normalizedPage * normalizedSize, total);
        int toIndex = Math.min(fromIndex + normalizedSize, total);
        List<AdminWithdrawalResponse> content = allRequests.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);

        return AdminWithdrawalPageResponse.builder()
                .content(content)
                .page(normalizedPage)
                .size(normalizedSize)
                .totalPages(totalPages)
                .totalElements(total)
                .build();
    }

    @Override
    @Transactional
    public RefundRequestResponse createRefundRequest(CreateRefundRequest request) {
        validateCreateRefundRequest(request);
        Long userId = authHelper.currentUserId();
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        EscrowTransaction escrow = resolveRefundEscrow(request);
        requireRefundEscrowParticipant(escrow, userId);
        ensureRefundEscrowOpen(escrow);
        RefundPayoutInfo payoutInfo = validateRefundPayoutInfo(
                request.getBankName(),
                request.getAccountNo(),
                request.getAccountHolderName());

        BigDecimal requestedAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal escrowAmount = amountOrZero(escrow.getAmount());
        if (requestedAmount.compareTo(escrowAmount) > 0) {
            throw new BusinessException("Số tiền hoàn không được vượt quá số tiền escrow");
        }
        if (refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                escrow.getEscrowId(),
                RefundRequestStatus.PENDING)) {
            throw new BusinessException("Escrow này đã có yêu cầu hoàn tiền đang chờ xử lý");
        }

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setEscrowTransaction(escrow);
        refundRequest.setRequestedBy(requester);
        refundRequest.setAmount(requestedAmount);
        refundRequest.setBankName(payoutInfo.bankName());
        refundRequest.setAccountNo(payoutInfo.accountNo());
        refundRequest.setAccountHolderName(payoutInfo.accountHolderName());
        refundRequest.setTransferStatus("PENDING");
        refundRequest.setReason(RefundPayoutInfoCodec.appendToReason(request.getReason().trim(), payoutInfo));
        refundRequest.setStatus(RefundRequestStatus.PENDING);
        refundRequest.setRequestedAt(LocalDateTime.now());
        RefundRequest saved = refundRequestRepository.save(refundRequest);

        EscrowTransaction heldEscrow = escrowService.holdForDispute(
                escrow.getEscrowId(),
                "Yêu cầu hoàn tiền #" + saved.getRefundId());
        saved.setEscrowTransaction(heldEscrow);
        notifyRefundAdmins(saved);
        paymentNotificationService.notifyPayment(
                requester,
                "Đã gửi yêu cầu hoàn tiền",
                "Yêu cầu hoàn " + formatAmount(saved.getAmount()) + " đang chờ quản trị viên xử lý.",
                "REFUND_REQUEST",
                saved.getRefundId());

        return toRefundRequestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundRequestResponse> getAdminRefundRequests(String status) {
        UserPrincipal reviewer = authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
        RefundRequestStatus statusFilter = parseRefundStatus(status);
        List<RefundRequest> requests = statusFilter != null
                ? refundRequestRepository.findByStatusOrderByRequestedAtDesc(statusFilter)
                : refundRequestRepository.findAllByOrderByRequestedAtDesc();
        return requests.stream()
                .filter(refundRequest -> canReviewRefundRequest(reviewer, refundRequest))
                .map(this::toRefundRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public RefundRequestResponse approveRefundRequest(Long refundId, RefundDecisionRequest request) {
        UserPrincipal reviewer = authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
        RefundRequest refundRequest = requirePendingRefund(refundId);
        requireCanReviewRefundRequest(reviewer, refundRequest);
        EscrowTransaction escrow = refundRequest.getEscrowTransaction();
        if (refundRequest.getCenterRequestFeeHold() != null) {
            BigDecimal approvedAmount = decisionAmount(request, refundRequest.getAmount());
            BigDecimal requestedAmount = amountOrZero(refundRequest.getAmount());
            if (approvedAmount.compareTo(requestedAmount) > 0) {
                throw new BusinessException("Số tiền hoàn được duyệt không được vượt quá số tiền yêu cầu");
            }
            String reason = decisionReason(request, "Duyệt yêu cầu hoàn phí trung tâm");

            refundRequest.setAmount(approvedAmount);
            refundRequest.setReason(appendDecisionNote(refundRequest.getReason(), "Duyệt hoàn tiền", reason));
            refundRequest.setStatus(RefundRequestStatus.APPROVED);
            refundRequest.setProcessedAt(LocalDateTime.now());
            refundRequest.setTransferStatus("PENDING");
            if (isBlank(refundRequest.getRefundReferenceCode())) {
                refundRequest.setRefundReferenceCode(
                        centerRequestFeeRefundReferenceCode(refundRequest.getCenterRequestFeeHold().getFeeHoldId()));
            }
            refundRequest = refundRequestRepository.save(refundRequest);

            PaymentTransaction tx = new PaymentTransaction();
            tx.setWallet(walletService.getSystemEscrowWallet());
            tx.setType(PaymentTransactionType.REFUND);
            tx.setStatus(PaymentTransactionStatus.PENDING);
            tx.setAmount(approvedAmount);
            tx.setDescription("Chờ chuyển khoản hoàn phí xử lý yêu cầu trung tâm");
            tx.setReferenceCode(refundRequest.getRefundReferenceCode());
            paymentTransactionRepository.save(tx);

            paymentNotificationService.notifyPayment(
                    refundRequest.getRequestedBy(),
                    "Yêu cầu hoàn phí trung tâm đã được duyệt",
                    "Yêu cầu hoàn " + formatAmount(approvedAmount)
                            + " đang chờ chuyển khoản qua SePay.",
                    "REFUND_REQUEST",
                    refundRequest.getRefundId());
            return toRefundRequestResponse(refundRequest);
        }
        ensureRefundEscrowOpen(escrow);

        BigDecimal approvedAmount = decisionAmount(request, refundRequest.getAmount());
        BigDecimal escrowAmount = amountOrZero(escrow.getAmount());
        if (approvedAmount.compareTo(escrowAmount) > 0) {
            throw new BusinessException("Số tiền hoàn được duyệt không được vượt quá số tiền escrow");
        }
        BigDecimal releaseAmount = escrowAmount.subtract(approvedAmount).setScale(2, RoundingMode.HALF_UP);
        String reason = decisionReason(request, "Duyệt yêu cầu hoàn tiền");

        refundRequest.setAmount(approvedAmount);
        refundRequest.setReason(appendDecisionNote(refundRequest.getReason(), "Duyệt hoàn tiền", reason));
        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setProcessedAt(LocalDateTime.now());
        refundRequest = refundRequestRepository.save(refundRequest);

        RefundPayoutInfo payoutInfo = resolveRefundPayoutInfo(refundRequest);
        escrowService.apply(new ReleaseInstruction(
                escrow.getEscrowId(),
                releaseAmount,
                approvedAmount,
                reason,
                payoutInfo));

        EscrowTransaction settledEscrow = escrowTransactionRepository.findById(escrow.getEscrowId()).orElse(escrow);
        boolean refundedToPayerWallet = escrowPaymentUsesPayerWallet(escrow);
        refundRequest.setEscrowTransaction(settledEscrow);
        refundRequest.setRefundReferenceCode(refundReferenceCode(escrow.getEscrowId()));
        refundRequest.setTransferStatus(refundedToPayerWallet ? "SUCCESS" : "PENDING");
        if (refundedToPayerWallet) {
            refundRequest.setTransferProcessedAt(LocalDateTime.now());
        }
        refundRequest.setStatus(refundedToPayerWallet ? RefundRequestStatus.COMPLETED : RefundRequestStatus.APPROVED);
        refundRequest.setProcessedAt(LocalDateTime.now());
        refundRequest = refundRequestRepository.save(refundRequest);

        paymentNotificationService.notifyPayment(
                refundRequest.getRequestedBy(),
                "Yêu cầu hoàn tiền đã được duyệt",
                refundedToPayerWallet
                        ? "Yêu cầu hoàn " + formatAmount(approvedAmount) + " đã được xử lý."
                        : "Yêu cầu hoàn " + formatAmount(approvedAmount)
                                + " đã được duyệt và đang chờ chuyển khoản qua SePay.",
                "REFUND_REQUEST",
                refundRequest.getRefundId());
        return toRefundRequestResponse(refundRequest);
    }

    @Override
    @Transactional
    public RefundRequestResponse rejectRefundRequest(Long refundId, RefundDecisionRequest request) {
        UserPrincipal reviewer = authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
        RefundRequest refundRequest = requirePendingRefund(refundId);
        requireCanReviewRefundRequest(reviewer, refundRequest);
        String reason = decisionReason(request, "Từ chối yêu cầu hoàn tiền");

        if (refundRequest.getCenterRequestFeeHold() != null) {
            refundRequest.setReason(appendDecisionNote(refundRequest.getReason(), "Từ chối hoàn tiền", reason));
            refundRequest.setStatus(RefundRequestStatus.REJECTED);
            refundRequest.setProcessedAt(LocalDateTime.now());
            RefundRequest saved = refundRequestRepository.save(refundRequest);
            CenterRequestFeeStatus previousStatus = refundRequest.getCenterRequestFeeHold().getStatus();
            if (previousStatus == CenterRequestFeeStatus.REFUND_REQUESTED) {
                refundRequest.getCenterRequestFeeHold().setStatus(CenterRequestFeeStatus.HELD);
                centerRequestFeeHoldRepository.save(refundRequest.getCenterRequestFeeHold());
            }
            paymentNotificationService.notifyPayment(
                    saved.getRequestedBy(),
                    "Yêu cầu hoàn phí trung tâm bị từ chối",
                    reason,
                    "REFUND_REQUEST",
                    saved.getRefundId());
            return toRefundRequestResponse(saved);
        }

        refundRequest.setReason(appendDecisionNote(refundRequest.getReason(), "Từ chối hoàn tiền", reason));
        refundRequest.setStatus(RefundRequestStatus.REJECTED);
        refundRequest.setProcessedAt(LocalDateTime.now());
        RefundRequest saved = refundRequestRepository.save(refundRequest);
        restoreEscrowIfOnlyRefundHeld(saved.getEscrowTransaction());

        paymentNotificationService.notifyPayment(
                saved.getRequestedBy(),
                "Yêu cầu hoàn tiền bị từ chối",
                reason,
                "REFUND_REQUEST",
                saved.getRefundId());
        return toRefundRequestResponse(saved);
    }

    @Override
    @Transactional
    public WalletTransactionsResponse getMyTransactions(
            int page,
            int size,
            String type,
            LocalDate from,
            LocalDate to) {

        Wallet wallet = currentWallet();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));

        LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDt = (to != null) ? to.atTime(LocalTime.MAX) : null;
        if (fromDt != null && toDt != null && fromDt.isAfter(toDt)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }

        PaymentTransactionType txType = null;
        if (type != null && !type.isBlank()) {
            try {
                txType = PaymentTransactionType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Loại giao dịch không hợp lệ: " + type);
            }
        }

        Page<PaymentTransaction> txPage = paymentTransactionRepository.findByWalletIdWithFilters(
                wallet.getWalletId(), txType, fromDt, toDt, pageable);

        List<TransactionResponse> transactions = txPage.getContent().stream()
                .map(this::toTransactionResponse)
                .toList();

        return WalletTransactionsResponse.builder()
                .transactions(transactions)
                .page(txPage.getNumber())
                .totalPages(txPage.getTotalPages())
                .totalElements(txPage.getTotalElements())
                .build();
    }

    private Wallet currentWallet() {
        return walletService.getRequired(requireEarningWalletUserId());
    }

    private Long requireEarningWalletUserId() {
        authHelper.requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER);
        return authHelper.currentUserId();
    }

    private Long requireCenterWalletUserId() {
        authHelper.requireRole(UserRole.TUTOR_CENTER);
        return authHelper.currentUserId();
    }

    private void validateTopupAmount(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
    }

    private void validateWithdrawalRequest(CreateWithdrawalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin rút tiền");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0");
        }
        if (request.getPaymentMethodId() == null) {
            throw new IllegalArgumentException("Vui lòng thêm và chọn tài khoản nhận tiền trước khi rút tiền");
        }
        if (!isBlank(request.getAccountHolderName()) && request.getAccountHolderName().trim().length() > 150) {
            throw new IllegalArgumentException("Tên chủ tài khoản không được vượt quá 150 ký tự");
        }
    }

    private PaymentMethod resolveWithdrawalMethod(Wallet wallet, CreateWithdrawalRequest request) {
        if (request.getPaymentMethodId() != null) {
            return requireOwnedActivePaymentMethod(wallet, request.getPaymentMethodId());
        }

        PaymentMethodData data = validatePaymentMethodRequest(
                request.getBankName(),
                request.getAccountNo(),
                request.getAccountHolderName());
        return paymentMethodRepository
                .findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
                        wallet.getWalletId(),
                        data.bankName(),
                        data.accountNo(),
                        PAYMENT_METHOD_ACTIVE)
                .orElseGet(() -> {
                    PaymentMethod paymentMethod = new PaymentMethod();
                    paymentMethod.setWallet(wallet);
                    paymentMethod.setType(BANK_TRANSFER_TYPE);
                    paymentMethod.setBankName(data.bankName());
                    paymentMethod.setAccountNo(data.accountNo());
                    paymentMethod.setAccountHolderName(data.accountHolderName());
                    paymentMethod.setVerifiedAt(LocalDateTime.now());
                    applyPayoutCooldown(paymentMethod);
                    paymentMethod.setStatus(PAYMENT_METHOD_ACTIVE);
                    return paymentMethodRepository.save(paymentMethod);
                });
    }

    private String createTopupReference() {
        return "TOPUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentTransaction requireOwnedTopup(String reference) {
        PaymentTransaction tx = paymentTransactionRepository.findByReferenceCode(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên nạp tiền"));

        Long currentUserId = authHelper.currentUserId();
        if (!tx.getWallet().getWalletId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Không tìm thấy phiên nạp tiền");
        }
        return tx;
    }

    private PaymentTransaction findMatchingTopup(SepayWebhookRequest request) {
        List<PaymentTransaction> candidates = findTransactionsByTypeStatusAmount(
                PaymentTransactionType.DEPOSIT,
                PaymentTransactionStatus.PENDING,
                request.getTransferAmount());

        String payload = compactWebhookPayload(request);
        String accountNumber = compact(request.getAccountNumber());

        return candidates.stream()
                .filter(tx -> !isTopupExpired(tx))
                .filter(tx -> accountMatches(accountNumber))
                .filter(tx -> transferContentMatches(tx, payload))
                .findFirst()
                .orElse(null);
    }

    private PaymentTransaction findMatchingEscrowPayment(SepayWebhookRequest request) {
        String accountNumber = compact(request.getAccountNumber());
        if (!accountMatches(accountNumber)) {
            return null;
        }

        String payload = compactWebhookPayload(request);
        return findTransactionsByTypeStatusAmount(
                        PaymentTransactionType.ESCROW_DEPOSIT,
                        PaymentTransactionStatus.PENDING,
                        request.getTransferAmount())
                .stream()
                .filter(tx -> transferContentMatches(tx, payload))
                .findFirst()
                .orElse(null);
    }

    private PaymentTransaction findMatchingOutgoingRefund(SepayWebhookRequest request) {
        String accountNumber = compact(request.getAccountNumber());
        if (!accountMatches(accountNumber)) {
            return null;
        }

        String payload = compactWebhookPayload(request);
        return findTransactionsByTypeStatusAmount(
                        PaymentTransactionType.REFUND,
                        PaymentTransactionStatus.PENDING,
                        request.getTransferAmount())
                .stream()
                .filter(tx -> transferContentMatches(tx, payload))
                .findFirst()
                .orElse(null);
    }

    private WithdrawalMatch findMatchingWithdrawal(SepayWebhookRequest request) {
        WithdrawalMatch requestIdMatch = findMatchingWithdrawalByRequestId(request);
        if (requestIdMatch != null) {
            return requestIdMatch;
        }

        List<PaymentTransaction> matches = findMatchingWithdrawalTransactions(request);
        if (matches.size() != 1) {
            return null;
        }

        PaymentTransaction tx = matches.get(0);
        WithdrawalRequest withdrawal = findMatchingPendingWithdrawal(tx);
        return withdrawal != null ? new WithdrawalMatch(withdrawal, tx) : null;
    }

    private WithdrawalMatch findMatchingWithdrawalByRequestId(SepayWebhookRequest request) {
        Long withdrawalId = extractWithdrawalRequestId(request);
        if (withdrawalId == null) {
            return null;
        }

        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId).orElse(null);
        if (withdrawal == null
                || !isWithdrawalAwaitingTransfer(withdrawal.getStatus())
                || withdrawal.getAmount() == null
                || withdrawal.getAmount().compareTo(request.getTransferAmount()) != 0) {
            return null;
        }

        PaymentTransaction tx = findSafeWithdrawalTransaction(withdrawal);
        return tx != null ? new WithdrawalMatch(withdrawal, tx) : null;
    }

    private Long extractWithdrawalRequestId(SepayWebhookRequest request) {
        Matcher matcher = WITHDRAWAL_REQUEST_ALIAS_PATTERN.matcher(rawWebhookPayload(request));
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }

    private List<PaymentTransaction> findMatchingWithdrawalTransactions(SepayWebhookRequest request) {
        String accountNumber = compact(request.getAccountNumber());
        if (!accountMatches(accountNumber)) {
            return List.of();
        }

        String payload = compactWebhookPayload(request);
        return findTransactionsByTypeStatusAmount(
                        PaymentTransactionType.WITHDRAWAL,
                        PaymentTransactionStatus.PENDING,
                        request.getTransferAmount())
                .stream()
                .filter(tx -> transferContentMatches(tx, payload))
                .toList();
    }

    private List<PaymentTransaction> findTransactionsByTypeStatusAmount(
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            BigDecimal amount) {
        List<PaymentTransaction> transactions =
                paymentTransactionRepository.findByTypeAndStatusAndAmount(type, status, amount);
        return transactions != null ? transactions : List.of();
    }

    private boolean accountMatches(String incomingAccountNumber) {
        if (incomingAccountNumber.isBlank()) {
            return true;
        }
        String configuredAccount = compact(TOPUP_ACCOUNT_NUMBER);
        return incomingAccountNumber.equals(configuredAccount)
                || incomingAccountNumber.contains(configuredAccount)
                || configuredAccount.contains(incomingAccountNumber);
    }

    private boolean transferContentMatches(PaymentTransaction tx, String payload) {
        String reference = compact(tx.getReferenceCode());
        if (reference.isBlank()) {
            return false;
        }
        return payload.contains(reference);
    }

    private boolean isInvalidWebhookRequest(SepayWebhookRequest request) {
        return request == null
                || request.getId() == null
                || request.getTransferAmount() == null
                || request.getTransferAmount().compareTo(BigDecimal.ZERO) <= 0
                || compactWebhookPayload(request).isBlank();
    }

    private String compactWebhookPayload(SepayWebhookRequest request) {
        if (request == null) {
            return "";
        }
        return compact(rawWebhookPayload(request));
    }

    private String rawWebhookPayload(SepayWebhookRequest request) {
        if (request == null) {
            return "";
        }
        return valueOrEmpty(request.getContent())
                + " "
                + valueOrEmpty(request.getCode())
                + " "
                + valueOrEmpty(request.getDescription())
                + " "
                + valueOrEmpty(request.getReferenceCode());
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String sepayOutgoingExternalId(Long id) {
        return "SEPAY-OUT-" + id;
    }

    private WithdrawalRequest findMatchingPendingWithdrawal(PaymentTransaction tx) {
        if (tx.getWallet() == null || tx.getCreatedAt() == null || tx.getAmount() == null) {
            return null;
        }

        LocalDateTime from = tx.getCreatedAt().minusMinutes(WITHDRAWAL_MATCH_WINDOW_MINUTES);
        LocalDateTime to = tx.getCreatedAt().plusMinutes(WITHDRAWAL_MATCH_WINDOW_MINUTES);
        List<WithdrawalRequest> candidates = List.of(
                        WithdrawalRequestStatus.PENDING,
                        WithdrawalRequestStatus.APPROVED)
                .stream()
                .flatMap(status -> withdrawalRequestRepository
                        .findByWallet_WalletIdAndStatusAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
                                tx.getWallet().getWalletId(),
                                status,
                                tx.getAmount(),
                                from,
                                to)
                        .stream())
                .toList();

        List<WithdrawalRequest> matched = candidates.stream()
                .filter(withdrawal -> paymentMethodMatches(withdrawal, tx))
                .toList();
        return matched.size() == 1 ? matched.get(0) : null;
    }

    private boolean paymentMethodMatches(WithdrawalRequest withdrawal, PaymentTransaction tx) {
        if (tx.getPaymentMethod() == null) {
            return true;
        }
        if (withdrawal.getPaymentMethod() == null) {
            return false;
        }
        return Objects.equals(
                withdrawal.getPaymentMethod().getPaymentMethodId(),
                tx.getPaymentMethod().getPaymentMethodId());
    }

    private WithdrawalResponse completeWithdrawal(
            WithdrawalRequest withdrawal,
            PaymentTransaction tx,
            String externalTransactionId,
            String description) {

        LocalDateTime now = LocalDateTime.now();
        Wallet wallet = walletService.releaseLockedFunds(
                withdrawal.getWallet().getWalletId(),
                withdrawal.getAmount(),
                tx.getReferenceCode());

        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setDescription(description);
        if (!isBlank(externalTransactionId)) {
            tx.setExternalTransactionId(externalTransactionId);
        }
        tx.setProcessedAt(now);
        tx.setFailureReason(null);
        paymentTransactionRepository.save(tx);

        withdrawal.setStatus(WithdrawalRequestStatus.COMPLETED);
        withdrawal.setProcessedAt(now);
        withdrawal.setFailureReason(null);
        WithdrawalRequest savedWithdrawal = withdrawalRequestRepository.save(withdrawal);
        flagPayoutMethodUsage(withdrawal.getPaymentMethod(), now);
        paymentNotificationService.notifyPayment(
                withdrawal.getWallet().getWalletId(),
                "Rút tiền thành công",
                "Yêu cầu rút " + formatAmount(withdrawal.getAmount()) + " đã được xử lý thành công.",
                "WITHDRAWAL_REQUEST",
                withdrawal.getWithdrawalId());

        return toWithdrawalResponse(savedWithdrawal, tx, wallet);
    }

    private EscrowTransaction completeEscrowPayment(
            PaymentTransaction tx,
            String externalTransactionId,
            String message) {

        if (tx.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return escrowService.fundConfirmedPayment(tx);
        }
        if (tx.getStatus() != PaymentTransactionStatus.PENDING) {
            throw new BusinessException("Giao dịch escrow không còn ở trạng thái chờ thanh toán");
        }

        LocalDateTime now = LocalDateTime.now();
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setExternalTransactionId(externalTransactionId);
        tx.setProcessedAt(now);
        tx.setDescription(message);
        paymentTransactionRepository.save(tx);

        EscrowTransaction savedEscrow = escrowService.fundConfirmedPayment(tx);
        notifyEscrowFunded(savedEscrow);

        return savedEscrow;
    }

    private void completeDuplicateEscrowPaymentIfNeeded(PaymentTransaction tx) {
        if (tx == null
                || tx.getType() != PaymentTransactionType.ESCROW_DEPOSIT
                || tx.getStatus() != PaymentTransactionStatus.SUCCESS
                || centerRequestFeeService.isCenterRequestFeePayment(tx)) {
            return;
        }
        boolean escrowAlreadyCreated = tx.getTransactionId() != null
                && escrowTransactionRepository.findByPayment_TransactionId(tx.getTransactionId()).isPresent();
        EscrowTransaction savedEscrow = escrowService.fundConfirmedPayment(tx);
        if (!escrowAlreadyCreated) {
            notifyEscrowFunded(savedEscrow);
        }
    }

    private void notifyEscrowFunded(EscrowTransaction savedEscrow) {
        Long payerUserId = escrowPayerUserId(savedEscrow);
        Long beneficiaryUserId = escrowBeneficiaryUserId(savedEscrow);
        TutoringClass tutoringClass = resolveTutoringClass(savedEscrow);
        paymentNotificationService.notifyPayment(
                payerUserId,
                "Thanh toán escrow thành công",
                "Học phí " + formatAmount(savedEscrow.getAmount()) + " đã được ghi nhận vào escrow.",
                "ESCROW",
                savedEscrow.getEscrowId());
        paymentNotificationService.notifyPayment(
                beneficiaryUserId,
                "Escrow đã được nạp",
                "Escrow #" + savedEscrow.getEscrowId() + " đã nhận "
                        + formatAmount(savedEscrow.getAmount()) + ".",
                "ESCROW",
                savedEscrow.getEscrowId());
        eventPublisher.publishEvent(new EscrowFunded(
                savedEscrow.getEscrowId(),
                tutoringClass != null ? tutoringClass.getClassId() : null,
                payerUserId,
                beneficiaryUserId,
                savedEscrow.getAmount(),
                savedEscrow.getAssignment() != null ? savedEscrow.getAssignment().getAssignmentId() : null,
                savedEscrow.getClassStudent() != null ? savedEscrow.getClassStudent().getClassStudentId() : null));
    }

    private void completeOutgoingRefund(
            PaymentTransaction tx,
            String externalTransactionId,
            String description) {

        if (tx.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return;
        }
        if (tx.getStatus() != PaymentTransactionStatus.PENDING) {
            throw new BusinessException("Giao dịch hoàn tiền không còn ở trạng thái chờ");
        }

        LocalDateTime now = LocalDateTime.now();
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setExternalTransactionId(externalTransactionId);
        tx.setProcessedAt(now);
        tx.setDescription(description);
        tx.setFailureReason(null);
        paymentTransactionRepository.save(tx);

        refundRequestRepository.findByRefundReferenceCode(tx.getReferenceCode()).ifPresent(refundRequest -> {
            refundRequest.setStatus(RefundRequestStatus.COMPLETED);
            refundRequest.setTransferStatus("SUCCESS");
            refundRequest.setTransferProcessedAt(now);
            refundRequestRepository.save(refundRequest);
            if (refundRequest.getCenterRequestFeeHold() != null) {
                refundRequest.getCenterRequestFeeHold().setStatus(CenterRequestFeeStatus.REFUNDED);
                refundRequest.getCenterRequestFeeHold().setRefundedAt(now);
                centerRequestFeeHoldRepository.save(refundRequest.getCenterRequestFeeHold());
            }
            notifyRefundTransferCompleted(refundRequest, tx.getAmount());
        });
        flagPayoutMethodUsage(tx.getPaymentMethod(), now);
    }

    private TopupStatusResponse completeTopup(
            PaymentTransaction tx,
            String externalTransactionId,
            String message) {

        if (tx.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return TopupStatusResponse.builder()
                    .reference(tx.getReferenceCode())
                    .status("SUCCESS")
                    .message("Giao dịch đã được ghi nhận trước đó.")
                    .wallet(toWalletResponse(walletService.getOrCreate(tx.getWallet().getWalletId())))
                    .build();
        }

        if (tx.getStatus() != PaymentTransactionStatus.PENDING) {
            return TopupStatusResponse.builder()
                    .reference(tx.getReferenceCode())
                    .status(tx.getStatus().name())
                    .message("Giao dịch không còn ở trạng thái chờ.")
                    .build();
        }

        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setExternalTransactionId(externalTransactionId);
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);

        walletService.credit(tx.getWallet().getWalletId(), tx.getAmount(), tx.getReferenceCode());
        flagPayoutMethodUsage(tx.getPaymentMethod(), LocalDateTime.now());
        Wallet wallet = walletService.getOrCreate(tx.getWallet().getWalletId());
        paymentNotificationService.notifyPayment(
                wallet.getWalletId(),
                "Nạp tiền thành công",
                "Ví của bạn đã được cộng " + formatAmount(tx.getAmount()) + ".",
                "PAYMENT_TRANSACTION",
                tx.getTransactionId());

        return TopupStatusResponse.builder()
                .reference(tx.getReferenceCode())
                .status("SUCCESS")
                .message(message)
                .wallet(toWalletResponse(wallet))
                .build();
    }

    private TopupStatusResponse toTopupStatusResponse(PaymentTransaction tx) {
        if (tx.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return TopupStatusResponse.builder()
                    .reference(tx.getReferenceCode())
                    .status("SUCCESS")
                    .message("Giao dịch thành công.")
                    .wallet(toWalletResponse(walletService.getOrCreate(tx.getWallet().getWalletId())))
                    .build();
        }

        if (tx.getStatus() == PaymentTransactionStatus.PENDING && isTopupExpired(tx)) {
            return TopupStatusResponse.builder()
                    .reference(tx.getReferenceCode())
                    .status("EXPIRED")
                    .message("QR đã hết hạn. Vui lòng tạo phiên nạp mới.")
                    .build();
        }

        return TopupStatusResponse.builder()
                .reference(tx.getReferenceCode())
                .status(tx.getStatus().name())
                .message("Chưa ghi nhận thanh toán. Vui lòng kiểm tra lại sau.")
                .build();
    }

    private boolean isTopupExpired(PaymentTransaction tx) {
        LocalDateTime createdAt = tx.getCreatedAt();
        return createdAt != null && LocalDateTime.now().isAfter(createdAt.plusMinutes(TOPUP_TTL_MINUTES));
    }

    private WithdrawalRequestStatus initialWithdrawalStatus(BigDecimal amount, List<String> riskFlags) {
        if (riskFlags != null && !riskFlags.isEmpty()) {
            return WithdrawalRequestStatus.PENDING;
        }
        if (withdrawalAutoApprovalThreshold == null
                || withdrawalAutoApprovalThreshold.compareTo(BigDecimal.ZERO) <= 0
                || amount == null) {
            return WithdrawalRequestStatus.PENDING;
        }
        return amount.compareTo(withdrawalAutoApprovalThreshold) <= 0
                ? WithdrawalRequestStatus.APPROVED
                : WithdrawalRequestStatus.PENDING;
    }

    private List<String> detectWithdrawalRiskFlags(
            Wallet wallet,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            LocalDateTime requestedAt) {
        List<String> flags = new ArrayList<>();
        if (wallet == null || paymentMethod == null || amount == null || requestedAt == null) {
            return flags;
        }

        if (isRecentlyChanged(paymentMethod, requestedAt, PAYOUT_CHANGE_REVIEW_HOURS)) {
            flags.add("Tài khoản nhận tiền vừa được thêm/cập nhật gần đây");
        }

        List<PaymentMethod> methods = activePaymentMethods(wallet);
        long recentPayoutMethods = methods.stream()
                .filter(method -> isRecentlyChanged(method, requestedAt, PAYOUT_CHANGE_REVIEW_HOURS))
                .count();
        if (methods.size() >= ACTIVE_PAYOUT_METHOD_REVIEW_THRESHOLD
                && recentPayoutMethods >= RECENT_PAYOUT_METHOD_REVIEW_THRESHOLD) {
            flags.add("Người dùng có nhiều tài khoản nhận tiền được thêm/cập nhật trong thời gian ngắn");
        }

        if (hasFastTopupWithdrawal(wallet, amount, requestedAt)) {
            flags.add("Ví vừa nạp tiền và tạo yêu cầu rút trong thời gian ngắn");
        }

        if (hasRepeatedRoundWithdrawal(wallet, amount, requestedAt)) {
            flags.add("Nhiều yêu cầu rút cùng số tiền tròn trong thời gian ngắn");
        }

        if (hasAggregatedInflowsWithdrawal(wallet, amount, requestedAt)) {
            flags.add("Nhiều khoản tiền vào gần đây được gom thành một yêu cầu rút lớn");
        }

        return flags;
    }

    private boolean isRecentlyChanged(PaymentMethod method, LocalDateTime now, int hours) {
        if (method == null || now == null) {
            return false;
        }
        LocalDateTime changedAt = method.getUpdatedAt() != null
                ? method.getUpdatedAt()
                : method.getCreatedAt();
        return changedAt != null && !changedAt.isAfter(now)
                && ChronoUnit.HOURS.between(changedAt, now) < hours;
    }

    private boolean hasFastTopupWithdrawal(Wallet wallet, BigDecimal amount, LocalDateTime requestedAt) {
        LocalDateTime from = requestedAt.minusHours(FAST_TOPUP_WITHDRAWAL_HOURS);
        List<PaymentTransaction> recentTopups = paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                        wallet.getWalletId(),
                        PaymentTransactionType.DEPOSIT,
                        PaymentTransactionStatus.SUCCESS,
                        from,
                        requestedAt);
        BigDecimal recentTopupAmount = sumTransactions(recentTopups);
        return recentTopupAmount.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(recentTopupAmount.multiply(AGGREGATED_INFLOW_RATIO)) >= 0;
    }

    private boolean hasRepeatedRoundWithdrawal(Wallet wallet, BigDecimal amount, LocalDateTime requestedAt) {
        if (!isRoundAmount(amount)) {
            return false;
        }
        LocalDateTime from = requestedAt.minusDays(ROUND_AMOUNT_REVIEW_DAYS);
        List<WithdrawalRequest> sameAmountWithdrawals = withdrawalRequestRepository
                .findByWallet_WalletIdAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
                        wallet.getWalletId(),
                        amount,
                        from,
                        requestedAt);
        if (sameAmountWithdrawals == null) {
            sameAmountWithdrawals = List.of();
        }
        return sameAmountWithdrawals.size() + 1 >= ROUND_AMOUNT_REPEAT_THRESHOLD;
    }

    private boolean hasAggregatedInflowsWithdrawal(Wallet wallet, BigDecimal amount, LocalDateTime requestedAt) {
        LocalDateTime from = requestedAt.minusHours(INFLOW_AGGREGATION_REVIEW_HOURS);
        List<PaymentTransaction> inflows = paymentTransactionRepository
                .findByWallet_WalletIdAndTypeInAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
                        wallet.getWalletId(),
                        List.of(PaymentTransactionType.DEPOSIT, PaymentTransactionType.ESCROW_RELEASE),
                        PaymentTransactionStatus.SUCCESS,
                        from,
                        requestedAt);
        if (inflows == null) {
            inflows = List.of();
        }
        if (inflows.size() < 3) {
            return false;
        }
        BigDecimal totalInflow = sumTransactions(inflows);
        return totalInflow.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(totalInflow.multiply(AGGREGATED_INFLOW_RATIO)) >= 0;
    }

    private boolean isRoundAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return amount.remainder(ROUND_AMOUNT_UNIT).compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal sumTransactions(List<PaymentTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return transactions.stream()
                .map(PaymentTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void notifyWithdrawalRiskReview(WithdrawalRequest withdrawal, List<String> riskFlags) {
        if (withdrawal == null || riskFlags == null || riskFlags.isEmpty()) {
            return;
        }
        Wallet wallet = withdrawal.getWallet();
        User owner = wallet != null ? wallet.getUser() : null;
        String ownerEmail = owner != null ? owner.getEmail() : "không rõ";
        String message = "Yêu cầu rút " + formatAmount(withdrawal.getAmount())
                + " từ " + ownerEmail
                + " được chuyển sang chờ xử lý thủ công vì: "
                + String.join("; ", riskFlags) + ".";
        auditPaymentRisk(
                owner != null ? owner.getUserId() : null,
                "WITHDRAWAL_RISK_REVIEW",
                "WithdrawalRequest",
                withdrawal.getWithdrawalId(),
                message);
        notifyRiskToAdmins(
                "Cảnh báo giao dịch rút tiền",
                message,
                "WITHDRAWAL_REQUEST",
                withdrawal.getWithdrawalId());
        paymentNotificationService.notifyPayment(
                owner,
                "Yêu cầu rút tiền đang chờ kiểm tra",
                "Yêu cầu rút " + formatAmount(withdrawal.getAmount())
                        + " đang chờ quản trị viên kiểm tra theo quy tắc an toàn giao dịch.",
                "WITHDRAWAL_REQUEST",
                withdrawal.getWithdrawalId());
    }

    private boolean isWithdrawalAwaitingTransfer(WithdrawalRequestStatus status) {
        return status == WithdrawalRequestStatus.PENDING || status == WithdrawalRequestStatus.APPROVED;
    }

    private String decisionReason(WithdrawalDecisionRequest request, String fallback) {
        if (request != null && !isBlank(request.getReason())) {
            return request.getReason().trim();
        }
        return fallback;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        return amount.setScale(0, RoundingMode.DOWN).toPlainString() + " đ";
    }

    private PaymentTransaction findSafeWithdrawalTransaction(WithdrawalRequest withdrawal) {
        if (withdrawal.getWallet() == null || withdrawal.getRequestedAt() == null || withdrawal.getAmount() == null) {
            return null;
        }

        LocalDateTime from = withdrawal.getRequestedAt().minusMinutes(WITHDRAWAL_MATCH_WINDOW_MINUTES);
        LocalDateTime to = withdrawal.getRequestedAt().plusMinutes(WITHDRAWAL_MATCH_WINDOW_MINUTES);
        List<PaymentTransaction> candidates =
                paymentTransactionRepository
                        .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                                withdrawal.getWallet().getWalletId(),
                                PaymentTransactionType.WITHDRAWAL,
                                PaymentTransactionStatus.PENDING,
                                withdrawal.getAmount(),
                                from,
                                to);

        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private PaymentTransaction findWithdrawalTransaction(WithdrawalRequest withdrawal) {
        if (withdrawal.getWallet() == null || withdrawal.getRequestedAt() == null || withdrawal.getAmount() == null) {
            return null;
        }

        LocalDateTime from = withdrawal.getRequestedAt().minusMinutes(WITHDRAWAL_MATCH_WINDOW_MINUTES);
        LocalDateTime to = withdrawal.getRequestedAt().plusMinutes(WITHDRAWAL_MATCH_WINDOW_MINUTES);
        List<PaymentTransaction> candidates =
                paymentTransactionRepository
                        .findByWallet_WalletIdAndTypeAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                                withdrawal.getWallet().getWalletId(),
                                PaymentTransactionType.WITHDRAWAL,
                                withdrawal.getAmount(),
                                from,
                                to);

        List<PaymentTransaction> matched = candidates.stream()
                .filter(tx -> paymentMethodMatches(withdrawal, tx))
                .toList();
        PaymentTransaction statusMatched = matched.stream()
                .filter(tx -> withdrawalStatusMatches(withdrawal.getStatus(), tx.getStatus()))
                .findFirst()
                .orElse(null);
        if (statusMatched != null) {
            return statusMatched;
        }
        return matched.size() == 1 ? matched.get(0) : null;
    }

    private boolean withdrawalStatusMatches(
            WithdrawalRequestStatus withdrawalStatus,
            PaymentTransactionStatus transactionStatus) {
        if (withdrawalStatus == null || transactionStatus == null) {
            return false;
        }
        return switch (withdrawalStatus) {
            case PENDING, APPROVED -> transactionStatus == PaymentTransactionStatus.PENDING;
            case COMPLETED -> transactionStatus == PaymentTransactionStatus.SUCCESS;
            case REJECTED -> transactionStatus == PaymentTransactionStatus.FAILED
                    || transactionStatus == PaymentTransactionStatus.CANCELLED;
        };
    }

    private TopupSessionResponse toTopupSessionResponse(
            String reference,
            BigDecimal amount,
            LocalDateTime expiresAt) {

        return TopupSessionResponse.builder()
                .reference(reference)
                .amount(amount)
                .status("PENDING")
                .qrUrl(buildQrUrl(amount, reference))
                .bankName(TOPUP_BANK_NAME)
                .bankBin(TOPUP_BANK_BIN)
                .accountNumber(TOPUP_ACCOUNT_NUMBER)
                .accountName(TOPUP_ACCOUNT_NAME)
                .transferContent(reference)
                .expiresAt(expiresAt)
                .expiresAtMillis(toEpochMillis(expiresAt))
                .build();
    }

    private String buildQrUrl(BigDecimal amount, String transferContent) {
        return "https://img.vietqr.io/image/"
                + TOPUP_BANK_BIN
                + "-"
                + TOPUP_ACCOUNT_NUMBER
                + "-compact2.png"
                + "?amount="
                + amount.setScale(0, RoundingMode.DOWN).toPlainString()
                + "&addInfo="
                + urlEncode(transferContent)
                + "&accountName="
                + urlEncode(TOPUP_ACCOUNT_NAME);
    }

    private long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String compact(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private WithdrawalRequestStatus parseWithdrawalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return WithdrawalRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Trạng thái yêu cầu rút tiền không hợp lệ: " + status);
        }
    }

    private void validateCreateRefundRequest(CreateRefundRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin yêu cầu hoàn tiền");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0");
        }
        if (isBlank(request.getReason())) {
            throw new IllegalArgumentException("Vui lòng nhập lý do yêu cầu hoàn tiền");
        }
        validatePaymentMethodRequest(request.getBankName(), request.getAccountNo());
        if (isBlank(request.getAccountHolderName())) {
            throw new IllegalArgumentException("Vui lòng nhập tên chủ tài khoản nhận hoàn tiền");
        }
        int selectorCount = countPresent(request.getEscrowId(), request.getAssignmentId(), request.getClassStudentId());
        if (selectorCount == 0) {
            throw new IllegalArgumentException("Cần cung cấp escrowId, assignmentId hoặc classStudentId");
        }
        if (selectorCount > 1) {
            throw new IllegalArgumentException("Chỉ được chọn một trong escrowId, assignmentId hoặc classStudentId");
        }
    }

    private EscrowTransaction resolveRefundEscrow(CreateRefundRequest request) {
        if (request.getEscrowId() != null) {
            return escrowTransactionRepository.findById(request.getEscrowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow"));
        }
        if (request.getAssignmentId() != null) {
            return escrowTransactionRepository.findByAssignment_AssignmentId(request.getAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow của phân công lớp"));
        }
        return escrowTransactionRepository.findByClassStudent_ClassStudentId(request.getClassStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow của ghi danh lớp"));
    }

    private int countPresent(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) {
                count++;
            }
        }
        return count;
    }

    private void requireRefundEscrowParticipant(EscrowTransaction escrow, Long userId) {
        if (!isPayer(escrow, userId)) {
            throw new ForbiddenException("Chỉ người thanh toán escrow mới có quyền gửi yêu cầu hoàn tiền");
        }
    }

    private boolean isPayer(EscrowTransaction escrow, Long userId) {
        if (escrow == null || userId == null) {
            return false;
        }
        ClassStudent classStudent = escrow.getClassStudent();
        if (classStudent != null && classStudent.getEnrolledByUser() != null) {
            return Objects.equals(classStudent.getEnrolledByUser().getUserId(), userId);
        }

        TutoringClass tutoringClass = resolveTutoringClass(escrow);
        if (tutoringClass != null && tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), userId)) {
            return true;
        }

        PaymentTransaction payment = escrow.getPayment();
        Wallet payerWallet = payment != null ? payment.getWallet() : null;
        if (payerWallet == null) {
            return false;
        }
        if (payerWallet.getUser() != null) {
            return Objects.equals(payerWallet.getUser().getUserId(), userId);
        }
        return Objects.equals(payerWallet.getWalletId(), userId);
    }

    private boolean isClassParticipant(TutoringClass tutoringClass, Long userId) {
        if (tutoringClass == null || userId == null) {
            return false;
        }
        if (tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), userId)) {
            return true;
        }
        return tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null
                && Objects.equals(tutoringClass.getCenter().getUser().getUserId(), userId);
    }

    private void ensureRefundEscrowOpen(EscrowTransaction escrow) {
        if (escrow == null) {
            throw new ResourceNotFoundException("Không tìm thấy escrow");
        }
        EscrowStatus status = escrow.getStatus();
        if (status == EscrowStatus.RELEASED || status == EscrowStatus.REFUNDED) {
            throw new BusinessException("Escrow đã tất toán nên không thể xử lý hoàn tiền");
        }
        if (status != EscrowStatus.FUNDED && status != EscrowStatus.ON_HOLD && status != EscrowStatus.DISPUTED) {
            throw new BusinessException("Chỉ escrow đã khóa tiền mới có thể tạo yêu cầu hoàn tiền");
        }
    }

    private RefundRequestStatus parseRefundStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return RefundRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Trạng thái yêu cầu hoàn tiền không hợp lệ: " + status);
        }
    }

    private RefundRequest requirePendingRefund(Long refundId) {
        if (refundId == null) {
            throw new IllegalArgumentException("refundId là bắt buộc");
        }
        RefundRequest refundRequest = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));
        if (refundRequest.getStatus() != RefundRequestStatus.PENDING) {
            throw new BusinessException("Chỉ yêu cầu hoàn tiền đang chờ xử lý mới có thể duyệt/từ chối");
        }
        return refundRequest;
    }

    private boolean canReviewRefundRequest(UserPrincipal reviewer, RefundRequest refundRequest) {
        if (reviewer == null || refundRequest == null) {
            return false;
        }
        if (reviewer.getRole() == UserRole.PLATFORM_ADMIN) {
            return true;
        }
        if (reviewer.getRole() != UserRole.TUTOR_CENTER) {
            return false;
        }
        return isOwnedCenterEscrow(refundRequest.getEscrowTransaction(), reviewer.getUserId());
    }

    private void requireCanReviewRefundRequest(UserPrincipal reviewer, RefundRequest refundRequest) {
        if (!canReviewRefundRequest(reviewer, refundRequest)) {
            throw new ForbiddenException("Bạn chỉ có quyền xử lý yêu cầu hoàn tiền của lớp trung tâm do mình quản lý");
        }
    }

    private boolean isOwnedCenterEscrow(EscrowTransaction escrow, Long centerUserId) {
        TutoringClass tutoringClass = resolveTutoringClass(escrow);
        if (tutoringClass == null || centerUserId == null || tutoringClass.getClassType() != ClassType.CENTER) {
            return false;
        }
        boolean ownsByCenterProfile = tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null
                && Objects.equals(tutoringClass.getCenter().getUser().getUserId(), centerUserId);
        boolean ownsByCreator = tutoringClass.getCreator() != null
                && Objects.equals(tutoringClass.getCreator().getUserId(), centerUserId);
        return ownsByCenterProfile || ownsByCreator;
    }

    private BigDecimal decisionAmount(RefundDecisionRequest request, BigDecimal fallback) {
        BigDecimal amount = request != null && request.getApprovedAmount() != null
                ? request.getApprovedAmount()
                : fallback;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn được duyệt phải lớn hơn 0");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String decisionReason(RefundDecisionRequest request, String fallback) {
        if (request != null && !isBlank(request.getReason())) {
            return request.getReason().trim();
        }
        return fallback;
    }

    private String appendDecisionNote(String originalReason, String title, String note) {
        RefundPayoutInfo payoutInfo = RefundPayoutInfoCodec.parseFromReason(originalReason);
        String baseReason = RefundPayoutInfoCodec.stripFromReason(originalReason);
        StringBuilder builder = new StringBuilder();
        if (!isBlank(baseReason)) {
            builder.append(baseReason.trim());
        }
        if (!isBlank(title) || !isBlank(note)) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("[")
                    .append(isBlank(title) ? "Quyết định" : title.trim())
                    .append("] ")
                    .append(isBlank(note) ? "" : note.trim());
        }
        return RefundPayoutInfoCodec.appendToReason(builder.toString(), payoutInfo);
    }

    private void restoreEscrowIfOnlyRefundHeld(EscrowTransaction escrow) {
        if (escrow == null || escrow.getEscrowId() == null || escrow.getStatus() != EscrowStatus.DISPUTED) {
            return;
        }
        boolean hasActiveDispute = disputeRepository.existsByEscrowTransaction_EscrowIdAndStatusNot(
                escrow.getEscrowId(),
                DisputeStatus.RESOLVED);
        boolean hasPendingRefund = refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                escrow.getEscrowId(),
                RefundRequestStatus.PENDING);
        if (!hasActiveDispute && !hasPendingRefund) {
            escrow.setStatus(EscrowStatus.FUNDED);
            escrowTransactionRepository.save(escrow);
        }
    }

    private void notifyRefundAdmins(RefundRequest refundRequest) {
        String classTitle = classTitle(refundRequest.getEscrowTransaction());
        String classSegment = classTitle != null ? " cho lớp \"" + classTitle + "\"" : "";
        for (PlatformAdmin admin : platformAdminRepository.findAll()) {
            paymentNotificationService.notifyPayment(
                    admin.getUser(),
                    "Có yêu cầu hoàn tiền mới",
                    "Yêu cầu hoàn " + formatAmount(refundRequest.getAmount())
                            + classSegment
                            + " đang chờ xử lý.",
                    "REFUND_REQUEST",
                    refundRequest.getRefundId());
        }
    }

    private void notifyRefundTransferCompleted(RefundRequest refundRequest, BigDecimal amount) {
        if (refundRequest == null) {
            return;
        }
        String content = "Khoản hoàn " + formatAmount(amount) + " đã được xác nhận qua SePay.";
        if (refundRequest.getCenterRequestFeeHold() != null) {
            Long centerUserId = refundRequest.getCenterRequestFeeHold().getCenterUserId();
            Long clientUserId = refundRequest.getCenterRequestFeeHold().getClientUserId();
            paymentNotificationService.notifyPayment(
                    centerUserId,
                    "Hoàn phí trung tâm đã chuyển khoản",
                    content,
                    "REFUND_REQUEST",
                    refundRequest.getRefundId());
            paymentNotificationService.notifyPayment(
                    clientUserId,
                    "Hoàn phí trung tâm đã chuyển khoản",
                    content,
                    "REFUND_REQUEST",
                    refundRequest.getRefundId());
            return;
        }
        User requester = refundRequest.getRequestedBy();
        paymentNotificationService.notifyPayment(
                requester,
                "Hoàn tiền đã chuyển khoản",
                content,
                "REFUND_REQUEST",
                refundRequest.getRefundId());

        Long payerUserId = safeEscrowPayerUserId(refundRequest.getEscrowTransaction());
        Long requesterId = requester != null ? requester.getUserId() : null;
        if (payerUserId != null && !Objects.equals(payerUserId, requesterId)) {
            paymentNotificationService.notifyPayment(
                    payerUserId,
                    "Hoàn tiền đã chuyển khoản",
                    content,
                    "REFUND_REQUEST",
                    refundRequest.getRefundId());
        }
    }

    private Long safeEscrowPayerUserId(EscrowTransaction escrow) {
        try {
            return escrowPayerUserId(escrow);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void notifyWithdrawalAdmins(WithdrawalRequest withdrawal) {
        if (withdrawal == null) {
            return;
        }
        String statusSegment = withdrawal.getStatus() == WithdrawalRequestStatus.APPROVED
                ? " đã được tự động duyệt và đang chờ chuyển khoản/đối soát."
                : " đang chờ quản trị viên duyệt.";
        String requesterSegment = withdrawalRequesterSegment(withdrawal);
        for (PlatformAdmin admin : platformAdminRepository.findAll()) {
            if (admin == null || admin.getUser() == null) {
                continue;
            }
            paymentNotificationService.notifyPayment(
                    admin.getUser(),
                    "Có yêu cầu rút tiền mới",
                    "Yêu cầu rút " + formatAmount(withdrawal.getAmount())
                            + requesterSegment
                            + statusSegment,
                    "WITHDRAWAL_REQUEST",
                    withdrawal.getWithdrawalId());
        }
    }

    private String withdrawalRequesterSegment(WithdrawalRequest withdrawal) {
        Wallet wallet = withdrawal != null ? withdrawal.getWallet() : null;
        User user = wallet != null ? wallet.getUser() : null;
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            return " từ " + user.getEmail();
        }
        if (wallet != null && wallet.getWalletId() != null) {
            return " từ ví #" + wallet.getWalletId();
        }
        return "";
    }

    private RefundRequestResponse toRefundRequestResponse(RefundRequest request) {
        EscrowTransaction escrow = request.getEscrowTransaction();
        CenterRequestFeeHold feeHold = request.getCenterRequestFeeHold();
        TutoringClass tutoringClass = resolveTutoringClass(escrow);
        User requester = request.getRequestedBy();
        RefundPayoutInfo payoutInfo = RefundPayoutInfoCodec.parseFromReason(request.getReason());
        String feeRequestTitle = feeHold == null
                ? null
                : classRequestStore.find(feeHold.getRequestId())
                        .map(data -> data.note() != null && !data.note().isBlank()
                                ? data.note()
                                : "Yêu cầu mở lớp #" + feeHold.getRequestId())
                        .orElse("Yêu cầu mở lớp #" + feeHold.getRequestId());
        return RefundRequestResponse.builder()
                .refundId(request.getRefundId())
                .escrowId(escrow != null ? escrow.getEscrowId() : null)
                .escrowStatus(escrow != null ? escrow.getStatus() : null)
                .requesterId(requester != null ? requester.getUserId() : null)
                .requesterEmail(requester != null ? requester.getEmail() : null)
                .classId(feeHold != null ? feeHold.getClassId()
                        : tutoringClass != null ? tutoringClass.getClassId() : null)
                .classTitle(feeHold != null ? feeRequestTitle
                        : tutoringClass != null ? tutoringClass.getTitle() : null)
                .assignmentId(feeHold != null ? feeHold.getAssignmentId()
                        : escrow != null && escrow.getAssignment() != null
                        ? escrow.getAssignment().getAssignmentId()
                        : null)
                .classStudentId(escrow != null && escrow.getClassStudent() != null
                        ? escrow.getClassStudent().getClassStudentId()
                        : null)
                .escrowAmount(feeHold != null ? feeHold.getProjectedEscrowAmount()
                        : escrow != null ? escrow.getAmount() : null)
                .amount(request.getAmount())
                .bankName(request.getBankName())
                .accountNoMasked(maskAccountNo(request.getAccountNo()))
                .accountHolderName(payoutInfo != null ? payoutInfo.accountHolderName() : null)
                .refundReferenceCode(request.getRefundReferenceCode())
                .transferStatus(request.getTransferStatus())
                .status(request.getStatus())
                .reason(RefundPayoutInfoCodec.stripFromReason(request.getReason()))
                .requestedAt(request.getRequestedAt())
                .processedAt(request.getProcessedAt())
                .transferProcessedAt(request.getTransferProcessedAt())
                .build();
    }

    private TutoringClass resolveTutoringClass(EscrowTransaction escrow) {
        if (escrow == null) {
            return null;
        }
        ClassAssignment assignment = escrow.getAssignment();
        if (assignment != null
                && assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null) {
            return assignment.getApplication().getTutoringClass();
        }
        ClassStudent classStudent = escrow.getClassStudent();
        return classStudent != null ? classStudent.getTutoringClass() : null;
    }

    private String classTitle(EscrowTransaction escrow) {
        TutoringClass tutoringClass = resolveTutoringClass(escrow);
        return tutoringClass != null ? tutoringClass.getTitle() : null;
    }

    private String refundReferenceCode(Long escrowId) {
        return "REFUND-ESCROW-" + escrowId;
    }

    private String centerRequestFeeRefundReferenceCode(Long feeHoldId) {
        return "REFUND-CREQFEE-" + feeHoldId;
    }

    private boolean escrowPaymentUsesPayerWallet(EscrowTransaction escrow) {
        if (escrow == null || escrow.getPayment() == null || escrow.getPayment().getWallet() == null) {
            return false;
        }
        Long payerUserId = escrowPayerUserId(escrow);
        Wallet wallet = escrow.getPayment().getWallet();
        return (wallet.getUser() != null && Objects.equals(wallet.getUser().getUserId(), payerUserId))
                || Objects.equals(wallet.getWalletId(), payerUserId);
    }

    private Long escrowPayerUserId(EscrowTransaction escrow) {
        ClassStudent classStudent = escrow != null ? escrow.getClassStudent() : null;
        if (classStudent != null && classStudent.getEnrolledByUser() != null) {
            return classStudent.getEnrolledByUser().getUserId();
        }

        TutoringClass tutoringClass = resolveTutoringClass(escrow);
        if (tutoringClass != null && tutoringClass.getCreator() != null) {
            return tutoringClass.getCreator().getUserId();
        }

        PaymentTransaction payment = escrow != null ? escrow.getPayment() : null;
        Wallet wallet = payment != null ? payment.getWallet() : null;
        if (wallet != null && wallet.getUser() != null) {
            return wallet.getUser().getUserId();
        }
        if (wallet != null) {
            return wallet.getWalletId();
        }
        throw new BusinessException("Không xác định được người thanh toán escrow");
    }

    private Long escrowBeneficiaryUserId(EscrowTransaction escrow) {
        if (escrow != null
                && escrow.getAssignment() != null
                && escrow.getAssignment().getTutor() != null
                && escrow.getAssignment().getTutor().getUser() != null) {
            return escrow.getAssignment().getTutor().getUser().getUserId();
        }

        TutoringClass tutoringClass = resolveTutoringClass(escrow);
        if (tutoringClass != null
                && tutoringClass.getCenter() != null
                && tutoringClass.getCenter().getUser() != null) {
            return tutoringClass.getCenter().getUser().getUserId();
        }
        if (tutoringClass != null && tutoringClass.getCreator() != null) {
            return tutoringClass.getCreator().getUserId();
        }
        throw new BusinessException("Không xác định được người nhận escrow");
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private List<PaymentMethod> activePaymentMethods(Wallet wallet) {
        List<PaymentMethod> methods = paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByLastUsedAtDescPaymentMethodIdAsc(
                wallet.getWalletId(),
                PAYMENT_METHOD_ACTIVE);
        return methods != null ? methods : List.of();
    }

    private Long defaultPaymentMethodId(List<PaymentMethod> methods) {
        return methods.isEmpty() ? null : methods.get(0).getPaymentMethodId();
    }

    private boolean isDefaultPaymentMethod(Wallet wallet, PaymentMethod method) {
        return Objects.equals(method.getPaymentMethodId(), defaultPaymentMethodId(activePaymentMethods(wallet)));
    }

    private void ensurePaymentMethodCanBeUsed(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return;
        }
        if (isBlank(paymentMethod.getAccountHolderName())) {
            throw new BusinessException("Vui lòng cập nhật tên chủ tài khoản nhận tiền trước khi rút tiền");
        }
        if (!isCenterPayoutMethod(paymentMethod)) {
            return;
        }
        LocalDateTime cooldownUntil = paymentMethod.getCooldownUntil();
        if (cooldownUntil != null && LocalDateTime.now().isBefore(cooldownUntil)) {
            throw new BusinessException("Tài khoản nhận tiền mới cần chờ một thời gian trước khi rút tiền");
        }
    }

    private String resolveWithdrawalAccountHolderName(CreateWithdrawalRequest request, PaymentMethod paymentMethod) {
        if (!isBlank(request.getAccountHolderName())) {
            return request.getAccountHolderName().trim().replaceAll("\\s+", " ");
        }
        if (paymentMethod != null && !isBlank(paymentMethod.getAccountHolderName())) {
            return paymentMethod.getAccountHolderName().trim().replaceAll("\\s+", " ");
        }
        return null;
    }

    private void flagPayoutMethodUsage(PaymentMethod method, LocalDateTime usedAt) {
        if (method == null || usedAt == null) {
            return;
        }
        method.setLastUsedAt(usedAt);
        if (isCenterPayoutMethod(method) && method.getCooldownUntil() == null) {
            method.setCooldownUntil(payoutCooldownUntil(method, usedAt));
        }
        paymentMethodRepository.save(method);
    }

    private PaymentMethod requireOwnedActivePaymentMethod(Wallet wallet, Long paymentMethodId) {
        if (paymentMethodId == null) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản nhận tiền");
        }
        return paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(
                        paymentMethodId,
                        wallet.getWalletId(),
                        PAYMENT_METHOD_ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản nhận tiền"));
    }

    private PaymentMethodData validatePaymentMethodRequest(PaymentMethodRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin tài khoản nhận tiền");
        }
        return validatePaymentMethodRequest(request.getBankName(), request.getAccountNo(), request.getAccountHolderName());
    }

    private PaymentMethodData validatePaymentMethodRequest(String bankName, String accountNo) {
        if (isBlank(bankName)) {
            throw new IllegalArgumentException("Vui lòng nhập tên ngân hàng nhận tiền");
        }
        if (isBlank(accountNo)) {
            throw new IllegalArgumentException("Vui lòng nhập số tài khoản nhận tiền");
        }

        String normalizedBankName = bankName.trim().replaceAll("\\s+", " ");
        String normalizedAccountNo = accountNo.trim().replaceAll("\\s+", "");
        if (normalizedBankName.length() > 100) {
            throw new IllegalArgumentException("Tên ngân hàng không được vượt quá 100 ký tự");
        }
        if (!normalizedAccountNo.matches("[A-Za-z0-9]{4,50}")) {
            throw new IllegalArgumentException("Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự");
        }
        return new PaymentMethodData(normalizedBankName, normalizedAccountNo, null);
    }

    private PaymentMethodData validatePaymentMethodRequest(
            String bankName,
            String accountNo,
            String accountHolderName) {
        PaymentMethodData base = validatePaymentMethodRequest(bankName, accountNo);
        if (isBlank(accountHolderName)) {
            throw new IllegalArgumentException("Vui lòng nhập tên chủ tài khoản nhận tiền");
        }
        String normalizedHolderName = accountHolderName.trim().replaceAll("\\s+", " ");
        if (normalizedHolderName.length() > 150) {
            throw new IllegalArgumentException("Tên chủ tài khoản không được vượt quá 150 ký tự");
        }
        return new PaymentMethodData(base.bankName(), base.accountNo(), normalizedHolderName);
    }

    private RefundPayoutInfo validateRefundPayoutInfo(String bankName, String accountNo, String accountHolderName) {
        PaymentMethodData data = validatePaymentMethodRequest(bankName, accountNo, accountHolderName);
        return new RefundPayoutInfo(
                data.bankName(),
                data.accountNo(),
                data.accountHolderName());
    }

    private RefundPayoutInfo resolveRefundPayoutInfo(RefundRequest request) {
        if (request == null) {
            return null;
        }
        RefundPayoutInfo parsed = RefundPayoutInfoCodec.parseFromReason(request.getReason());
        if (RefundPayoutInfoCodec.hasCompletePayout(parsed)) {
            return parsed;
        }
        if (!isBlank(request.getBankName()) && !isBlank(request.getAccountNo()) && !isBlank(request.getAccountHolderName())) {
            return new RefundPayoutInfo(
                    RefundPayoutInfoCodec.normalize(request.getBankName()),
                    RefundPayoutInfoCodec.normalizeAccountNo(request.getAccountNo()),
                    RefundPayoutInfoCodec.normalize(request.getAccountHolderName()));
        }
        return parsed;
    }

    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod method, boolean isDefault) {
        return PaymentMethodResponse.builder()
                .paymentMethodId(method.getPaymentMethodId())
                .type(method.getType())
                .provider(method.getBankName())
                .bankName(method.getBankName())
                .accountHolderName(method.getAccountHolderName())
                .lastFour(lastFour(method.getAccountNo()))
                .accountNoMasked(maskAccountNo(method.getAccountNo()))
                .isDefault(isDefault)
                .verifiedAt(method.getVerifiedAt())
                .cooldownUntil(isCenterPayoutMethod(method) ? method.getCooldownUntil() : null)
                .lastUsedAt(method.getLastUsedAt())
                .createdAt(method.getCreatedAt())
                .updatedAt(method.getUpdatedAt())
                .build();
    }

    private void maybeWarnAnomalousPaymentMethod(PaymentMethod method) {
        if (method == null || !isCenterPayoutMethod(method)) {
            return;
        }
        LocalDateTime referenceTime = method.getVerifiedAt() != null
                ? method.getVerifiedAt()
                : method.getCreatedAt();
        if (referenceTime == null) {
            return;
        }
        if (isWithinPayoutCooldown(method, referenceTime, LocalDateTime.now())) {
            String owner = method.getWallet() != null && method.getWallet().getUser() != null
                    ? method.getWallet().getUser().getEmail()
                    : "không rõ";
            auditPaymentRisk(
                    method.getWallet() != null && method.getWallet().getUser() != null
                            ? method.getWallet().getUser().getUserId()
                            : null,
                    "PAYOUT_METHOD_COOLING_OFF",
                    "PaymentMethod",
                    method.getPaymentMethodId(),
                    "Tài khoản nhận tiền mới/đổi thông tin đang trong thời gian chờ");
            notifyRiskToAdmins(
                    "Cảnh báo tài khoản nhận tiền mới",
                    "Người dùng " + owner + " vừa thêm/cập nhật tài khoản nhận tiền. "
                            + "Tài khoản đang trong thời gian chờ trước khi được rút tiền.",
                    "PAYMENT_METHOD",
                    method.getPaymentMethodId());
            paymentNotificationService.notifyPayment(
                    method.getWallet() != null && method.getWallet().getUser() != null
                            ? method.getWallet().getUser()
                            : null,
                    "Tài khoản nhận tiền mới",
                    "Tài khoản nhận tiền vừa được thêm/cập nhật và đang ở thời gian chờ.",
                    "PAYMENT_METHOD",
                    method.getPaymentMethodId());
        }
    }

    private void maybeWarnSuspiciousWithdrawal(WithdrawalRequest withdrawal, PaymentMethod paymentMethod) {
        if (withdrawal == null || paymentMethod == null || !isCenterPayoutMethod(paymentMethod)) {
            return;
        }
        boolean newMethod = isWithinPayoutCooldown(paymentMethod, paymentMethod.getCreatedAt(), withdrawal.getRequestedAt());
        if (!newMethod) {
            return;
        }
        Wallet wallet = withdrawal.getWallet();
        User owner = wallet != null ? wallet.getUser() : null;
        String ownerEmail = owner != null ? owner.getEmail() : "không rõ";
        String message = "Yêu cầu rút " + formatAmount(withdrawal.getAmount())
                + " từ " + ownerEmail
                + " dùng tài khoản nhận tiền mới/đổi gần đây, cần theo dõi thủ công.";
        auditPaymentRisk(
                owner != null ? owner.getUserId() : null,
                "WITHDRAWAL_USING_NEW_PAYOUT_METHOD",
                "WithdrawalRequest",
                withdrawal.getWithdrawalId(),
                message);
        notifyRiskToAdmins(
                "Cảnh báo rút tiền cần theo dõi",
                message,
                "WITHDRAWAL_REQUEST",
                withdrawal.getWithdrawalId());
    }

    private void notifyRiskToAdmins(String title, String content, String referenceType, Long referenceId) {
        List<PlatformAdmin> admins = platformAdminRepository.findAll();
        if (admins == null) {
            return;
        }
        for (PlatformAdmin admin : admins) {
            if (admin == null || admin.getUser() == null) {
                continue;
            }
            paymentNotificationService.notifyPayment(
                    admin.getUser(),
                    title,
                    content,
                    referenceType,
                    referenceId);
        }
    }

    private void auditPaymentRisk(
            Long actorUserId,
            String action,
            String entityType,
            Long entityId,
            String message) {
        try {
            auditLogService.record(
                    actorUserId,
                    action,
                    entityType,
                    entityId,
                    null,
                    java.util.Map.of("message", message));
        } catch (RuntimeException ignored) {
            // Audit should not block a payment flow.
        }
    }

    private void applyPayoutCooldown(PaymentMethod method) {
        if (method == null) {
            return;
        }
        if (!isCenterPayoutMethod(method)) {
            method.setCooldownUntil(null);
            return;
        }
        LocalDateTime verifiedAt = method.getVerifiedAt() != null ? method.getVerifiedAt() : LocalDateTime.now();
        method.setCooldownUntil(payoutCooldownUntil(method, verifiedAt));
    }

    private LocalDateTime payoutCooldownUntil(PaymentMethod method, LocalDateTime from) {
        if (isCenterPayoutMethod(method)) {
            return from.plusMinutes(CENTER_PAYOUT_COOLDOWN_MINUTES);
        }
        return null;
    }

    private boolean isWithinPayoutCooldown(PaymentMethod method, LocalDateTime referenceTime, LocalDateTime checkedAt) {
        if (referenceTime == null || checkedAt == null) {
            return false;
        }
        long cooldownMinutes = payoutCooldownMinutes(method);
        return cooldownMinutes > 0 && ChronoUnit.MINUTES.between(referenceTime, checkedAt) < cooldownMinutes;
    }

    private long payoutCooldownMinutes(PaymentMethod method) {
        return isCenterPayoutMethod(method) ? CENTER_PAYOUT_COOLDOWN_MINUTES : 0L;
    }

    private boolean isCenterPayoutMethod(PaymentMethod method) {
        Wallet wallet = method != null ? method.getWallet() : null;
        User owner = wallet != null ? wallet.getUser() : null;
        Long ownerId = owner != null ? owner.getUserId() : null;
        return ownerId != null && tutorCenterRepository.findByUser_UserId(ownerId).isPresent();
    }

    private String lastFour(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return "";
        }
        String trimmed = accountNo.trim();
        return trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .balance(wallet.getAvailableBalance())
                .availableBalance(wallet.getAvailableBalance())
                .frozenBalance(wallet.getFrozenBalance())
                .status(wallet.getStatus())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private TransactionResponse toTransactionResponse(PaymentTransaction tx) {
        return TransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .type(tx.getType())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .referenceCode(tx.getReferenceCode())
                .processedAt(tx.getProcessedAt())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private AdminWithdrawalResponse toAdminWithdrawalResponse(
            WithdrawalRequest withdrawal,
            PaymentTransaction tx) {
        Wallet wallet = withdrawal.getWallet();
        PaymentMethod paymentMethod = withdrawal.getPaymentMethod();
        return AdminWithdrawalResponse.builder()
                .withdrawalId(withdrawal.getWithdrawalId())
                .refundId(null)
                .requestType("WITHDRAWAL")
                .walletId(wallet != null ? wallet.getWalletId() : null)
                .requesterEmail(wallet != null && wallet.getUser() != null ? wallet.getUser().getEmail() : null)
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus())
                .paymentMethodId(paymentMethod != null ? paymentMethod.getPaymentMethodId() : null)
                .bankName(paymentMethod != null ? paymentMethod.getBankName() : null)
                .accountNo(paymentMethod != null ? paymentMethod.getAccountNo() : null)
                .accountNoMasked(paymentMethod != null ? maskAccountNo(paymentMethod.getAccountNo()) : "")
                .accountHolderName(withdrawal.getAccountHolderName() != null
                        ? withdrawal.getAccountHolderName()
                        : paymentMethod != null ? paymentMethod.getAccountHolderName() : null)
                .transactionId(tx != null ? tx.getTransactionId() : null)
                .transactionStatus(tx != null ? tx.getStatus() : null)
                .referenceCode(tx != null ? tx.getReferenceCode() : null)
                .externalTransactionId(tx != null ? tx.getExternalTransactionId() : null)
                .requestedAt(withdrawal.getRequestedAt())
                .processedAt(withdrawal.getProcessedAt())
                .failureReason(withdrawal.getFailureReason())
                .build();
    }

    private AdminWithdrawalResponse toAdminRefundTransferResponse(RefundRequest refundRequest) {
        PaymentTransaction tx = findRefundTransaction(refundRequest);
        PaymentTransactionStatus transactionStatus = tx != null
                ? tx.getStatus()
                : refundTransferPaymentStatus(refundRequest);
        return AdminWithdrawalResponse.builder()
                .withdrawalId(null)
                .refundId(refundRequest.getRefundId())
                .requestType("REFUND")
                .walletId(null)
                .requesterEmail(refundRecipientEmail(refundRequest))
                .amount(refundRequest.getAmount())
                .status(refundTransferWithdrawalStatus(refundRequest))
                .paymentMethodId(null)
                .bankName(refundRequest.getBankName())
                .accountNo(refundRequest.getAccountNo())
                .accountNoMasked(maskAccountNo(refundRequest.getAccountNo()))
                .accountHolderName(refundRequest.getAccountHolderName())
                .transactionId(tx != null ? tx.getTransactionId() : null)
                .transactionStatus(transactionStatus)
                .referenceCode(refundRequest.getRefundReferenceCode())
                .externalTransactionId(tx != null ? tx.getExternalTransactionId() : null)
                .requestedAt(refundRequest.getProcessedAt() != null
                        ? refundRequest.getProcessedAt()
                        : refundRequest.getRequestedAt())
                .processedAt(refundRequest.getTransferProcessedAt())
                .failureReason(null)
                .build();
    }

    private boolean isRefundTransferRequest(RefundRequest refundRequest) {
        return refundRequest != null
                && amountOrZero(refundRequest.getAmount()).compareTo(BigDecimal.ZERO) > 0
                && !isBlank(refundRequest.getRefundReferenceCode())
                && refundRequest.getStatus() != RefundRequestStatus.REJECTED
                && !isBlank(refundRequest.getBankName())
                && !isBlank(refundRequest.getAccountNo())
                && !isBlank(refundRequest.getAccountHolderName());
    }

    private PaymentTransaction findRefundTransaction(RefundRequest refundRequest) {
        if (refundRequest == null || isBlank(refundRequest.getRefundReferenceCode())) {
            return null;
        }
        return paymentTransactionRepository.findByReferenceCode(refundRequest.getRefundReferenceCode()).orElse(null);
    }

    private WithdrawalRequestStatus refundTransferWithdrawalStatus(RefundRequest refundRequest) {
        String transferStatus = refundRequest.getTransferStatus();
        if ("SUCCESS".equalsIgnoreCase(transferStatus)) {
            return WithdrawalRequestStatus.COMPLETED;
        }
        if ("FAILED".equalsIgnoreCase(transferStatus) || "CANCELLED".equalsIgnoreCase(transferStatus)) {
            return WithdrawalRequestStatus.REJECTED;
        }
        return WithdrawalRequestStatus.APPROVED;
    }

    private PaymentTransactionStatus refundTransferPaymentStatus(RefundRequest refundRequest) {
        String transferStatus = refundRequest != null ? refundRequest.getTransferStatus() : null;
        if ("SUCCESS".equalsIgnoreCase(transferStatus)) {
            return PaymentTransactionStatus.SUCCESS;
        }
        if ("FAILED".equalsIgnoreCase(transferStatus)) {
            return PaymentTransactionStatus.FAILED;
        }
        if ("CANCELLED".equalsIgnoreCase(transferStatus)) {
            return PaymentTransactionStatus.CANCELLED;
        }
        return PaymentTransactionStatus.PENDING;
    }

    private String refundRecipientEmail(RefundRequest refundRequest) {
        if (refundRequest == null) {
            return null;
        }
        if (refundRequest.getCenterRequestFeeHold() != null
                && refundRequest.getCenterRequestFeeHold().getClientUserId() != null) {
            return userRepository.findById(refundRequest.getCenterRequestFeeHold().getClientUserId())
                    .map(User::getEmail)
                    .orElseGet(() -> requestedByEmail(refundRequest));
        }
        Long payerUserId = safeEscrowPayerUserId(refundRequest.getEscrowTransaction());
        if (payerUserId != null) {
            return userRepository.findById(payerUserId)
                    .map(User::getEmail)
                    .orElseGet(() -> requestedByEmail(refundRequest));
        }
        return requestedByEmail(refundRequest);
    }

    private String requestedByEmail(RefundRequest refundRequest) {
        User requestedBy = refundRequest != null ? refundRequest.getRequestedBy() : null;
        return requestedBy != null ? requestedBy.getEmail() : null;
    }

    private WithdrawalResponse toWithdrawalResponse(
            WithdrawalRequest withdrawal,
            PaymentTransaction tx,
            Wallet wallet) {
        PaymentMethod paymentMethod = withdrawal.getPaymentMethod();
        return WithdrawalResponse.builder()
                .withdrawalId(withdrawal.getWithdrawalId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus())
                .paymentMethodId(paymentMethod.getPaymentMethodId())
                .bankName(paymentMethod.getBankName())
                .accountNoMasked(maskAccountNo(paymentMethod.getAccountNo()))
                .accountHolderName(withdrawal.getAccountHolderName())
                .referenceCode(tx.getReferenceCode())
                .requestedAt(withdrawal.getRequestedAt())
                .wallet(toWalletResponse(wallet))
                .build();
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return "";
        }
        String trimmed = accountNo.trim();
        if (trimmed.length() <= 4) {
            return trimmed;
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private record PaymentMethodData(String bankName, String accountNo, String accountHolderName) {
    }

    private record WithdrawalMatch(WithdrawalRequest withdrawal, PaymentTransaction tx) {
    }
}
