package com.tcs.module.finance.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.service.FinanceService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final AuthHelper authHelper;
    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        return toWalletResponse(requireWallet());
    }

    @Override
    @Transactional
    public WalletResponse deposit(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        Wallet wallet = requireWallet();
        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription() != null ? request.getDescription() : "Nạp tiền ví");
        tx.setReferenceCode(UUID.randomUUID().toString());
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getAmount()));
        return toWalletResponse(walletRepository.save(wallet));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods() {
        Wallet wallet = requireWallet();
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

    private Wallet requireWallet() {
        return walletRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví"));
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
}
