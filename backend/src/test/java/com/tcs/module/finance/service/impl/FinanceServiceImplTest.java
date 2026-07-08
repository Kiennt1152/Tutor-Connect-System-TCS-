package com.tcs.module.finance.service.impl;

import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.WalletService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private WalletService walletService;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private FinanceServiceImpl financeService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setWalletId(USER_ID);
        wallet.setAvailableBalance(new BigDecimal("250000.00"));
        wallet.setFrozenBalance(new BigDecimal("50000.00"));
        wallet.setStatus(WalletStatus.ACTIVE);
    }

    @Test
    @DisplayName("getMyWallet returns an existing or newly created wallet")
    void getMyWalletReturnsCurrentWallet() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);

        WalletResponse response = financeService.getMyWallet();

        assertEquals(USER_ID, response.getWalletId());
        assertEquals(new BigDecimal("250000.00"), response.getBalance());
        assertEquals(new BigDecimal("250000.00"), response.getAvailableBalance());
        assertEquals(new BigDecimal("50000.00"), response.getFrozenBalance());
        assertEquals(WalletStatus.ACTIVE, response.getStatus());
        verify(walletService).getOrCreate(USER_ID);
    }

    @Test
    @DisplayName("getMyTransactions applies type and date filters")
    void getMyTransactionsAppliesFilters() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(99L);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(new BigDecimal("100000.00"));
        tx.setDescription("Nạp tiền ví");
        tx.setReferenceCode("TOPUP-1");
        tx.setCreatedAt(LocalDateTime.of(2026, 7, 8, 9, 30));

        when(paymentTransactionRepository.findByWalletIdWithFilters(
                eq(USER_ID),
                eq(PaymentTransactionType.DEPOSIT),
                eq(LocalDate.of(2026, 7, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 7, 31).atTime(23, 59, 59, 999999999)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        WalletTransactionsResponse response = financeService.getMyTransactions(
                0, 20, "deposit", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTransactions().size());
        assertEquals(PaymentTransactionType.DEPOSIT, response.getTransactions().get(0).getType());
    }

    @Test
    @DisplayName("getMyTransactions clamps invalid paging input")
    void getMyTransactionsClampsPaging() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);
        when(paymentTransactionRepository.findByWalletIdWithFilters(
                eq(USER_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        financeService.getMyTransactions(-1, 500, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentTransactionRepository).findByWalletIdWithFilters(
                eq(USER_ID), isNull(), isNull(), isNull(), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("getMyTransactions rejects unknown transaction types")
    void getMyTransactionsRejectsInvalidType() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);

        assertThrows(IllegalArgumentException.class,
                () -> financeService.getMyTransactions(0, 20, "UNKNOWN", null, null));
        verifyNoInteractions(paymentTransactionRepository);
    }
}
