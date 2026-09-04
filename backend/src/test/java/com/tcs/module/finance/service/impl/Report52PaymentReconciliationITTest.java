package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52PaymentReconciliationITTest {

    private static final Long USER_ID = 7L;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @InjectMocks
    private PaymentReconciliationService reconciliationService;

    @Test
    @Tag("report52-it")
    void IT_WLT_018_CancelExpiredWalletTopupSession() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        PaymentTransaction topup = transaction(
                PaymentTransactionType.DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("100000.00"),
                "TOPUP-ABC",
                now.minusMinutes(20));

        when(paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.DEPOSIT,
                PaymentTransactionStatus.PENDING,
                now.minusMinutes(15)))
                .thenReturn(List.of(topup));

        int changed = reconciliationService.expirePendingTopups(now);

        assertEquals(1, changed);
        assertEquals(PaymentTransactionStatus.CANCELLED, topup.getStatus());
        assertEquals(now, topup.getProcessedAt());
        assertNotNull(topup.getFailureReason());
        verify(paymentTransactionRepository).saveAll(List.of(topup));
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_018_CancelExpiredClassEscrowPaymentSession() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        PaymentTransaction escrowPayment = transaction(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("500000.00"),
                "ESCROW-A7",
                now.minusMinutes(20));

        when(paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                now.minusMinutes(15)))
                .thenReturn(List.of(escrowPayment));

        int changed = reconciliationService.expirePendingEscrowDeposits(now);

        assertEquals(1, changed);
        assertEquals(PaymentTransactionStatus.CANCELLED, escrowPayment.getStatus());
        assertEquals(now, escrowPayment.getProcessedAt());
        assertNotNull(escrowPayment.getFailureReason());
        verify(paymentTransactionRepository).saveAll(List.of(escrowPayment));
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_018_KeepCenterRequestFeeOutsideClassEscrowTimeout() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        PaymentTransaction centerRequestFee = transaction(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("2500.00"),
                "CENTERREQ-ABC12345",
                now.minusMinutes(20));

        when(paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                now.minusMinutes(15)))
                .thenReturn(List.of(centerRequestFee));

        int changed = reconciliationService.expirePendingEscrowDeposits(now);

        assertEquals(0, changed);
        assertEquals(PaymentTransactionStatus.PENDING, centerRequestFee.getStatus());
        verify(paymentTransactionRepository).saveAll(List.of());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_017_RefundStalePendingWithdrawalAndCancelTransaction() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        BigDecimal amount = new BigDecimal("100000.00");
        Wallet wallet = wallet(USER_ID);
        WithdrawalRequest withdrawal = withdrawal(wallet, amount, now.minusHours(49), WithdrawalRequestStatus.PENDING);
        PaymentTransaction transaction = withdrawalTransaction(wallet, amount, "WITHDRAW-ABC", withdrawal.getRequestedAt());

        when(withdrawalRequestRepository.findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
                WithdrawalRequestStatus.PENDING,
                now.minusHours(48)))
                .thenReturn(List.of(withdrawal));
        when(withdrawalRequestRepository.findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
                WithdrawalRequestStatus.APPROVED,
                now.minusHours(48)))
                .thenReturn(List.of());
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        USER_ID,
                        PaymentTransactionType.WITHDRAWAL,
                        PaymentTransactionStatus.PENDING,
                        amount,
                        withdrawal.getRequestedAt().minusMinutes(5),
                        withdrawal.getRequestedAt().plusMinutes(5)))
                .thenReturn(List.of(transaction));

        int changed = reconciliationService.refundStaleWithdrawals(now);

        assertEquals(1, changed);
        assertEquals(PaymentTransactionStatus.CANCELLED, transaction.getStatus());
        assertEquals(WithdrawalRequestStatus.REJECTED, withdrawal.getStatus());
        verify(walletService).refundLockedFunds(USER_ID, amount, "WITHDRAW-ABC");
        verify(paymentTransactionRepository).save(transaction);
        verify(withdrawalRequestRepository).save(withdrawal);
    }

    @Test
    void SUPPORT_RECONCILIATION_DoNotRefundStaleWithdrawalWhenTransactionMatchIsAmbiguous() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        BigDecimal amount = new BigDecimal("100000.00");
        Wallet wallet = wallet(USER_ID);
        WithdrawalRequest withdrawal = withdrawal(wallet, amount, now.minusHours(49), WithdrawalRequestStatus.PENDING);
        PaymentTransaction first = withdrawalTransaction(wallet, amount, "WITHDRAW-1", withdrawal.getRequestedAt());
        PaymentTransaction second = withdrawalTransaction(wallet, amount, "WITHDRAW-2", withdrawal.getRequestedAt());

        when(withdrawalRequestRepository.findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
                WithdrawalRequestStatus.PENDING,
                now.minusHours(48)))
                .thenReturn(List.of(withdrawal));
        when(withdrawalRequestRepository.findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
                WithdrawalRequestStatus.APPROVED,
                now.minusHours(48)))
                .thenReturn(List.of());
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        eq(USER_ID),
                        eq(PaymentTransactionType.WITHDRAWAL),
                        eq(PaymentTransactionStatus.PENDING),
                        eq(amount),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));

        int changed = reconciliationService.refundStaleWithdrawals(now);

        assertEquals(0, changed);
        assertEquals(WithdrawalRequestStatus.PENDING, withdrawal.getStatus());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
        verify(withdrawalRequestRepository, never()).save(any());
    }

    private PaymentTransaction transaction(
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            BigDecimal amount,
            String referenceCode,
            LocalDateTime createdAt) {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setWallet(wallet(USER_ID));
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setCreatedAt(createdAt);
        return transaction;
    }

    private Wallet wallet(Long walletId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        return wallet;
    }

    private WithdrawalRequest withdrawal(
            Wallet wallet,
            BigDecimal amount,
            LocalDateTime requestedAt,
            WithdrawalRequestStatus status) {

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalId(11L);
        withdrawal.setWallet(wallet);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(status);
        withdrawal.setRequestedAt(requestedAt);
        return withdrawal;
    }

    private PaymentTransaction withdrawalTransaction(
            Wallet wallet,
            BigDecimal amount,
            String referenceCode,
            LocalDateTime createdAt) {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setType(PaymentTransactionType.WITHDRAWAL);
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        transaction.setCreatedAt(createdAt);
        return transaction;
    }
}
