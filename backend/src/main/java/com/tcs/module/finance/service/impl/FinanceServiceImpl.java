package com.tcs.module.finance.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.PaymentMethodRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.TopupStatusResponse;
import com.tcs.module.finance.dto.response.TransactionResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.entity.PaymentMethod;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.FinanceService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int TOPUP_TTL_MINUTES = 15;
    private static final int WITHDRAWAL_MATCH_WINDOW_MINUTES = 5;
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

    @Override
    @Transactional
    public WalletResponse getMyWallet() {
        return toWalletResponse(currentWallet());
    }

    @Override
    @Transactional
    public WalletResponse deposit(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        Long userId = authHelper.currentUserId();
        Wallet wallet = walletService.getOrCreate(userId);
        String referenceCode = "TOPUP-" + UUID.randomUUID();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription() != null ? request.getDescription() : "Nạp tiền ví");
        tx.setReferenceCode(referenceCode);
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);

        walletService.credit(userId, request.getAmount(), referenceCode);
        return toWalletResponse(walletService.getOrCreate(userId));
    }

    @Override
    @Transactional
    public TopupSessionResponse createTopup(DepositRequest request) {
        validateTopupAmount(request);

        Long userId = authHelper.currentUserId();
        Wallet wallet = walletService.getOrCreate(userId);
        String reference = createTopupReference();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOPUP_TTL_MINUTES);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Nạp tiền ví qua VietQR");
        tx.setReferenceCode(reference);
        paymentTransactionRepository.save(tx);

        return toTopupSessionResponse(reference, request.getAmount(), expiresAt);
    }

    @Override
    @Transactional
    public TopupStatusResponse getTopupStatus(String reference) {
        PaymentTransaction tx = requireOwnedTopup(reference);
        return toTopupStatusResponse(tx);
    }

    @Override
    @Transactional
    public TopupStatusResponse simulateTopupSuccess(String reference) {
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
        if (paymentTransactionRepository.findByExternalTransactionId(externalTransactionId).isPresent()) {
            return PaymentWebhookResponse.builder()
                    .status("success")
                    .message("Webhook đã được xử lý trước đó")
                    .build();
        }

        PaymentTransaction matched = findMatchingTopup(request);
        if (matched == null) {
            return PaymentWebhookResponse.builder()
                    .status("ignored")
                    .message("Không tìm thấy giao dịch khớp số tiền, nội dung và tài khoản")
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
            return PaymentWebhookResponse.builder()
                    .status("ignored")
                    .message("Không tìm thấy yêu cầu rút tiền khớp số tiền, nội dung và tài khoản")
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
                    paymentMethod.setStatus(PAYMENT_METHOD_ACTIVE);
                    return paymentMethodRepository.save(paymentMethod);
                });

        boolean isDefault = activeMethods.isEmpty()
                || Objects.equals(method.getPaymentMethodId(), defaultPaymentMethodId(activeMethods));
        return toPaymentMethodResponse(method, isDefault);
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
        PaymentMethod saved = paymentMethodRepository.save(method);
        return toPaymentMethodResponse(saved, isDefaultPaymentMethod(wallet, saved));
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

        Long userId = authHelper.currentUserId();
        Wallet wallet = walletService.getOrCreate(userId);
        PaymentMethod paymentMethod = resolveWithdrawalMethod(wallet, request);
        String referenceCode = "WITHDRAW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Wallet updatedWallet = walletService.lockFunds(userId, request.getAmount(), referenceCode);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(updatedWallet);
        tx.setPaymentMethod(paymentMethod);
        tx.setType(PaymentTransactionType.WITHDRAWAL);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(request.getAmount());
        tx.setDescription("Yêu cầu rút tiền đang chờ xử lý");
        tx.setReferenceCode(referenceCode);
        paymentTransactionRepository.save(tx);

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWallet(updatedWallet);
        withdrawal.setPaymentMethod(paymentMethod);
        withdrawal.setAmount(request.getAmount());
        withdrawal.setStatus(WithdrawalRequestStatus.PENDING);
        withdrawal.setRequestedAt(LocalDateTime.now());
        WithdrawalRequest savedWithdrawal = withdrawalRequestRepository.save(withdrawal);

        return toWithdrawalResponse(savedWithdrawal, tx, updatedWallet);
    }

    @Override
    @Transactional
    public WithdrawalResponse acceptWithdrawal(Long withdrawalId) {
        authHelper.requireRole(UserRole.PLATFORM_ADMIN);

        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu rút tiền"));
        if (withdrawal.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ yêu cầu rút tiền đang xử lý mới có thể được duyệt");
        }

        PaymentTransaction tx = findSafeWithdrawalTransaction(withdrawal);
        if (tx == null) {
            throw new IllegalArgumentException("Không xác định được giao dịch rút tiền tương ứng");
        }

        return completeWithdrawal(withdrawal, tx, null, "Yêu cầu rút tiền đã được duyệt");
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
        return walletService.getOrCreate(authHelper.currentUserId());
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
            if (isBlank(request.getBankName())) {
                throw new IllegalArgumentException("Vui lòng nhập tên ngân hàng nhận tiền");
            }
            if (isBlank(request.getAccountNo())) {
                throw new IllegalArgumentException("Vui lòng nhập số tài khoản nhận tiền");
            }
        }
    }

    private PaymentMethod resolveWithdrawalMethod(Wallet wallet, CreateWithdrawalRequest request) {
        if (request.getPaymentMethodId() != null) {
            return requireOwnedActivePaymentMethod(wallet, request.getPaymentMethodId());
        }

        PaymentMethodData data = validatePaymentMethodRequest(request.getBankName(), request.getAccountNo());
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
        List<PaymentTransaction> candidates = paymentTransactionRepository.findByTypeAndStatusAndAmount(
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
                || withdrawal.getStatus() != WithdrawalRequestStatus.PENDING
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
        return paymentTransactionRepository.findByTypeAndStatusAndAmount(
                        PaymentTransactionType.WITHDRAWAL,
                        PaymentTransactionStatus.PENDING,
                        request.getTransferAmount())
                .stream()
                .filter(tx -> transferContentMatches(tx, payload))
                .toList();
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
        List<WithdrawalRequest> candidates =
                withdrawalRequestRepository
                        .findByWallet_WalletIdAndStatusAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
                                tx.getWallet().getWalletId(),
                                WithdrawalRequestStatus.PENDING,
                                tx.getAmount(),
                                from,
                                to);

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

        return toWithdrawalResponse(savedWithdrawal, tx, wallet);
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
        Wallet wallet = walletService.getOrCreate(tx.getWallet().getWalletId());

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

    private List<PaymentMethod> activePaymentMethods(Wallet wallet) {
        return paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByPaymentMethodIdAsc(
                wallet.getWalletId(),
                PAYMENT_METHOD_ACTIVE);
    }

    private Long defaultPaymentMethodId(List<PaymentMethod> methods) {
        return methods.isEmpty() ? null : methods.get(0).getPaymentMethodId();
    }

    private boolean isDefaultPaymentMethod(Wallet wallet, PaymentMethod method) {
        return Objects.equals(method.getPaymentMethodId(), defaultPaymentMethodId(activePaymentMethods(wallet)));
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
        return validatePaymentMethodRequest(request.getBankName(), request.getAccountNo());
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
        return new PaymentMethodData(normalizedBankName, normalizedAccountNo);
    }

    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod method, boolean isDefault) {
        return PaymentMethodResponse.builder()
                .paymentMethodId(method.getPaymentMethodId())
                .type(method.getType())
                .provider(method.getBankName())
                .bankName(method.getBankName())
                .lastFour(lastFour(method.getAccountNo()))
                .accountNoMasked(maskAccountNo(method.getAccountNo()))
                .isDefault(isDefault)
                .build();
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

    private record PaymentMethodData(String bankName, String accountNo) {
    }

    private record WithdrawalMatch(WithdrawalRequest withdrawal, PaymentTransaction tx) {
    }
}
