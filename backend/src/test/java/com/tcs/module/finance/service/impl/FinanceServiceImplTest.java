package com.tcs.module.finance.service.impl;

import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.TopupStatusResponse;
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
import java.util.Optional;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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
    @DisplayName("createTopup creates a pending wallet deposit QR session")
    void createTopupCreatesPendingSession() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100000"));
        request.setDescription("Nạp tiền test");

        TopupSessionResponse response = financeService.createTopup(request);

        assertEquals(new BigDecimal("100000"), response.getAmount());
        assertEquals("PENDING", response.getStatus());
        assertTrue(response.getReference().startsWith("TOPUP-"));
        assertTrue(response.getQrUrl().contains("img.vietqr.io"));
        assertEquals("02660559201", response.getAccountNumber());
        assertEquals(response.getReference(), response.getTransferContent());

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        PaymentTransaction saved = txCaptor.getValue();
        assertEquals(wallet, saved.getWallet());
        assertEquals(PaymentTransactionType.DEPOSIT, saved.getType());
        assertEquals(PaymentTransactionStatus.PENDING, saved.getStatus());
        assertEquals(new BigDecimal("100000"), saved.getAmount());
    }

    @Test
    @DisplayName("simulateTopupSuccess marks pending topup paid and credits wallet")
    void simulateTopupSuccessCreditsWallet() {
        PaymentTransaction tx = pendingTopup("TOPUP-ABC", new BigDecimal("100000"));
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(paymentTransactionRepository.findByReferenceCode("TOPUP-ABC")).thenReturn(Optional.of(tx));
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);

        TopupStatusResponse response = financeService.simulateTopupSuccess("TOPUP-ABC");

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("SIMULATED-TOPUP-ABC", tx.getExternalTransactionId());
        verify(walletService).credit(USER_ID, new BigDecimal("100000"), "TOPUP-ABC");
        verify(paymentTransactionRepository).save(tx);
    }

    @Test
    @DisplayName("handleSepayWebhook matches amount and transfer content then credits once")
    void handleSepayWebhookMatchesPendingTopup() {
        PaymentTransaction tx = pendingTopup("TOPUP-ABC", new BigDecimal("100000"));
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(123L);
        request.setTransferType("in");
        request.setTransferAmount(new BigDecimal("100000"));
        request.setContent("Chuyen khoan TOPUP-ABC");
        request.setAccountNumber("02660559201");

        when(paymentTransactionRepository.findByExternalTransactionId("123")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("100000")))
                .thenReturn(List.of(tx));
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("TOPUP-ABC", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("123", tx.getExternalTransactionId());
        verify(walletService).credit(USER_ID, new BigDecimal("100000"), "TOPUP-ABC");
    }

    @Test
    @DisplayName("handleSepayWebhook ignores duplicate external transactions")
    void handleSepayWebhookIgnoresDuplicateExternalTransaction() {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(123L);
        request.setTransferType("in");
        request.setTransferAmount(new BigDecimal("100000"));
        request.setContent("TOPUP-ABC");

        when(paymentTransactionRepository.findByExternalTransactionId("123"))
                .thenReturn(Optional.of(pendingTopup("TOPUP-ABC", new BigDecimal("100000"))));

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        verify(walletService, never()).credit(any(), any(), any());
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

    private PaymentTransaction pendingTopup(String reference, BigDecimal amount) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setDescription("Nạp tiền ví qua VietQR");
        tx.setReferenceCode(reference);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }
}
