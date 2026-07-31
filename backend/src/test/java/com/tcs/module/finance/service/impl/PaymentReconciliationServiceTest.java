package com.tcs.module.finance.service.impl;

import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private PaymentReconciliationService service;

    @Test
    void expirePendingTopupsCancelsExpiredTransactions() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 10, 0);
        PaymentTransaction tx = new PaymentTransaction();
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setReferenceCode("TOPUP-ABC");

        when(paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.DEPOSIT,
                PaymentTransactionStatus.PENDING,
                now.minusMinutes(15)))
                .thenReturn(List.of(tx));

        int changed = service.expirePendingTopups(now);

        assertEquals(1, changed);
        assertEquals(PaymentTransactionStatus.CANCELLED, tx.getStatus());
        assertEquals(now, tx.getProcessedAt());
        assertNotNull(tx.getFailureReason());
        verify(paymentTransactionRepository).saveAll(List.of(tx));
    }

    @Test
    void refundStaleWithdrawalsCancelsTransactionAndRefundsLockedFunds() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 10, 0);
        BigDecimal amount = new BigDecimal("100000.00");
        Wallet wallet = wallet(USER_ID);
        WithdrawalRequest withdrawal = withdrawal(wallet, amount, now.minusHours(49));
        PaymentTransaction tx = withdrawalTransaction(wallet, amount, "WITHDRAW-ABC", withdrawal.getRequestedAt());

        when(withdrawalRequestRepository.findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
                WithdrawalRequestStatus.PENDING,
                now.minusHours(48)))
                .thenReturn(List.of(withdrawal));
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        USER_ID,
                        PaymentTransactionType.WITHDRAWAL,
                        PaymentTransactionStatus.PENDING,
                        amount,
                        withdrawal.getRequestedAt().minusMinutes(5),
                        withdrawal.getRequestedAt().plusMinutes(5)))
                .thenReturn(List.of(tx));

        int changed = service.refundStaleWithdrawals(now);

        assertEquals(1, changed);
        assertEquals(PaymentTransactionStatus.CANCELLED, tx.getStatus());
        assertEquals(now, tx.getProcessedAt());
        assertNotNull(tx.getFailureReason());
        assertEquals(WithdrawalRequestStatus.REJECTED, withdrawal.getStatus());
        assertEquals(now, withdrawal.getProcessedAt());
        assertNotNull(withdrawal.getFailureReason());
        verify(walletService).refundLockedFunds(USER_ID, amount, "WITHDRAW-ABC");
        verify(paymentTransactionRepository).save(tx);
        verify(withdrawalRequestRepository).save(withdrawal);
    }

    @Test
    void refundStaleWithdrawalsSkipsAmbiguousTransactionMatches() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 10, 0);
        BigDecimal amount = new BigDecimal("100000.00");
        Wallet wallet = wallet(USER_ID);
        WithdrawalRequest withdrawal = withdrawal(wallet, amount, now.minusHours(49));
        PaymentTransaction first = withdrawalTransaction(wallet, amount, "WITHDRAW-1", withdrawal.getRequestedAt());
        PaymentTransaction second = withdrawalTransaction(wallet, amount, "WITHDRAW-2", withdrawal.getRequestedAt());

        when(withdrawalRequestRepository.findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
                WithdrawalRequestStatus.PENDING,
                now.minusHours(48)))
                .thenReturn(List.of(withdrawal));
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        eq(USER_ID),
                        eq(PaymentTransactionType.WITHDRAWAL),
                        eq(PaymentTransactionStatus.PENDING),
                        eq(amount),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));

        int changed = service.refundStaleWithdrawals(now);

        assertEquals(0, changed);
        assertEquals(WithdrawalRequestStatus.PENDING, withdrawal.getStatus());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
        verify(withdrawalRequestRepository, never()).save(any());
    }

    private Wallet wallet(Long walletId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        return wallet;
    }

    private WithdrawalRequest withdrawal(Wallet wallet, BigDecimal amount, LocalDateTime requestedAt) {
        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalId(11L);
        withdrawal.setWallet(wallet);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalRequestStatus.PENDING);
        withdrawal.setRequestedAt(requestedAt);
        return withdrawal;
    }

    private PaymentTransaction withdrawalTransaction(
            Wallet wallet,
            BigDecimal amount,
            String reference,
            LocalDateTime createdAt) {

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setReferenceCode(reference);
        tx.setType(PaymentTransactionType.WITHDRAWAL);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setCreatedAt(createdAt);
        return tx;
    }
}
