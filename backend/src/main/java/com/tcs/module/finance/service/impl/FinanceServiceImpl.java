package com.tcs.module.finance.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.request.AddPaymentMethodRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.ReviewWithdrawalRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.AdminWithdrawalResponse;
import com.tcs.module.finance.dto.response.DepositResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.TransactionResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.entity.PaymentMethod;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.FinanceService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private static final Logger log = LoggerFactory.getLogger(FinanceServiceImpl.class);

    private static final String REF_PREFIX = "TCS";
    private static final Pattern REF_PATTERN = Pattern.compile(REF_PREFIX + "[0-9A-Z]{6}");
    private static final char[] REF_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final String PM_ACTIVE = "ACTIVE";
    private static final String PM_INACTIVE = "INACTIVE";

    private final AuthHelper authHelper;
    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;

    @Value("${app.deposit.bank-code:MB}")
    private String bankCode;

    @Value("${app.deposit.bank-name:MBBank}")
    private String bankName;

    @Value("${app.deposit.account-no:0858500038}")
    private String accountNo;

    @Value("${app.deposit.account-name:VU QUOC KHANH}")
    private String accountName;

    @Value("${app.deposit.qr-template:compact2}")
    private String qrTemplate;

    // ===================== Vi =====================

    @Override
    @Transactional
    public WalletResponse getMyWallet() {
        return toWalletResponse(getOrCreateWallet());
    }

    // ===================== Nap tien =====================

    @Override
    @Transactional
    public DepositResponse createDeposit(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("So tien nap phai lon hon 0");
        }
        Wallet wallet = getOrCreateWallet();
        String referenceCode = generateReferenceCode();

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription() != null ? request.getDescription() : "Nap tien vi");
        tx.setReferenceCode(referenceCode);
        paymentTransactionRepository.save(tx);

        return DepositResponse.builder()
                .transactionId(tx.getTransactionId())
                .referenceCode(referenceCode)
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .qrImageUrl(buildVietQrUrl(tx.getAmount(), referenceCode))
                .bankName(bankName)
                .accountNo(accountNo)
                .accountName(accountName)
                .transferContent(referenceCode)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getMyTransactions() {
        Wallet wallet = getOrCreateWallet();
        return paymentTransactionRepository
                .findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId()).stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Override
    @Transactional
    public void handleSepayWebhook(SepayWebhookRequest request) {
        if (request.getTransferType() != null && !"in".equalsIgnoreCase(request.getTransferType())) {
            log.info("SePay webhook bo qua transferType={}", request.getTransferType());
            return;
        }
        if (request.getId() != null
                && paymentTransactionRepository.existsByExternalTransactionId(String.valueOf(request.getId()))) {
            log.info("SePay webhook id={} da xu ly truoc do, bo qua", request.getId());
            return;
        }

        String referenceCode = extractReferenceCode(request);
        if (referenceCode == null) {
            log.warn("SePay webhook khong tim thay ma don trong noi dung: {}", request.getContent());
            return;
        }

        Optional<PaymentTransaction> found = paymentTransactionRepository.findByReferenceCode(referenceCode);
        if (found.isEmpty()) {
            log.warn("SePay webhook: khong co don nap ung voi ma {}", referenceCode);
            return;
        }
        PaymentTransaction tx = found.get();
        if (tx.getStatus() != PaymentTransactionStatus.PENDING
                || tx.getType() != PaymentTransactionType.DEPOSIT) {
            log.info("SePay webhook: don {} khong o trang thai cho nap (status={})", referenceCode, tx.getStatus());
            return;
        }

        BigDecimal received = request.getTransferAmount();
        if (received != null && received.compareTo(tx.getAmount()) < 0) {
            log.warn("SePay webhook: so tien chuyen {} nho hon so tien don {} ({})",
                    received, tx.getAmount(), referenceCode);
            return;
        }

        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setProcessedAt(LocalDateTime.now());
        if (request.getId() != null) {
            tx.setExternalTransactionId(String.valueOf(request.getId()));
        }
        paymentTransactionRepository.save(tx);

        Wallet wallet = tx.getWallet();
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(tx.getAmount()));
        walletRepository.save(wallet);
        log.info("SePay webhook: nap thanh cong {} vao vi {} (ma {})",
                tx.getAmount(), wallet.getWalletId(), referenceCode);
    }

    // ===================== Tai khoan ngan hang =====================

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods() {
        Wallet wallet = getOrCreateWallet();
        return paymentMethodRepository
                .findByWallet_WalletIdAndStatus(wallet.getWalletId(), PM_ACTIVE).stream()
                .map(this::toPaymentMethodResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentMethodResponse addPaymentMethod(AddPaymentMethodRequest request) {
        if (!StringUtils.hasText(request.getBankName())
                || !StringUtils.hasText(request.getAccountNo())
                || !StringUtils.hasText(request.getAccountName())) {
            throw new IllegalArgumentException("Vui long nhap day du ngan hang, so tai khoan va ten chu tai khoan");
        }
        Wallet wallet = getOrCreateWallet();
        PaymentMethod pm = new PaymentMethod();
        pm.setWallet(wallet);
        pm.setType("BANK");
        pm.setBankName(request.getBankName().trim());
        pm.setAccountNo(request.getAccountNo().trim());
        pm.setAccountName(request.getAccountName().trim());
        pm.setStatus(PM_ACTIVE);
        return toPaymentMethodResponse(paymentMethodRepository.save(pm));
    }

    @Override
    @Transactional
    public void deletePaymentMethod(Long paymentMethodId) {
        Wallet wallet = getOrCreateWallet();
        PaymentMethod pm = paymentMethodRepository
                .findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan ngan hang"));
        if (!pm.getWallet().getWalletId().equals(wallet.getWalletId())) {
            throw new ForbiddenException("Khong the xoa tai khoan ngan hang cua nguoi khac");
        }
        // Soft-delete de khong pha vo rang buoc khoa ngoai tu withdrawal_requests.
        pm.setStatus(PM_INACTIVE);
        paymentMethodRepository.save(pm);
    }

    // ===================== Rut tien =====================

    @Override
    @Transactional
    public WithdrawalResponse createWithdrawal(CreateWithdrawalRequest request) {
        UserPrincipal principal = authHelper.requireAuthenticated();
        UserRole role = principal.getRole();

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("So tien rut phai lon hon 0");
        }
        Wallet wallet = getOrCreateWallet();
        PaymentMethod pm = paymentMethodRepository
                .findById(request.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan ngan hang"));
        if (!pm.getWallet().getWalletId().equals(wallet.getWalletId()) || !PM_ACTIVE.equals(pm.getStatus())) {
            throw new ForbiddenException("Tai khoan ngan hang khong hop le");
        }
        if (wallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("So du kha dung khong du de rut");
        }

        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setWallet(wallet);
        wr.setPaymentMethod(pm);
        wr.setAmount(request.getAmount());
        wr.setRequestedAt(LocalDateTime.now());

        boolean direct = role == UserRole.TUTOR || role == UserRole.TUTOR_CENTER;
        if (direct) {
            // Tutor / Tutor Center: rut truc tiep, hoan tat ngay.
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(request.getAmount()));
            wr.setStatus(WithdrawalRequestStatus.COMPLETED);
            wr.setProcessedAt(LocalDateTime.now());
            walletRepository.save(wallet);
            withdrawalRequestRepository.save(wr);
            recordWithdrawalTransaction(wallet, request.getAmount(), pm);
        } else {
            // Client: gui don, giu tien (available->frozen), cho admin duyet.
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(request.getAmount()));
            wallet.setFrozenBalance(wallet.getFrozenBalance().add(request.getAmount()));
            wr.setStatus(WithdrawalRequestStatus.PENDING);
            walletRepository.save(wallet);
            withdrawalRequestRepository.save(wr);
        }
        return toWithdrawalResponse(wr, direct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalResponse> getMyWithdrawals() {
        Wallet wallet = getOrCreateWallet();
        return withdrawalRequestRepository
                .findByWallet_WalletIdOrderByRequestedAtDesc(wallet.getWalletId()).stream()
                .map(wr -> toWithdrawalResponse(wr, false))
                .toList();
    }

    // ===================== Admin duyet rut =====================

    @Override
    @Transactional(readOnly = true)
    public List<AdminWithdrawalResponse> listAllWithdrawals(WithdrawalRequestStatus status) {
        List<WithdrawalRequest> list = status == null
                ? withdrawalRequestRepository.findAllByOrderByRequestedAtDesc()
                : withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(status);
        return list.stream().map(this::toAdminWithdrawalResponse).toList();
    }

    @Override
    @Transactional
    public AdminWithdrawalResponse reviewWithdrawal(Long withdrawalId, ReviewWithdrawalRequest request) {
        WithdrawalRequest wr = withdrawalRequestRepository
                .findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay yeu cau rut"));
        if (wr.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Yeu cau rut da duoc xu ly");
        }
        Wallet wallet = wr.getWallet();

        if (request.isApprove()) {
            // Duyet: giai ngan tien dang giu.
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(wr.getAmount()));
            wr.setStatus(WithdrawalRequestStatus.COMPLETED);
            wr.setProcessedAt(LocalDateTime.now());
            walletRepository.save(wallet);
            withdrawalRequestRepository.save(wr);
            recordWithdrawalTransaction(wallet, wr.getAmount(), wr.getPaymentMethod());
        } else {
            // Tu choi: hoan tien ve so du kha dung.
            if (!StringUtils.hasText(request.getReason())) {
                throw new IllegalArgumentException("Vui long nhap ly do tu choi");
            }
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(wr.getAmount()));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(wr.getAmount()));
            wr.setStatus(WithdrawalRequestStatus.REJECTED);
            wr.setFailureReason(request.getReason().trim());
            wr.setProcessedAt(LocalDateTime.now());
            walletRepository.save(wallet);
            withdrawalRequestRepository.save(wr);
        }
        return toAdminWithdrawalResponse(wr);
    }

    // ===================== Helpers =====================

    private void recordWithdrawalTransaction(Wallet wallet, BigDecimal amount, PaymentMethod pm) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setPaymentMethod(pm);
        tx.setType(PaymentTransactionType.WITHDRAWAL);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(amount);
        tx.setDescription("Rut tien ve " + pm.getBankName() + " - " + pm.getAccountNo());
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);
    }

    private Wallet getOrCreateWallet() {
        Long userId = authHelper.currentUserId();
        return walletRepository.findByUser_UserId(userId).orElseGet(() -> {
            User user = userRepository
                    .findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan"));
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setAvailableBalance(BigDecimal.ZERO);
            wallet.setFrozenBalance(BigDecimal.ZERO);
            wallet.setStatus(WalletStatus.ACTIVE);
            return walletRepository.save(wallet);
        });
    }

    private String generateReferenceCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(REF_PREFIX);
            long seed = System.nanoTime() ^ (long) (Math.random() * Long.MAX_VALUE);
            for (int i = 0; i < 6; i++) {
                sb.append(REF_ALPHABET[(int) (Math.floorMod(seed, REF_ALPHABET.length))]);
                seed /= REF_ALPHABET.length;
            }
            String code = sb.toString();
            if (paymentTransactionRepository.findByReferenceCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Khong sinh duoc ma don nap, thu lai");
    }

    private String extractReferenceCode(SepayWebhookRequest request) {
        for (String raw : new String[] {request.getCode(), request.getContent(), request.getDescription()}) {
            if (raw == null) {
                continue;
            }
            String normalized = raw.toUpperCase().replaceAll("[^0-9A-Z]", "");
            Matcher matcher = REF_PATTERN.matcher(normalized);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private String buildVietQrUrl(BigDecimal amount, String content) {
        long vnd = amount.setScale(0, java.math.RoundingMode.DOWN).longValueExact();
        return "https://img.vietqr.io/image/" + bankCode + "-" + accountNo + "-" + qrTemplate + ".png"
                + "?amount=" + vnd
                + "&addInfo=" + enc(content)
                + "&accountName=" + enc(accountName);
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
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
                .createdAt(tx.getCreatedAt())
                .processedAt(tx.getProcessedAt())
                .build();
    }

    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod pm) {
        return PaymentMethodResponse.builder()
                .paymentMethodId(pm.getPaymentMethodId())
                .type(pm.getType())
                .bankName(pm.getBankName())
                .accountNo(pm.getAccountNo())
                .accountName(pm.getAccountName())
                .build();
    }

    private WithdrawalResponse toWithdrawalResponse(WithdrawalRequest wr, boolean direct) {
        PaymentMethod pm = wr.getPaymentMethod();
        return WithdrawalResponse.builder()
                .withdrawalId(wr.getWithdrawalId())
                .amount(wr.getAmount())
                .status(wr.getStatus())
                .bankName(pm != null ? pm.getBankName() : null)
                .accountNo(pm != null ? pm.getAccountNo() : null)
                .accountName(pm != null ? pm.getAccountName() : null)
                .requestedAt(wr.getRequestedAt())
                .processedAt(wr.getProcessedAt())
                .failureReason(wr.getFailureReason())
                .direct(direct)
                .build();
    }

    private AdminWithdrawalResponse toAdminWithdrawalResponse(WithdrawalRequest wr) {
        PaymentMethod pm = wr.getPaymentMethod();
        User user = wr.getWallet().getUser();
        return AdminWithdrawalResponse.builder()
                .withdrawalId(wr.getWithdrawalId())
                .userId(user != null ? user.getUserId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .amount(wr.getAmount())
                .status(wr.getStatus())
                .bankName(pm != null ? pm.getBankName() : null)
                .accountNo(pm != null ? pm.getAccountNo() : null)
                .accountName(pm != null ? pm.getAccountName() : null)
                .requestedAt(wr.getRequestedAt())
                .processedAt(wr.getProcessedAt())
                .failureReason(wr.getFailureReason())
                .build();
    }
}
