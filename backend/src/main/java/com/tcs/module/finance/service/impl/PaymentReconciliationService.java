package com.tcs.module.finance.service.impl;

import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    private static final int TOPUP_TTL_MINUTES = 15;
    private static final int WITHDRAWAL_STALE_HOURS = 48;
    private static final int WITHDRAWAL_MATCH_WINDOW_MINUTES = 5;
    private static final String TOPUP_EXPIRED_REASON = "Phiên nạp tiền đã hết hạn, chưa ghi nhận thanh toán.";
    private static final String WITHDRAWAL_STALE_REASON =
            "Yêu cầu rút tiền quá thời gian xử lý, hệ thống đã hoàn lại số dư khả dụng.";

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletService walletService;
    private final PaymentNotificationService paymentNotificationService;

    @Scheduled(
            fixedDelayString = "${finance.reconciliation.fixed-delay-ms:300000}",
            initialDelayString = "${finance.reconciliation.initial-delay-ms:60000}")
    @Transactional
    public void reconcilePendingPayments() {
        LocalDateTime now = LocalDateTime.now();
        int expiredTopups = expirePendingTopups(now);
        int refundedWithdrawals = refundStaleWithdrawals(now);

        if (expiredTopups > 0 || refundedWithdrawals > 0) {
            log.info(
                    "Đối soát thanh toán: đã hủy {} phiên nạp hết hạn, hoàn {} yêu cầu rút quá hạn",
                    expiredTopups,
                    refundedWithdrawals);
        }
    }

    int expirePendingTopups(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(TOPUP_TTL_MINUTES);
        List<PaymentTransaction> expiredTopups = paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.DEPOSIT,
                PaymentTransactionStatus.PENDING,
                cutoff);

        expiredTopups.forEach(tx -> {
            tx.setStatus(PaymentTransactionStatus.CANCELLED);
            tx.setProcessedAt(now);
            tx.setFailureReason(TOPUP_EXPIRED_REASON);
            notifyWalletOwner(
                    tx.getWallet(),
                    "Phiên nạp tiền đã hết hạn",
                    TOPUP_EXPIRED_REASON,
                    "PAYMENT_TRANSACTION",
                    tx.getTransactionId());
        });
        paymentTransactionRepository.saveAll(expiredTopups);
        return expiredTopups.size();
    }

    int refundStaleWithdrawals(LocalDateTime now) {
        LocalDateTime cutoff = now.minusHours(WITHDRAWAL_STALE_HOURS);
        List<WithdrawalRequest> staleWithdrawals = List.of(
                        WithdrawalRequestStatus.PENDING,
                        WithdrawalRequestStatus.APPROVED)
                .stream()
                .flatMap(status -> withdrawalRequestRepository
                        .findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(status, cutoff)
                        .stream())
                .toList();

        int refunded = 0;
        for (WithdrawalRequest withdrawal : staleWithdrawals) {
            PaymentTransaction tx = findSafeWithdrawalTransaction(withdrawal);
            if (tx == null) {
                log.warn(
                        "Bỏ qua hoàn yêu cầu rút {} vì không khớp duy nhất giao dịch rút tiền",
                        withdrawal.getWithdrawalId());
                continue;
            }

            walletService.refundLockedFunds(
                    withdrawal.getWallet().getWalletId(),
                    withdrawal.getAmount(),
                    tx.getReferenceCode());

            tx.setStatus(PaymentTransactionStatus.CANCELLED);
            tx.setProcessedAt(now);
            tx.setFailureReason(WITHDRAWAL_STALE_REASON);
            paymentTransactionRepository.save(tx);

            withdrawal.setStatus(WithdrawalRequestStatus.REJECTED);
            withdrawal.setProcessedAt(now);
            withdrawal.setFailureReason(WITHDRAWAL_STALE_REASON);
            withdrawalRequestRepository.save(withdrawal);
            notifyWalletOwner(
                    withdrawal.getWallet(),
                    "Yêu cầu rút tiền quá hạn",
                    WITHDRAWAL_STALE_REASON,
                    "WITHDRAWAL_REQUEST",
                    withdrawal.getWithdrawalId());
            refunded++;
        }
        return refunded;
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

    private void notifyWalletOwner(
            Wallet wallet,
            String title,
            String content,
            String referenceType,
            Long referenceId) {

        if (wallet == null || wallet.getWalletId() == null) {
            return;
        }
        paymentNotificationService.notifyPayment(wallet.getWalletId(), title, content, referenceType, referenceId);
    }
}
