package com.tcs.module.finance.service.impl;

import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.TransactionResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.FinanceService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
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

    private final AuthHelper authHelper;
    private final WalletService walletService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;

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
    public List<PaymentMethodResponse> getPaymentMethods() {
        Wallet wallet = currentWallet();
        return paymentMethodRepository.findByWallet_WalletId(wallet.getWalletId()).stream()
                .map(pm -> PaymentMethodResponse.builder()
                        .paymentMethodId(pm.getPaymentMethodId())
                        .type(pm.getType())
                        .provider(pm.getBankName())
                        .lastFour(pm.getAccountNo() != null && pm.getAccountNo().length() >= 4
                                ? pm.getAccountNo().substring(pm.getAccountNo().length() - 4)
                                : pm.getAccountNo())
                        .isDefault(false)
                        .build())
                .toList();
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

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
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
}
