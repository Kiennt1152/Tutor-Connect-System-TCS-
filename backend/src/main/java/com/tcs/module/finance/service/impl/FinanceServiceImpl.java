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
import com.tcs.module.profile.dto.response.GuardianApprovalResponse;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.module.profile.service.ClientLegalAccountService.LegalAccountContext;
import com.tcs.module.profile.service.GuardianApprovalService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ClientLegalAccountService clientLegalAccountService;
    private final GuardianApprovalService guardianApprovalService;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        LegalAccountContext legalContext = clientLegalAccountService.requireLegalAccountForCurrentClient();
        return toWalletResponse(requireWallet(legalContext.getLegalUserId()), legalContext, null, null);
    }

    @Override
    @Transactional
    public WalletResponse deposit(DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        LegalAccountContext legalContext = clientLegalAccountService.requireLegalAccountForCurrentClient();
        Wallet wallet = requireWallet(legalContext.getLegalUserId());

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setAmount(request.getAmount());
        tx.setDescription(buildDepositDescription(request.getDescription(), legalContext));
        tx.setReferenceCode(UUID.randomUUID().toString());

        if (legalContext.isDelegatedToParent()) {
            tx.setStatus(PaymentTransactionStatus.PENDING);
            PaymentTransaction savedTx = paymentTransactionRepository.save(tx);
            GuardianApprovalResponse approval = guardianApprovalService.submitDepositApproval(legalContext, savedTx);
            return toWalletResponse(
                    wallet,
                    legalContext,
                    approval,
                    "Yêu cầu nạp tiền đã gửi. Phụ huynh "
                            + legalContext.getLegalHolderName()
                            + " sẽ nhận thông báo qua hệ thống và email để xác nhận.");
        }

        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setProcessedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);
        return toWalletResponse(savedWallet, legalContext, null, "Nạp tiền thành công.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods() {
        LegalAccountContext legalContext = clientLegalAccountService.requireLegalAccountForCurrentClient();
        Wallet wallet = requireWallet(legalContext.getLegalUserId());
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

    private String buildDepositDescription(String requestedDescription, LegalAccountContext legalContext) {
        if (!legalContext.isDelegatedToParent()) {
            return StringUtils.hasText(requestedDescription) ? requestedDescription : "Nạp tiền ví";
        }
        String base = "Nạp tiền ví phụ huynh";
        if (StringUtils.hasText(legalContext.getBeneficiaryMinorName())) {
            base += " (hộ " + legalContext.getBeneficiaryMinorName() + ")";
        }
        return StringUtils.hasText(requestedDescription) ? requestedDescription : base;
    }

    private Wallet requireWallet(Long legalUserId) {
        return walletRepository
                .findByUser_UserId(legalUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví phụ huynh liên kết"));
    }

    private WalletResponse toWalletResponse(
            Wallet wallet,
            LegalAccountContext legalContext,
            GuardianApprovalResponse approval,
            String message) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .availableBalance(wallet.getAvailableBalance())
                .frozenBalance(wallet.getFrozenBalance())
                .status(wallet.getStatus())
                .updatedAt(wallet.getUpdatedAt())
                .legalOwnerUserId(legalContext.getLegalUserId())
                .legalOwnerName(legalContext.getLegalHolderName())
                .delegatedToParent(legalContext.isDelegatedToParent())
                .beneficiaryMinorUserId(legalContext.getBeneficiaryMinorUserId())
                .beneficiaryMinorName(legalContext.getBeneficiaryMinorName())
                .pendingGuardianApproval(approval != null)
                .guardianApprovalId(approval != null ? approval.getApprovalId() : null)
                .guardianApprovalStatus(approval != null ? approval.getStatus() : null)
                .message(message)
                .build();
    }
}
