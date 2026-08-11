package com.tcs.module.finance.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.common.event.EscrowFunded;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.PaymentMethodRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.TopupStatusResponse;
import com.tcs.module.finance.dto.response.WalletResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentMethod;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentMethodRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @Mock
    private PlatformAdminRepository platformAdminRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        ReflectionTestUtils.setField(financeService, "directDepositEnabled", true);
        ReflectionTestUtils.setField(financeService, "simulateTopupEnabled", true);
    }

    @Test
    @DisplayName("getMyWallet returns an existing wallet")
    void getMyWalletReturnsCurrentWallet() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

        WalletResponse response = financeService.getMyWallet();

        assertEquals(USER_ID, response.getWalletId());
        assertEquals(new BigDecimal("250000.00"), response.getBalance());
        assertEquals(new BigDecimal("250000.00"), response.getAvailableBalance());
        assertEquals(new BigDecimal("50000.00"), response.getFrozenBalance());
        assertEquals(WalletStatus.ACTIVE, response.getStatus());
        verify(walletService).getRequired(USER_ID);
    }

    @Test
    @DisplayName("createMyWallet creates the current payout wallet")
    void createMyWalletCreatesCurrentWallet() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.create(USER_ID)).thenReturn(wallet);

        WalletResponse response = financeService.createMyWallet();

        assertEquals(USER_ID, response.getWalletId());
        assertEquals(new BigDecimal("250000.00"), response.getAvailableBalance());
        verify(authHelper).requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER);
        verify(walletService).create(USER_ID);
    }

    @Test
    @DisplayName("createMyWallet rejects client wallets")
    void createMyWalletRejectsClientWallets() {
        when(authHelper.requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER))
                .thenThrow(new ForbiddenException("Không có quyền truy cập"));

        assertThrows(ForbiddenException.class, () -> financeService.createMyWallet());
        verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("createTopup creates a pending center wallet deposit QR session")
    void createTopupCreatesPendingCenterSession() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
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
    @DisplayName("createTopup rejects non-center wallet deposits")
    void createTopupRejectsNonCenterWalletDeposits() {
        when(authHelper.requireRole(UserRole.TUTOR_CENTER))
                .thenThrow(new ForbiddenException("Không có quyền truy cập"));

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100000"));

        assertThrows(ForbiddenException.class, () -> financeService.createTopup(request));
        verify(walletService, never()).getRequired(any());
        verify(paymentTransactionRepository, never()).save(any());
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
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("100000")))
                .thenReturn(List.of());
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
    @DisplayName("handleSepayWebhook marks direct escrow payment funded")
    void handleSepayWebhookMarksEscrowPaymentFunded() {
        BigDecimal amount = new BigDecimal("500000");
        PaymentTransaction tx = pendingEscrowPayment("ESCROW-A7", amount);
        EscrowTransaction escrow = privateEscrow(5L, tx, amount);
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(456L);
        request.setTransferType("in");
        request.setTransferAmount(amount);
        request.setContent("Thanh toan hoc phi ESCROW-A7");
        request.setAccountNumber("02660559201");

        when(paymentTransactionRepository.findByExternalTransactionId("456")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                amount))
                .thenReturn(List.of(tx));
        when(escrowTransactionRepository.findByPayment_TransactionId(88L)).thenReturn(Optional.of(escrow));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("ESCROW-A7", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("456", tx.getExternalTransactionId());
        assertEquals(EscrowStatus.FUNDED, escrow.getStatus());
        verify(paymentTransactionRepository).save(tx);
        verify(escrowTransactionRepository).save(escrow);
        verify(eventPublisher).publishEvent(any(EscrowFunded.class));
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
    @DisplayName("handleSepayOutgoingWebhook completes a pending withdrawal from SePay money-out")
    void handleSepayOutgoingWebhookCompletesPendingWithdrawal() {
        BigDecimal amount = new BigDecimal("100000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 13, 9, 0);
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, amount, requestedAt);
        PaymentTransaction tx = pendingWithdrawalTransaction(method, amount, "WITHDRAW-ABC", requestedAt);

        Wallet releasedWallet = new Wallet();
        releasedWallet.setWalletId(USER_ID);
        releasedWallet.setAvailableBalance(new BigDecimal("150000.00"));
        releasedWallet.setFrozenBalance(BigDecimal.ZERO);
        releasedWallet.setStatus(WalletStatus.ACTIVE);

        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(987L);
        request.setTransferType("out");
        request.setTransferAmount(amount);
        request.setContent("Chuyen tien rut vi WITHDRAW-ABC");
        request.setAccountNumber("02660559201");

        when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-987"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.WITHDRAWAL,
                PaymentTransactionStatus.PENDING,
                amount))
                .thenReturn(List.of(tx));
        when(withdrawalRequestRepository
                .findByWallet_WalletIdAndStatusAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
                        USER_ID,
                        WithdrawalRequestStatus.PENDING,
                        amount,
                        requestedAt.minusMinutes(5),
                        requestedAt.plusMinutes(5)))
                .thenReturn(List.of(withdrawal));
        when(walletService.releaseLockedFunds(USER_ID, amount, "WITHDRAW-ABC")).thenReturn(releasedWallet);
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("WITHDRAW-ABC", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("SEPAY-OUT-987", tx.getExternalTransactionId());
        assertEquals("Yêu cầu rút tiền đã được xác nhận qua SePay", tx.getDescription());
        assertEquals(WithdrawalRequestStatus.COMPLETED, withdrawal.getStatus());
        verify(walletService).releaseLockedFunds(USER_ID, amount, "WITHDRAW-ABC");
        verify(paymentTransactionRepository).save(tx);
        verify(withdrawalRequestRepository).save(withdrawal);
    }

    @Test
    @DisplayName("handleSepayOutgoingWebhook can match a withdrawal request id in transfer content")
    void handleSepayOutgoingWebhookMatchesWithdrawalRequestId() {
        BigDecimal amount = new BigDecimal("100000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 13, 9, 0);
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, amount, requestedAt);
        PaymentTransaction tx = pendingWithdrawalTransaction(method, amount, "WITHDRAW-4F2A9B10", requestedAt);

        Wallet releasedWallet = new Wallet();
        releasedWallet.setWalletId(USER_ID);
        releasedWallet.setAvailableBalance(new BigDecimal("150000.00"));
        releasedWallet.setFrozenBalance(BigDecimal.ZERO);
        releasedWallet.setStatus(WalletStatus.ACTIVE);

        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(988L);
        request.setTransferType("out");
        request.setTransferAmount(amount);
        request.setContent("Rut tien WITHDRAW-15");
        request.setAccountNumber("02660559201");

        when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-988"))
                .thenReturn(Optional.empty());
        when(withdrawalRequestRepository.findById(15L)).thenReturn(Optional.of(withdrawal));
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        USER_ID,
                        PaymentTransactionType.WITHDRAWAL,
                        PaymentTransactionStatus.PENDING,
                        amount,
                        requestedAt.minusMinutes(5),
                        requestedAt.plusMinutes(5)))
                .thenReturn(List.of(tx));
        when(walletService.releaseLockedFunds(USER_ID, amount, "WITHDRAW-4F2A9B10")).thenReturn(releasedWallet);
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("WITHDRAW-4F2A9B10", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("SEPAY-OUT-988", tx.getExternalTransactionId());
        assertEquals(WithdrawalRequestStatus.COMPLETED, withdrawal.getStatus());
        verify(walletService).releaseLockedFunds(USER_ID, amount, "WITHDRAW-4F2A9B10");
    }

    @Test
    @DisplayName("handleSepayOutgoingWebhook ignores duplicate money-out webhooks")
    void handleSepayOutgoingWebhookIgnoresDuplicateExternalTransaction() {
        PaymentMethod method = savedPaymentMethod();
        PaymentTransaction tx = pendingWithdrawalTransaction(
                method,
                new BigDecimal("100000.00"),
                "WITHDRAW-ABC",
                LocalDateTime.of(2026, 7, 13, 9, 0));

        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(987L);
        request.setTransferType("out");
        request.setTransferAmount(new BigDecimal("100000.00"));
        request.setContent("WITHDRAW-ABC");

        when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-987"))
                .thenReturn(Optional.of(tx));

        PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("WITHDRAW-ABC", response.getReference());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleSepayOutgoingWebhook notifies requester and payer when refund transfer succeeds")
    void handleSepayOutgoingWebhookNotifiesRefundParties() {
        BigDecimal amount = new BigDecimal("100000.00");
        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setWallet(wallet);
        refundTx.setType(PaymentTransactionType.REFUND);
        refundTx.setStatus(PaymentTransactionStatus.PENDING);
        refundTx.setAmount(amount);
        refundTx.setReferenceCode("REFUND-ESCROW-10");

        EscrowTransaction escrow = privateEscrow(10L, pendingEscrowPayment("ESCROW-A7", amount), amount);
        User adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setEmail("admin@tcs.com");
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setRefundId(77L);
        refundRequest.setEscrowTransaction(escrow);
        refundRequest.setRequestedBy(adminUser);
        refundRequest.setAmount(amount);
        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setRefundReferenceCode("REFUND-ESCROW-10");

        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(990L);
        request.setTransferType("out");
        request.setTransferAmount(amount);
        request.setContent("Hoan tien REFUND-ESCROW-10");
        request.setAccountNumber("02660559201");

        when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-990"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.WITHDRAWAL,
                PaymentTransactionStatus.PENDING,
                amount))
                .thenReturn(List.of());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.REFUND,
                PaymentTransactionStatus.PENDING,
                amount))
                .thenReturn(List.of(refundTx));
        when(refundRequestRepository.findByRefundReferenceCode("REFUND-ESCROW-10"))
                .thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("REFUND-ESCROW-10", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, refundTx.getStatus());
        assertEquals(RefundRequestStatus.COMPLETED, refundRequest.getStatus());
        assertEquals("SUCCESS", refundRequest.getTransferStatus());
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Hoàn tiền đã chuyển khoản"),
                eq("Khoản hoàn 100000 đ đã được xác nhận qua SePay."),
                eq("REFUND_REQUEST"),
                eq(77L));
        verify(paymentNotificationService).notifyPayment(
                eq(USER_ID),
                eq("Hoàn tiền đã chuyển khoản"),
                eq("Khoản hoàn 100000 đ đã được xác nhận qua SePay."),
                eq("REFUND_REQUEST"),
                eq(77L));
    }

    @Test
    @DisplayName("getPaymentMethods returns active saved payout accounts")
    void getPaymentMethodsReturnsActiveMethods() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId(3L);
        method.setWallet(wallet);
        method.setType("BANK_TRANSFER");
        method.setBankName("TPBank");
        method.setAccountNo("1234567890");
        method.setStatus("ACTIVE");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByPaymentMethodIdAsc(USER_ID, "ACTIVE"))
                .thenReturn(List.of(method));

        List<PaymentMethodResponse> response = financeService.getPaymentMethods();

        assertEquals(1, response.size());
        assertEquals(3L, response.get(0).getPaymentMethodId());
        assertEquals("TPBank", response.get(0).getBankName());
        assertEquals("7890", response.get(0).getLastFour());
        assertEquals("****7890", response.get(0).getAccountNoMasked());
        assertTrue(response.get(0).getIsDefault());
    }

    @Test
    @DisplayName("createPaymentMethod validates and creates an active payout account")
    void createPaymentMethodCreatesActiveMethod() {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setBankName(" TPBank ");
        request.setAccountNo(" 1234 5678 90 ");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByPaymentMethodIdAsc(USER_ID, "ACTIVE"))
                .thenReturn(List.of());
        when(paymentMethodRepository.findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
                USER_ID, "TPBank", "1234567890", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> {
            PaymentMethod method = invocation.getArgument(0);
            method.setPaymentMethodId(3L);
            return method;
        });

        PaymentMethodResponse response = financeService.createPaymentMethod(request);

        assertEquals(3L, response.getPaymentMethodId());
        assertEquals("TPBank", response.getBankName());
        assertEquals("****7890", response.getAccountNoMasked());
        assertTrue(response.getIsDefault());

        ArgumentCaptor<PaymentMethod> methodCaptor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepository).save(methodCaptor.capture());
        assertEquals("BANK_TRANSFER", methodCaptor.getValue().getType());
        assertEquals("ACTIVE", methodCaptor.getValue().getStatus());
        assertEquals("1234567890", methodCaptor.getValue().getAccountNo());
    }

    @Test
    @DisplayName("updatePaymentMethod rejects duplicate active payout accounts")
    void updatePaymentMethodRejectsDuplicate() {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setBankName("TPBank");
        request.setAccountNo("1234567890");

        PaymentMethod current = new PaymentMethod();
        current.setPaymentMethodId(3L);
        current.setWallet(wallet);
        current.setType("BANK_TRANSFER");
        current.setBankName("VCB");
        current.setAccountNo("99998888");
        current.setStatus("ACTIVE");

        PaymentMethod duplicate = new PaymentMethod();
        duplicate.setPaymentMethodId(4L);
        duplicate.setWallet(wallet);
        duplicate.setType("BANK_TRANSFER");
        duplicate.setBankName("TPBank");
        duplicate.setAccountNo("1234567890");
        duplicate.setStatus("ACTIVE");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(current));
        when(paymentMethodRepository.findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
                USER_ID, "TPBank", "1234567890", "ACTIVE"))
                .thenReturn(Optional.of(duplicate));

        assertThrows(IllegalArgumentException.class, () -> financeService.updatePaymentMethod(3L, request));
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    @DisplayName("deletePaymentMethod marks the payout account inactive")
    void deletePaymentMethodMarksInactive() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId(3L);
        method.setWallet(wallet);
        method.setType("BANK_TRANSFER");
        method.setBankName("TPBank");
        method.setAccountNo("1234567890");
        method.setStatus("ACTIVE");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(method));

        financeService.deletePaymentMethod(3L);

        assertEquals("INACTIVE", method.getStatus());
        verify(paymentMethodRepository).save(method);
    }

    @Test
    @DisplayName("createWithdrawal locks wallet funds and creates a pending withdrawal request")
    void createWithdrawalCreatesPendingRequest() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(new BigDecimal("100000.00"));
        request.setBankName("TPBank");
        request.setAccountNo("1234567890");

        PaymentMethod savedMethod = new PaymentMethod();
        savedMethod.setPaymentMethodId(3L);
        savedMethod.setWallet(wallet);
        savedMethod.setType("BANK_TRANSFER");
        savedMethod.setBankName("TPBank");
        savedMethod.setAccountNo("1234567890");
        savedMethod.setStatus("ACTIVE");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any())).thenReturn(wallet);
        User adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setEmail("admin@tcs.com");
        PlatformAdmin admin = new PlatformAdmin();
        admin.setUser(adminUser);
        when(platformAdminRepository.findAll()).thenReturn(List.of(admin));
        when(paymentMethodRepository.findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
                USER_ID, "TPBank", "1234567890", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(savedMethod);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest withdrawal = invocation.getArgument(0);
            withdrawal.setWithdrawalId(15L);
            return withdrawal;
        });

        WithdrawalResponse response = financeService.createWithdrawal(request);

        assertEquals(15L, response.getWithdrawalId());
        assertEquals(new BigDecimal("100000.00"), response.getAmount());
        assertEquals(WithdrawalRequestStatus.PENDING, response.getStatus());
        assertEquals(3L, response.getPaymentMethodId());
        assertEquals("TPBank", response.getBankName());
        assertEquals("****7890", response.getAccountNoMasked());
        assertTrue(response.getReferenceCode().startsWith("WITHDRAW-"));
        assertEquals(USER_ID, response.getWallet().getWalletId());

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        PaymentTransaction tx = txCaptor.getValue();
        assertEquals(PaymentTransactionType.WITHDRAWAL, tx.getType());
        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
        assertEquals(savedMethod, tx.getPaymentMethod());
        verify(walletService).lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), eq(tx.getReferenceCode()));

        ArgumentCaptor<WithdrawalRequest> withdrawalCaptor = ArgumentCaptor.forClass(WithdrawalRequest.class);
        verify(withdrawalRequestRepository).save(withdrawalCaptor.capture());
        assertEquals(savedMethod, withdrawalCaptor.getValue().getPaymentMethod());
        assertEquals(WithdrawalRequestStatus.PENDING, withdrawalCaptor.getValue().getStatus());
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu rút tiền mới"),
                eq("Yêu cầu rút 100000 đ từ ví #7 đang chờ quản trị viên duyệt."),
                eq("WITHDRAWAL_REQUEST"),
                eq(15L));
    }

    @Test
    @DisplayName("createWithdrawal can use an active saved payment method")
    void createWithdrawalUsesSavedPaymentMethod() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(new BigDecimal("100000.00"));
        request.setPaymentMethodId(3L);

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setPaymentMethodId(3L);
        paymentMethod.setWallet(wallet);
        paymentMethod.setType("BANK_TRANSFER");
        paymentMethod.setBankName("TPBank");
        paymentMethod.setAccountNo("1234567890");
        paymentMethod.setStatus("ACTIVE");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any())).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        financeService.createWithdrawal(request);

        verify(paymentMethodRepository, never()).save(any());
        verify(walletService).lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any());
    }

    @Test
    @DisplayName("createWithdrawal rejects missing bank account data")
    void createWithdrawalRejectsMissingBankData() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(new BigDecimal("100000.00"));
        request.setBankName("TPBank");

        assertThrows(IllegalArgumentException.class, () -> financeService.createWithdrawal(request));
        verifyNoInteractions(withdrawalRequestRepository);
        verify(walletService, never()).lockFunds(any(), any(), any());
    }

    @Test
    @DisplayName("acceptWithdrawal approves a pending withdrawal and waits for SePay")
    void acceptWithdrawalApprovesPendingRequest() {
        BigDecimal amount = new BigDecimal("100000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 13, 9, 0);
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, amount, requestedAt);
        PaymentTransaction tx = pendingWithdrawalTransaction(method, amount, "WITHDRAW-ABC", requestedAt);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(null);
        when(withdrawalRequestRepository.findById(15L)).thenReturn(Optional.of(withdrawal));
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        USER_ID,
                        PaymentTransactionType.WITHDRAWAL,
                        PaymentTransactionStatus.PENDING,
                        amount,
                        requestedAt.minusMinutes(5),
                        requestedAt.plusMinutes(5)))
                .thenReturn(List.of(tx));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalResponse response = financeService.acceptWithdrawal(15L);

        assertEquals(WithdrawalRequestStatus.APPROVED, response.getStatus());
        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
        assertEquals("Yêu cầu rút tiền đã được duyệt, chờ đối soát SePay", tx.getDescription());
        assertEquals(WithdrawalRequestStatus.APPROVED, withdrawal.getStatus());
        assertEquals(USER_ID, response.getWallet().getWalletId());
        assertEquals(new BigDecimal("50000.00"), response.getWallet().getFrozenBalance());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository).save(tx);
        verify(withdrawalRequestRepository).save(withdrawal);
    }

    @Test
    @DisplayName("acceptWithdrawal rejects requests that are no longer pending")
    void acceptWithdrawalRejectsNonPendingRequest() {
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(
                15L,
                method,
                new BigDecimal("100000.00"),
                LocalDateTime.of(2026, 7, 13, 9, 0));
        withdrawal.setStatus(WithdrawalRequestStatus.COMPLETED);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(null);
        when(withdrawalRequestRepository.findById(15L)).thenReturn(Optional.of(withdrawal));

        assertThrows(IllegalArgumentException.class, () -> financeService.acceptWithdrawal(15L));
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMyTransactions applies type and date filters")
    void getMyTransactionsAppliesFilters() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

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
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
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
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

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

    private PaymentTransaction pendingEscrowPayment(String reference, BigDecimal amount) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(88L);
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setReferenceCode(reference);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    private EscrowTransaction privateEscrow(Long escrowId, PaymentTransaction payment, BigDecimal amount) {
        User payer = new User();
        payer.setUserId(USER_ID);
        payer.setEmail("client@tcs.com");
        User tutorUser = new User();
        tutorUser.setUserId(22L);
        tutorUser.setEmail("tutor@tcs.com");
        Tutor tutor = new Tutor();
        tutor.setUser(tutorUser);
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(3L);
        tutoringClass.setCreator(payer);
        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setTutor(tutor);
        assignment.setApplication(application);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setPayment(payment);
        escrow.setAssignment(assignment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.PENDING);
        return escrow;
    }

    private PaymentMethod savedPaymentMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId(3L);
        method.setWallet(wallet);
        method.setType("BANK_TRANSFER");
        method.setBankName("TPBank");
        method.setAccountNo("1234567890");
        method.setStatus("ACTIVE");
        return method;
    }

    private WithdrawalRequest pendingWithdrawal(
            Long withdrawalId,
            PaymentMethod method,
            BigDecimal amount,
            LocalDateTime requestedAt) {

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalId(withdrawalId);
        withdrawal.setWallet(wallet);
        withdrawal.setPaymentMethod(method);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalRequestStatus.PENDING);
        withdrawal.setRequestedAt(requestedAt);
        return withdrawal;
    }

    private PaymentTransaction pendingWithdrawalTransaction(
            PaymentMethod method,
            BigDecimal amount,
            String reference,
            LocalDateTime createdAt) {

        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setPaymentMethod(method);
        tx.setType(PaymentTransactionType.WITHDRAWAL);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setReferenceCode(reference);
        tx.setCreatedAt(createdAt);
        return tx;
    }
}
