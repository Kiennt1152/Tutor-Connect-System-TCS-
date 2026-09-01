package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
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
import com.tcs.module.finance.dto.ReleaseInstruction;
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
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.platform.service.PenaltyAccessService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceImplTest {

    @Mock private PenaltyAccessService penaltyAccessService;

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
    private CenterRequestFeeService centerRequestFeeService;

    @Mock
    private EscrowService escrowService;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @Mock
    private com.tcs.module.identity.repository.UserRepository userRepository;
    @Mock
    private PlatformAdminRepository platformAdminRepository;

    @Mock
    private com.tcs.module.finance.repository.CenterRequestFeeHoldRepository centerRequestFeeHoldRepository;

    @Mock
    private com.tcs.module.finance.repository.DisputeRepository disputeRepository;

    @Mock
    private com.tcs.common.classrequest.ClassRequestStore classRequestStore;

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

    /** Ngoài phạm vi Report 5.1 (MethodList không có getMyWallet) - test bổ sung */
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

    /** Ngoài phạm vi Report 5.1 (MethodList không có createMyWallet) - test bổ sung */
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

    /** Ngoài phạm vi Report 5.1 (MethodList không có createMyWallet) - test bổ sung */
    @Test
    @DisplayName("createMyWallet rejects client wallets")
    void createMyWalletRejectsClientWallets() {
        when(authHelper.requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER))
                .thenThrow(new ForbiddenException("Không có quyền truy cập"));

        assertThrows(ForbiddenException.class, () -> financeService.createMyWallet());
        verifyNoInteractions(walletService);
    }

    /** Sheet financeCreateTopup - UTCID01 (N): ví trung tâm/gia sư, chưa có phiên nạp -> tạo phiên PENDING kèm mã QR */
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

    /** Sheet financeCreateTopup - UTCID03 (A): ví client không dùng cơ chế nạp này */
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

    /** Sheet financeCreateTopup - UTCID02 (N): phiên nạp đã tạo và thanh toán xác nhận -> ghi có đúng số tiền và ghi journal */
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

    /** Sheet financeSepayWebhook - UTCID02 (N): tiền vào khớp phiên nạp đang chờ -> ghi có vào ví */
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

    /** Sheet financeSepayWebhook - UTCID01 (N): tiền vào của giao dịch escrow -> đánh dấu đã thanh toán và sinh escrow FUNDED */
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
        when(centerRequestFeeService.isCenterRequestFeePayment(tx)).thenReturn(false);
        when(escrowService.fundConfirmedPayment(tx)).thenAnswer(invocation -> {
            escrow.setStatus(EscrowStatus.FUNDED);
            return escrow;
        });

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("ESCROW-A7", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("456", tx.getExternalTransactionId());
        assertEquals(EscrowStatus.FUNDED, escrow.getStatus());
        verify(paymentTransactionRepository).save(tx);
        verify(escrowService).fundConfirmedPayment(tx);
        verify(eventPublisher).publishEvent(any(EscrowFunded.class));
    }

    /** Sheet financeSepayWebhook - UTCID03 (B): mã giao dịch ngoài đã xử lý -> bỏ qua, không xử lý lần hai */
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

    /** Sheet financeSepayWebhook - UTCID04 (N): tiền ra khớp yêu cầu rút đang chờ -> hoàn tất yêu cầu rút */
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

    /** Sheet financeSepayWebhook - UTCID04 (N): tiền ra khớp theo withdrawalRequestId */
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

    /** Sheet financeSepayWebhook - UTCID05 (B): callback tiền ra bị lặp -> bỏ qua */
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

    /** Bổ sung ngoài các UTCID của sheet financeSepayWebhook: tiền ra của lệnh hoàn tiền -> thông báo các bên liên quan */
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

    /** Ngoài phạm vi Report 5.1 (MethodList không có getPaymentMethods) - test bổ sung */
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
        when(paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByLastUsedAtDescPaymentMethodIdAsc(USER_ID, "ACTIVE"))
                .thenReturn(List.of(method));

        List<PaymentMethodResponse> response = financeService.getPaymentMethods();

        assertEquals(1, response.size());
        assertEquals(3L, response.get(0).getPaymentMethodId());
        assertEquals("TPBank", response.get(0).getBankName());
        assertEquals("7890", response.get(0).getLastFour());
        assertEquals("****7890", response.get(0).getAccountNoMasked());
        assertTrue(response.get(0).getIsDefault());
    }

    /** Ngoài phạm vi Report 5.1 (MethodList không có createPaymentMethod) - test bổ sung */
    @Test
    @DisplayName("createPaymentMethod validates and creates an active payout account")
    void createPaymentMethodCreatesActiveMethod() {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setBankName(" TPBank ");
        request.setAccountNo(" 1234 5678 90 ");
        request.setAccountHolderName(" Nguyễn Văn A ");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByLastUsedAtDescPaymentMethodIdAsc(USER_ID, "ACTIVE"))
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
        assertEquals("Nguyễn Văn A", methodCaptor.getValue().getAccountHolderName());
    }

    /** Ngoài phạm vi Report 5.1 (MethodList không có updatePaymentMethod) - test bổ sung */
    @Test
    @DisplayName("updatePaymentMethod rejects duplicate active payout accounts")
    void updatePaymentMethodRejectsDuplicate() {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setBankName("TPBank");
        request.setAccountNo("1234567890");
        request.setAccountHolderName("Nguyễn Văn A");

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

    /** Ngoài phạm vi Report 5.1 (MethodList không có deletePaymentMethod) - test bổ sung */
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

    /** Sheet financeCreateWithdrawal - UTCID01 (N): ví hoạt động đủ số dư + paymentMethodId hợp lệ -> tạo yêu cầu rút và khóa tiền */
    @Test
    @DisplayName("createWithdrawal locks wallet funds and creates a pending withdrawal request")
    void createWithdrawalCreatesPendingRequest() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(new BigDecimal("100000.00"));
        request.setPaymentMethodId(3L);

        PaymentMethod savedMethod = new PaymentMethod();
        savedMethod.setPaymentMethodId(3L);
        savedMethod.setWallet(wallet);
        savedMethod.setType("BANK_TRANSFER");
        savedMethod.setBankName("TPBank");
        savedMethod.setAccountNo("1234567890");
        savedMethod.setAccountHolderName("Nguyễn Văn A");
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
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(
                3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(savedMethod));
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

    /** Sheet financeCreateWithdrawal - UTCID02 (N): dùng phương thức nhận tiền đã lưu kèm accountHolderName */
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
        paymentMethod.setAccountHolderName("Nguyễn Văn A");
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

    /** Sheet financeCreateWithdrawal - UTCID03 (A): paymentMethodId = null */
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

    /** Sheet financeApproveWithdrawal - UTCID01 (N): admin duyệt yêu cầu PENDING có giao dịch khớp -> chuyển sang đã duyệt, chờ đối soát SePay */
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

    /** Sheet financeApproveWithdrawal - UTCID04 (A): yêu cầu không ở trạng thái chờ duyệt */
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

    /** Sheet financeGetTransactions - UTCID01 (N): lọc theo loại giao dịch hợp lệ và phân trang trong khoảng cho phép */
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

    /** Sheet financeGetTransactions - UTCID02 (B): tham số phân trang ngoài khoảng -> kẹp về khoảng hợp lệ */
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

    /** Sheet financeGetTransactions - UTCID03 (A): loại giao dịch không tồn tại */
    @Test
    @DisplayName("getMyTransactions rejects unknown transaction types")
    void getMyTransactionsRejectsInvalidType() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

        assertThrows(IllegalArgumentException.class,
                () -> financeService.getMyTransactions(0, 20, "UNKNOWN", null, null));
        verifyNoInteractions(paymentTransactionRepository);
    }

    /** Sheet financeGetTransactions - ngay bat dau sau ngay ket thuc -> chan truoc khi truy van. */
    /** Sheet financeGetTransactions - UTCID04 (B): ngày bắt đầu sau ngày kết thúc */
    @Test
    @DisplayName("getMyTransactions rejects a from-date later than the to-date")
    void getMyTransactionsRejectsInvertedDateRange() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financeService.getMyTransactions(0, 20, null,
                        java.time.LocalDate.of(2026, 8, 31), java.time.LocalDate.of(2026, 8, 1)));
        assertEquals("Ngày bắt đầu không được sau ngày kết thúc", ex.getMessage());
        verifyNoInteractions(paymentTransactionRepository);
    }

    private PaymentTransaction pendingTopup(String reference, BigDecimal amount) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(amount);
        tx.setDescription("Nạp tiền ví qua mã QR chuyển khoản");
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
        method.setAccountHolderName("Nguyễn Văn A");
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

    // ===================================================================
    //  Sheet financeCreateWithdrawal - UTCID04..UTCID08 (validateWithdrawalRequest)
    // ===================================================================

    /** Sheet financeCreateWithdrawal - UTCID04 (A): request = null -> 'Thiếu thông tin rút tiền'. */
    @Test
    @DisplayName("createWithdrawal rejects a null request")
    void createWithdrawalRejectsNullRequest() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financeService.createWithdrawal(null));
        assertEquals("Thiếu thông tin rút tiền", ex.getMessage());
        verify(withdrawalRequestRepository, never()).save(any());
        verify(walletService, never()).lockFunds(any(), any(), any());
    }

    /** Sheet financeCreateWithdrawal - UTCID05 (B): amount = 0 (can duoi) -> 'Số tiền rút phải lớn hơn 0'. */
    @Test
    @DisplayName("createWithdrawal rejects a zero amount")
    void createWithdrawalRejectsZeroAmount() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(BigDecimal.ZERO);
        request.setPaymentMethodId(3L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financeService.createWithdrawal(request));
        assertEquals("Số tiền rút phải lớn hơn 0", ex.getMessage());
        verify(withdrawalRequestRepository, never()).save(any());
    }

    /** Sheet financeCreateWithdrawal - UTCID06 (A): amount = null -> 'Số tiền rút phải lớn hơn 0'. */
    @Test
    @DisplayName("createWithdrawal rejects a null amount")
    void createWithdrawalRejectsNullAmount() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(null);
        request.setPaymentMethodId(3L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financeService.createWithdrawal(request));
        assertEquals("Số tiền rút phải lớn hơn 0", ex.getMessage());
        verify(withdrawalRequestRepository, never()).save(any());
    }

    /** Sheet financeCreateWithdrawal - UTCID07 (B): accountHolderName 151 ky tu (vuot can tren) -> chan. */
    @Test
    @DisplayName("createWithdrawal rejects an account holder name longer than 150 characters")
    void createWithdrawalRejectsTooLongAccountHolderName() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(new BigDecimal("100000.00"));
        request.setPaymentMethodId(3L);
        request.setAccountHolderName("A".repeat(151));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financeService.createWithdrawal(request));
        assertEquals("Tên chủ tài khoản không được vượt quá 150 ký tự", ex.getMessage());
        verify(withdrawalRequestRepository, never()).save(any());
    }

    /** Sheet financeCreateWithdrawal - UTCID08 (B): accountHolderName dung 150 ky tu (dung can tren) -> chap nhan. */
    @Test
    @DisplayName("createWithdrawal accepts an account holder name of exactly 150 characters")
    void createWithdrawalAcceptsAccountHolderNameAtBoundary() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(new BigDecimal("100000.00"));
        request.setPaymentMethodId(3L);
        request.setAccountHolderName("A".repeat(150));

        PaymentMethod savedMethod = new PaymentMethod();
        savedMethod.setPaymentMethodId(3L);
        savedMethod.setWallet(wallet);
        savedMethod.setType("BANK_TRANSFER");
        savedMethod.setBankName("TPBank");
        savedMethod.setAccountNo("1234567890");
        savedMethod.setAccountHolderName("A".repeat(150));
        savedMethod.setStatus("ACTIVE");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any())).thenReturn(wallet);
        when(platformAdminRepository.findAll()).thenReturn(List.of());
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(savedMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest withdrawal = invocation.getArgument(0);
            withdrawal.setWithdrawalId(16L);
            return withdrawal;
        });

        WithdrawalResponse response = financeService.createWithdrawal(request);

        assertEquals(16L, response.getWithdrawalId());
        assertEquals(WithdrawalRequestStatus.PENDING, response.getStatus());
        verify(walletService).lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any());
    }

    // =====================================================================================
    //  Sheet: financeDeposit (nap tien truc tiep - chi bat o moi truong dev)
    // =====================================================================================
    @org.junit.jupiter.api.Nested
    @DisplayName("financeDeposit")
    class FinanceDeposit {

        private com.tcs.module.finance.dto.request.DepositRequest request(
                BigDecimal amount, String description) {
            var r = new com.tcs.module.finance.dto.request.DepositRequest();
            r.setAmount(amount);
            r.setDescription(description);
            return r;
        }

        private void givenCenterWallet() {
            when(authHelper.currentUserId()).thenReturn(USER_ID);
            when(walletService.getRequired(USER_ID)).thenReturn(wallet);
            when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                    .thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - Trung tam nap tien, co bat cong tac dev -> tao giao dich DEPOSIT SUCCESS va cong vi")
        void utcid01_depositSuccessfully() {
            givenCenterWallet();

            financeService.deposit(request(new BigDecimal("500000.00"), "Nap tien dot 1"));

            ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(paymentTransactionRepository).save(captor.capture());
            PaymentTransaction tx = captor.getValue();
            assertEquals(PaymentTransactionType.DEPOSIT, tx.getType());
            assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
            assertTrue(tx.getReferenceCode().startsWith("TOPUP-"));
            assertEquals("Nap tien dot 1", tx.getDescription());
            verify(walletService).credit(eq(USER_ID), eq(new BigDecimal("500000.00")), eq(tx.getReferenceCode()));
        }

        @Test
        @DisplayName("UTCID02 (A) - amount = null -> 'Số tiền nạp phải lớn hơn 0'")
        void utcid02_nullAmount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.deposit(request(null, "Nap tien")));
            assertEquals("Số tiền nạp phải lớn hơn 0", ex.getMessage());
            verify(paymentTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (B) - amount = 0 (dung can duoi) -> chan")
        void utcid03_zeroAmount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.deposit(request(BigDecimal.ZERO, "Nap tien")));
            assertEquals("Số tiền nạp phải lớn hơn 0", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - amount am -> chan")
        void utcid04_negativeAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> financeService.deposit(request(new BigDecimal("-1000"), "Nap tien")));
            verify(paymentTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (A) - Cong tac nap truc tiep dang tat -> 'Nạp tiền trực tiếp đã tắt. ...'")
        void utcid05_directDepositDisabled() {
            ReflectionTestUtils.setField(financeService, "directDepositEnabled", false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.deposit(request(new BigDecimal("500000.00"), "Nap tien")));
            assertEquals("Nạp tiền trực tiếp đã tắt. Vui lòng nạp tiền qua cổng thanh toán.", ex.getMessage());
            verify(paymentTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID06 (A) - Nguoi goi khong phai TUTOR_CENTER -> ForbiddenException")
        void utcid06_callerIsNotACenter() {
            when(authHelper.requireRole(UserRole.TUTOR_CENTER))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class,
                    () -> financeService.deposit(request(new BigDecimal("500000.00"), "Nap tien")));
            verify(paymentTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID07 (A) - Trung tam chua co vi -> 'Không tìm thấy ví cho người dùng này'")
        void utcid07_walletMissing() {
            when(authHelper.currentUserId()).thenReturn(USER_ID);
            when(walletService.getRequired(USER_ID))
                    .thenThrow(new BusinessException("Không tìm thấy ví cho người dùng này"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.deposit(request(new BigDecimal("500000.00"), "Nap tien")));
            assertEquals("Không tìm thấy ví cho người dùng này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - amount = 0.01 (gia tri duong nho nhat) -> van nap duoc")
        void utcid08_smallestPositiveAmount() {
            givenCenterWallet();

            financeService.deposit(request(new BigDecimal("0.01"), "Nap thu"));

            verify(walletService).credit(eq(USER_ID), eq(new BigDecimal("0.01")), any());
        }

        @Test
        @DisplayName("UTCID09 (N) - description = null -> dung noi dung mac dinh 'Nạp tiền ví trung tâm'")
        void utcid09_defaultDescription() {
            givenCenterWallet();

            financeService.deposit(request(new BigDecimal("500000.00"), null));

            ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(paymentTransactionRepository).save(captor.capture());
            assertEquals("Nạp tiền ví trung tâm", captor.getValue().getDescription());
        }
    }

    // =====================================================================================
    //  Sheet: financeRejectWithdrawal & financeWithdrawalFailed (dao nguoc yeu cau rut tien)
    // =====================================================================================
    @org.junit.jupiter.api.Nested
    @DisplayName("financeRejectWithdrawal")
    class FinanceRejectWithdrawal {

        @Test
        @DisplayName("UTCID01 (N) - Admin tu choi yeu cau dang cho -> hoan tien dong bang, giao dich CANCELLED")
        void utcid01_rejectSuccessfully() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            PaymentTransaction tx = pendingWithdrawalTransaction();
            givenReversibleWithdrawal(withdrawal, tx);

            financeService.rejectWithdrawal(20L, withdrawalDecision("Sai thong tin tai khoan"));

            assertEquals(PaymentTransactionStatus.CANCELLED, tx.getStatus());
            assertEquals("Sai thong tin tai khoan", tx.getFailureReason());
            assertEquals(WithdrawalRequestStatus.REJECTED, withdrawal.getStatus());
            verify(walletService).refundLockedFunds(USER_ID, withdrawal.getAmount(), tx.getReferenceCode());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong phai PLATFORM_ADMIN -> ForbiddenException")
        void utcid02_notAdmin() {
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class,
                    () -> financeService.rejectWithdrawal(20L, withdrawalDecision("ly do")));
            verify(withdrawalRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - withdrawalId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu rút tiền'")
        void utcid03_withdrawalNotFound() {
            when(withdrawalRequestRepository.findById(20L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> financeService.rejectWithdrawal(20L, withdrawalDecision("ly do")));
            assertEquals("Không tìm thấy yêu cầu rút tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Yeu cau khong con o trang thai cho -> 'Chỉ yêu cầu rút tiền đang chờ mới có thể hoàn lại'")
        void utcid04_notAwaitingTransfer() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            withdrawal.setStatus(WithdrawalRequestStatus.COMPLETED);
            when(withdrawalRequestRepository.findById(20L)).thenReturn(Optional.of(withdrawal));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.rejectWithdrawal(20L, withdrawalDecision("ly do")));
            assertEquals("Chỉ yêu cầu rút tiền đang chờ mới có thể hoàn lại", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Khong xac dinh duoc giao dich rut tien tuong ung -> chan")
        void utcid05_noMatchingTransaction() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            when(withdrawalRequestRepository.findById(20L)).thenReturn(Optional.of(withdrawal));
            when(paymentTransactionRepository
                    .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                            any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.rejectWithdrawal(20L, withdrawalDecision("ly do")));
            assertEquals("Không xác định được giao dịch rút tiền tương ứng", ex.getMessage());
            verify(walletService, never()).refundLockedFunds(any(), any(), any());
        }

        @Test
        @DisplayName("UTCID06 (N) - reason = null -> dung ly do mac dinh 'Yêu cầu rút tiền bị từ chối'")
        void utcid06_defaultReason() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            PaymentTransaction tx = pendingWithdrawalTransaction();
            givenReversibleWithdrawal(withdrawal, tx);

            financeService.rejectWithdrawal(20L, withdrawalDecision(null));

            assertEquals("Yêu cầu rút tiền bị từ chối", tx.getFailureReason());
            assertEquals(WithdrawalRequestStatus.REJECTED, withdrawal.getStatus());
        }
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("financeWithdrawalFailed")
    class FinanceWithdrawalFailed {

        @Test
        @DisplayName("UTCID01 (N) - Admin danh dau chuyen khoan that bai -> hoan tien, giao dich FAILED")
        void utcid01_markFailedSuccessfully() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            PaymentTransaction tx = pendingWithdrawalTransaction();
            givenReversibleWithdrawal(withdrawal, tx);

            financeService.markWithdrawalTransferFailed(20L, withdrawalDecision("Ngan hang tu choi lenh"));

            assertEquals(PaymentTransactionStatus.FAILED, tx.getStatus(),
                    "Chuyen khoan that bai phai la FAILED, khong phai CANCELLED");
            assertEquals("Ngan hang tu choi lenh", tx.getFailureReason());
            assertEquals(WithdrawalRequestStatus.REJECTED, withdrawal.getStatus());
            verify(walletService).refundLockedFunds(USER_ID, withdrawal.getAmount(), tx.getReferenceCode());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong phai PLATFORM_ADMIN -> ForbiddenException")
        void utcid02_notAdmin() {
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class,
                    () -> financeService.markWithdrawalTransferFailed(20L, withdrawalDecision("ly do")));
        }

        @Test
        @DisplayName("UTCID03 (A) - withdrawalId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu rút tiền'")
        void utcid03_withdrawalNotFound() {
            when(withdrawalRequestRepository.findById(20L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> financeService.markWithdrawalTransferFailed(20L, withdrawalDecision("ly do")));
            assertEquals("Không tìm thấy yêu cầu rút tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Yeu cau khong con o trang thai cho -> chan")
        void utcid04_notAwaitingTransfer() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            withdrawal.setStatus(WithdrawalRequestStatus.REJECTED);
            when(withdrawalRequestRepository.findById(20L)).thenReturn(Optional.of(withdrawal));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.markWithdrawalTransferFailed(20L, withdrawalDecision("ly do")));
            assertEquals("Chỉ yêu cầu rút tiền đang chờ mới có thể hoàn lại", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Khong xac dinh duoc giao dich rut tien tuong ung -> chan")
        void utcid05_noMatchingTransaction() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            when(withdrawalRequestRepository.findById(20L)).thenReturn(Optional.of(withdrawal));
            when(paymentTransactionRepository
                    .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                            any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.markWithdrawalTransferFailed(20L, withdrawalDecision("ly do")));
            assertEquals("Không xác định được giao dịch rút tiền tương ứng", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (N) - reason = null -> dung ly do mac dinh ve chuyen khoan that bai")
        void utcid06_defaultReason() {
            WithdrawalRequest withdrawal = awaitingWithdrawal();
            PaymentTransaction tx = pendingWithdrawalTransaction();
            givenReversibleWithdrawal(withdrawal, tx);

            financeService.markWithdrawalTransferFailed(20L, withdrawalDecision(null));

            assertEquals("Chuyển khoản ngân hàng thất bại, hệ thống đã hoàn lại số dư khả dụng",
                    tx.getFailureReason());
        }
    }

    /** Yeu cau rut tien dang cho chuyen khoan. */
    private WithdrawalRequest awaitingWithdrawal() {
        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalId(20L);
        withdrawal.setWallet(wallet);
        withdrawal.setAmount(new BigDecimal("100000.00"));
        withdrawal.setStatus(WithdrawalRequestStatus.PENDING);
        withdrawal.setRequestedAt(java.time.LocalDateTime.now().minusMinutes(5));
        return withdrawal;
    }

    /** Giao dich rut tien dang PENDING khop voi yeu cau tren. */
    private PaymentTransaction pendingWithdrawalTransaction() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(30L);
        tx.setWallet(wallet);
        tx.setType(PaymentTransactionType.WITHDRAWAL);
        tx.setStatus(PaymentTransactionStatus.PENDING);
        tx.setAmount(new BigDecimal("100000.00"));
        tx.setReferenceCode("WITHDRAW-20");
        return tx;
    }

    private com.tcs.module.finance.dto.request.WithdrawalDecisionRequest withdrawalDecision(String reason) {
        var r = new com.tcs.module.finance.dto.request.WithdrawalDecisionRequest();
        r.setReason(reason);
        return r;
    }

    private void givenReversibleWithdrawal(WithdrawalRequest withdrawal, PaymentTransaction tx) {
        when(withdrawalRequestRepository.findById(20L)).thenReturn(Optional.of(withdrawal));
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletService.refundLockedFunds(any(), any(), any())).thenReturn(wallet);
    }

    // =====================================================================================
    //  Sheet: financeSepayIncoming (webhook tien vao tu SePay)
    // =====================================================================================
    @org.junit.jupiter.api.Nested
    @DisplayName("financeSepayIncoming")
    class FinanceSepayIncoming {

        private static final BigDecimal AMOUNT = new BigDecimal("100000");

        private SepayWebhookRequest incoming(Long id, BigDecimal amount, String content) {
            SepayWebhookRequest r = new SepayWebhookRequest();
            r.setId(id);
            r.setTransferType("in");
            r.setTransferAmount(amount);
            r.setContent(content);
            r.setAccountNumber("02660559201");
            return r;
        }

        private void givenNoDuplicate(String externalId) {
            when(paymentTransactionRepository.findByExternalTransactionId(externalId))
                    .thenReturn(Optional.empty());
        }

        private void givenNoEscrowCandidate() {
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.ESCROW_DEPOSIT, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("UTCID01 (N) - Khop giao dich nap tien dang cho -> success, hoan tat nap tien")
        void utcid01_matchesPendingTopup() {
            PaymentTransaction tx = pendingTopup("TOPUP-ABC", AMOUNT);
            givenNoDuplicate("123");
            givenNoEscrowCandidate();
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.DEPOSIT, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of(tx));
            when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);

            PaymentWebhookResponse response =
                    financeService.handleSepayIncomingWebhook(incoming(123L, AMOUNT, "Chuyen khoan TOPUP-ABC"));

            assertEquals("success", response.getStatus());
            assertEquals("Đã ghi nhận giao dịch SePay thành công", response.getMessage());
            assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
            verify(walletService).credit(USER_ID, AMOUNT, "TOPUP-ABC");
        }

        @Test
        @DisplayName("UTCID02 (N) - Khop thanh toan ky quy (khong phai phi trung tam) -> 'Đã ghi nhận học phí SePay vào ký quỹ'")
        void utcid02_matchesEscrowPayment() {
            BigDecimal amount = new BigDecimal("500000");
            PaymentTransaction tx = pendingEscrowPayment("ESCROW-A7", amount);
            EscrowTransaction escrow = privateEscrow(5L, tx, amount);
            givenNoDuplicate("456");
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.ESCROW_DEPOSIT, PaymentTransactionStatus.PENDING, amount))
                    .thenReturn(List.of(tx));
            when(centerRequestFeeService.isCenterRequestFeePayment(tx)).thenReturn(false);
            when(escrowService.fundConfirmedPayment(tx)).thenReturn(escrow);

            PaymentWebhookResponse response = financeService.handleSepayIncomingWebhook(
                    incoming(456L, amount, "Thanh toan hoc phi ESCROW-A7"));

            assertEquals("success", response.getStatus());
            assertEquals("Đã ghi nhận học phí SePay vào ký quỹ", response.getMessage());
            verify(escrowService).fundConfirmedPayment(tx);
        }

        @Test
        @DisplayName("UTCID03 (N) - Khoan khop la phi xu ly yeu cau trung tam -> 'Đã ghi nhận phí xử lý yêu cầu trung tâm'")
        void utcid03_matchesCenterRequestFee() {
            BigDecimal amount = new BigDecimal("500000");
            PaymentTransaction tx = pendingEscrowPayment("FEE-A7", amount);
            givenNoDuplicate("457");
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.ESCROW_DEPOSIT, PaymentTransactionStatus.PENDING, amount))
                    .thenReturn(List.of(tx));
            when(centerRequestFeeService.isCenterRequestFeePayment(tx)).thenReturn(true);

            PaymentWebhookResponse response = financeService.handleSepayIncomingWebhook(
                    incoming(457L, amount, "Thanh toan phi FEE-A7"));

            assertEquals("success", response.getStatus());
            assertEquals("Đã ghi nhận phí xử lý yêu cầu trung tâm", response.getMessage());
            verify(centerRequestFeeService).completeIncomingPayment(tx, "457");
            verify(escrowService, never()).fundConfirmedPayment(any());
        }

        @Test
        @DisplayName("UTCID04 (N) - externalTransactionId da duoc xu ly (webhook gui lai) -> 'Webhook đã được xử lý trước đó', khong cong tien lan hai")
        void utcid04_duplicateWebhook() {
            when(paymentTransactionRepository.findByExternalTransactionId("123"))
                    .thenReturn(Optional.of(pendingTopup("TOPUP-ABC", AMOUNT)));

            PaymentWebhookResponse response =
                    financeService.handleSepayIncomingWebhook(incoming(123L, AMOUNT, "TOPUP-ABC"));

            assertEquals("success", response.getStatus());
            assertEquals("Webhook đã được xử lý trước đó", response.getMessage());
            verify(walletService, never()).credit(any(), any(), any());
        }

        @Test
        @DisplayName("UTCID05 (A) - request = null -> error 'Thiếu id, transferAmount hoặc nội dung giao dịch'")
        void utcid05_nullRequest() {
            PaymentWebhookResponse response = financeService.handleSepayIncomingWebhook(null);

            assertEquals("error", response.getStatus());
            assertEquals("Thiếu id, transferAmount hoặc nội dung giao dịch", response.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - id = null -> error")
        void utcid06_nullId() {
            PaymentWebhookResponse response =
                    financeService.handleSepayIncomingWebhook(incoming(null, AMOUNT, "TOPUP-ABC"));

            assertEquals("error", response.getStatus());
            assertEquals("Thiếu id, transferAmount hoặc nội dung giao dịch", response.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - transferAmount = null -> error")
        void utcid07_nullAmount() {
            PaymentWebhookResponse response =
                    financeService.handleSepayIncomingWebhook(incoming(123L, null, "TOPUP-ABC"));

            assertEquals("error", response.getStatus());
        }

        @Test
        @DisplayName("UTCID08 (B) - transferAmount = 0 (dung can duoi) -> error")
        void utcid08_zeroAmount() {
            PaymentWebhookResponse response =
                    financeService.handleSepayIncomingWebhook(incoming(123L, BigDecimal.ZERO, "TOPUP-ABC"));

            assertEquals("error", response.getStatus());
        }

        @Test
        @DisplayName("UTCID09 (A) - Noi dung chuyen khoan rong -> error")
        void utcid09_blankContent() {
            SepayWebhookRequest request = new SepayWebhookRequest();
            request.setId(123L);
            request.setTransferType("in");
            request.setTransferAmount(AMOUNT);

            PaymentWebhookResponse response = financeService.handleSepayIncomingWebhook(request);

            assertEquals("error", response.getStatus());
            assertEquals("Thiếu id, transferAmount hoặc nội dung giao dịch", response.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (A) - transferType = out -> ignored 'Giao dịch không phải tiền vào'")
        void utcid10_notIncoming() {
            SepayWebhookRequest request = incoming(123L, AMOUNT, "TOPUP-ABC");
            request.setTransferType("out");

            PaymentWebhookResponse response = financeService.handleSepayIncomingWebhook(request);

            assertEquals("ignored", response.getStatus());
            assertEquals("Giao dịch không phải tiền vào", response.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - Khong khop giao dich nao -> ignored kem thong bao khong tim thay")
        void utcid11_noMatch() {
            givenNoDuplicate("123");
            givenNoEscrowCandidate();
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.DEPOSIT, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of());

            PaymentWebhookResponse response =
                    financeService.handleSepayIncomingWebhook(incoming(123L, AMOUNT, "Khong khop gi ca"));

            assertEquals("ignored", response.getStatus());
            assertEquals("Không tìm thấy giao dịch nạp tiền/ký quỹ khớp số tiền, nội dung và tài khoản",
                    response.getMessage());
            verify(walletService, never()).credit(any(), any(), any());
        }
    }

    // =====================================================================================
    //  Sheet: financeSepayOutgoing (webhook tien ra tu SePay)
    // =====================================================================================
    @org.junit.jupiter.api.Nested
    @DisplayName("financeSepayOutgoing")
    class FinanceSepayOutgoing {

        private static final BigDecimal AMOUNT = new BigDecimal("100000.00");

        private SepayWebhookRequest outgoing(Long id, BigDecimal amount, String content) {
            SepayWebhookRequest r = new SepayWebhookRequest();
            r.setId(id);
            r.setTransferType("out");
            r.setTransferAmount(amount);
            r.setContent(content);
            r.setAccountNumber("02660559201");
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - Khop yeu cau rut tien -> 'Đã xác nhận giao dịch rút tiền từ SePay'")
        void utcid01_matchesWithdrawal() {
            LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 13, 9, 0);
            PaymentMethod method = savedPaymentMethod();
            WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, AMOUNT, requestedAt);
            PaymentTransaction tx = pendingWithdrawalTransaction(method, AMOUNT, "WITHDRAW-ABC", requestedAt);

            when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-987"))
                    .thenReturn(Optional.empty());
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.WITHDRAWAL, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of(tx));
            when(withdrawalRequestRepository
                    .findByWallet_WalletIdAndStatusAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
                            USER_ID, WithdrawalRequestStatus.PENDING, AMOUNT,
                            requestedAt.minusMinutes(5), requestedAt.plusMinutes(5)))
                    .thenReturn(List.of(withdrawal));
            when(walletService.releaseLockedFunds(USER_ID, AMOUNT, "WITHDRAW-ABC")).thenReturn(wallet);
            when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                    .thenAnswer(i -> i.getArgument(0));

            PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(
                    outgoing(987L, AMOUNT, "Chuyen tien rut vi WITHDRAW-ABC"));

            assertEquals("success", response.getStatus());
            assertEquals("Đã xác nhận giao dịch rút tiền từ SePay", response.getMessage());
            assertEquals(WithdrawalRequestStatus.COMPLETED, withdrawal.getStatus());
        }

        @Test
        @DisplayName("UTCID02 (N) - Khong khop rut tien nhung khop giao dich hoan tien -> 'Đã xác nhận giao dịch hoàn tiền từ SePay'")
        void utcid02_matchesRefund() {
            PaymentTransaction refundTx = new PaymentTransaction();
            refundTx.setTransactionId(90L);
            refundTx.setWallet(wallet);
            refundTx.setType(PaymentTransactionType.REFUND);
            refundTx.setStatus(PaymentTransactionStatus.PENDING);
            refundTx.setAmount(AMOUNT);
            refundTx.setReferenceCode("REFUND-XYZ");
            refundTx.setCreatedAt(LocalDateTime.now());

            when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-988"))
                    .thenReturn(Optional.empty());
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.WITHDRAWAL, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of());
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.REFUND, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of(refundTx));

            PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(
                    outgoing(988L, AMOUNT, "Hoan tien REFUND-XYZ"));

            assertEquals("success", response.getStatus());
            assertEquals("Đã xác nhận giao dịch hoàn tiền từ SePay", response.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (N) - externalTransactionId da xu ly -> 'Webhook đã được xử lý trước đó'")
        void utcid03_duplicateWebhook() {
            PaymentTransaction processed = pendingWithdrawalTransaction(
                    savedPaymentMethod(), AMOUNT, "WITHDRAW-ABC", LocalDateTime.now());
            when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-987"))
                    .thenReturn(Optional.of(processed));

            PaymentWebhookResponse response =
                    financeService.handleSepayOutgoingWebhook(outgoing(987L, AMOUNT, "WITHDRAW-ABC"));

            assertEquals("success", response.getStatus());
            assertEquals("Webhook đã được xử lý trước đó", response.getMessage());
            verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        }

        @Test
        @DisplayName("UTCID04 (A) - request = null -> error 'Thiếu id, transferAmount hoặc nội dung giao dịch'")
        void utcid04_nullRequest() {
            PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(null);

            assertEquals("error", response.getStatus());
            assertEquals("Thiếu id, transferAmount hoặc nội dung giao dịch", response.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - id = null -> error")
        void utcid05_nullId() {
            PaymentWebhookResponse response =
                    financeService.handleSepayOutgoingWebhook(outgoing(null, AMOUNT, "WITHDRAW-ABC"));

            assertEquals("error", response.getStatus());
        }

        @Test
        @DisplayName("UTCID06 (B) - transferAmount = 0 (dung can duoi) -> error")
        void utcid06_zeroAmount() {
            PaymentWebhookResponse response =
                    financeService.handleSepayOutgoingWebhook(outgoing(987L, BigDecimal.ZERO, "WITHDRAW-ABC"));

            assertEquals("error", response.getStatus());
        }

        @Test
        @DisplayName("UTCID07 (A) - Noi dung chuyen khoan rong -> error")
        void utcid07_blankContent() {
            SepayWebhookRequest request = new SepayWebhookRequest();
            request.setId(987L);
            request.setTransferType("out");
            request.setTransferAmount(AMOUNT);

            PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

            assertEquals("error", response.getStatus());
        }

        @Test
        @DisplayName("UTCID08 (A) - transferType = in -> ignored 'Giao dịch không phải tiền ra'")
        void utcid08_notOutgoing() {
            SepayWebhookRequest request = outgoing(987L, AMOUNT, "WITHDRAW-ABC");
            request.setTransferType("in");

            PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

            assertEquals("ignored", response.getStatus());
            assertEquals("Giao dịch không phải tiền ra", response.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Khong khop rut tien lan hoan tien -> ignored kem thong bao khong tim thay")
        void utcid09_noMatch() {
            when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-987"))
                    .thenReturn(Optional.empty());
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.WITHDRAWAL, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of());
            when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                    PaymentTransactionType.REFUND, PaymentTransactionStatus.PENDING, AMOUNT))
                    .thenReturn(List.of());

            PaymentWebhookResponse response =
                    financeService.handleSepayOutgoingWebhook(outgoing(987L, AMOUNT, "Khong khop gi ca"));

            assertEquals("ignored", response.getStatus());
            assertEquals("Không tìm thấy yêu cầu rút/hoàn tiền khớp số tiền, nội dung và tài khoản",
                    response.getMessage());
        }
    }

    // =====================================================================================
    //  Sheet: financeApproveRefundReq & financeRejectRefundReq (admin/trung tam xu ly hoan tien)
    // =====================================================================================

    private static final Long REFUND_ID = 40L;
    private static final Long ESCROW_ID = 71L;

    /** Escrow lop trung tam do centerUserId quan ly, dang FUNDED. */
    private EscrowTransaction centerEscrow(Long centerUserId) {
        User centerUser = new User();
        centerUser.setUserId(centerUserId);
        centerUser.setEmail("trungtam@tcs.com");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(3L);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.CENTER);
        tutoringClass.setCreator(centerUser);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(8L);
        classStudent.setTutoringClass(tutoringClass);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(ESCROW_ID);
        escrow.setAmount(new BigDecimal("1000000.00"));
        escrow.setStatus(EscrowStatus.FUNDED);
        escrow.setClassStudent(classStudent);
        return escrow;
    }

    private RefundRequest pendingRefund(EscrowTransaction escrow, BigDecimal amount) {
        User requester = new User();
        requester.setUserId(USER_ID);
        requester.setEmail("phuhuynh@tcs.com");

        RefundRequest refund = new RefundRequest();
        refund.setRefundId(REFUND_ID);
        refund.setEscrowTransaction(escrow);
        refund.setAmount(amount);
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setReason("Lop dung som");
        refund.setRequestedBy(requester);
        refund.setRequestedAt(LocalDateTime.now().minusDays(1));
        return refund;
    }

    private com.tcs.module.finance.dto.request.RefundDecisionRequest refundDecision(
            BigDecimal approvedAmount, String reason) {
        var r = new com.tcs.module.finance.dto.request.RefundDecisionRequest();
        r.setApprovedAmount(approvedAmount);
        r.setReason(reason);
        return r;
    }

    private void loginAsPlatformAdmin() {
        User admin = new User();
        admin.setUserId(1L);
        admin.setEmail("admin@tcs.com");
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new com.tcs.security.UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("financeApproveRefundReq")
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    class FinanceApproveRefundReq {

        @org.junit.jupiter.api.BeforeEach
        void loginAsAdmin() {
            loginAsPlatformAdmin();
            when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - Admin duyet hoan tien tren escrow thuong -> tat toan escrow qua escrowService.apply")
        void utcid01_approveNormalEscrow() {
            EscrowTransaction escrow = centerEscrow(555L);
            RefundRequest refund = pendingRefund(escrow, new BigDecimal("400000.00"));
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));
            when(escrowTransactionRepository.findById(ESCROW_ID)).thenReturn(Optional.of(escrow));

            financeService.approveRefundRequest(REFUND_ID, refundDecision(null, "Duyet hoan mot phan"));

            ArgumentCaptor<ReleaseInstruction> captor = ArgumentCaptor.forClass(ReleaseInstruction.class);
            verify(escrowService).apply(captor.capture());
            ReleaseInstruction instruction = captor.getValue();
            assertEquals(ESCROW_ID, instruction.escrowId());
            assertEquals(new BigDecimal("400000.00"), instruction.refundToPayer());
            assertEquals(new BigDecimal("600000.00"), instruction.releaseToBeneficiary(),
                    "Phan con lai cua escrow phai duoc giai ngan cho ben thu huong");
        }

        @Test
        @DisplayName("UTCID02 (N) - Yeu cau gan voi phi giu cua trung tam -> APPROVED, transferStatus = PENDING, sinh ma hoan tien")
        void utcid02_approveCenterFeeHold() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("200000.00"));
            refund.setCenterRequestFeeHold(centerFeeHold());
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));
            when(walletService.getSystemEscrowWallet()).thenReturn(wallet);

            financeService.approveRefundRequest(REFUND_ID, refundDecision(null, "Duyet hoan phi"));

            assertEquals(RefundRequestStatus.APPROVED, refund.getStatus());
            assertEquals("PENDING", refund.getTransferStatus());
            assertNotNull(refund.getProcessedAt());
            assertNotNull(refund.getRefundReferenceCode(), "Phai sinh ma hoan tien khi chua co");
            verify(escrowService, never()).apply(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - refundId = null -> 'refundId là bắt buộc'")
        void utcid03_nullRefundId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.approveRefundRequest(null, refundDecision(null, "ly do")));
            assertEquals("refundId là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - refundId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu hoàn tiền'")
        void utcid04_refundNotFound() {
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> financeService.approveRefundRequest(REFUND_ID, refundDecision(null, "ly do")));
            assertEquals("Không tìm thấy yêu cầu hoàn tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau khong con o trang thai cho -> chan duyet")
        void utcid05_refundNotPending() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("400000.00"));
            refund.setStatus(RefundRequestStatus.APPROVED);
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.approveRefundRequest(REFUND_ID, refundDecision(null, "ly do")));
            assertEquals("Chỉ yêu cầu hoàn tiền đang chờ xử lý mới có thể duyệt/từ chối", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Trung tam khong so huu lop cua yeu cau nay -> ForbiddenException")
        void utcid06_centerDoesNotOwnClass() {
            User centerUser = new User();
            centerUser.setUserId(999L);
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                    .thenReturn(new com.tcs.security.UserPrincipal(centerUser, UserRole.TUTOR_CENTER));
            when(refundRequestRepository.findById(REFUND_ID))
                    .thenReturn(Optional.of(pendingRefund(centerEscrow(555L), new BigDecimal("400000.00"))));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> financeService.approveRefundRequest(REFUND_ID, refundDecision(null, "ly do")));
            assertEquals("Bạn chỉ có quyền xử lý yêu cầu hoàn tiền của lớp trung tâm do mình quản lý",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - So tien duyet vuot qua so tien yeu cau (phi trung tam) -> chan")
        void utcid07_approvedAmountAboveRequested() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("200000.00"));
            refund.setCenterRequestFeeHold(centerFeeHold());
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.approveRefundRequest(
                            REFUND_ID, refundDecision(new BigDecimal("300000.00"), "ly do")));
            assertEquals("Số tiền hoàn được duyệt không được vượt quá số tiền yêu cầu", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - So tien duyet bang dung so tien yeu cau (phi trung tam) -> chap nhan")
        void utcid08_approvedAmountEqualsRequested() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("200000.00"));
            refund.setCenterRequestFeeHold(centerFeeHold());
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));
            when(walletService.getSystemEscrowWallet()).thenReturn(wallet);

            financeService.approveRefundRequest(
                    REFUND_ID, refundDecision(new BigDecimal("200000.00"), "Duyet toan bo"));

            assertEquals(RefundRequestStatus.APPROVED, refund.getStatus());
            assertEquals(new BigDecimal("200000.00"), refund.getAmount());
        }

        @Test
        @DisplayName("UTCID09 (A) - Nguoi goi khong phai PLATFORM_ADMIN cung khong phai TUTOR_CENTER -> ForbiddenException")
        void utcid09_wrongRole() {
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                    .thenThrow(new ForbiddenException("Không có quyền truy cập"));

            assertThrows(ForbiddenException.class,
                    () -> financeService.approveRefundRequest(REFUND_ID, refundDecision(null, "ly do")));
            verify(refundRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID10 (A) - So tien duyet vuot qua so tien escrow -> chan")
        void utcid10_approvedAmountAboveEscrow() {
            EscrowTransaction escrow = centerEscrow(555L);
            RefundRequest refund = pendingRefund(escrow, new BigDecimal("400000.00"));
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.approveRefundRequest(
                            REFUND_ID, refundDecision(new BigDecimal("2000000.00"), "ly do")));
            assertEquals("Số tiền hoàn được duyệt không được vượt quá số tiền escrow", ex.getMessage());
            verify(escrowService, never()).apply(any());
        }
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("financeRejectRefundReq")
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    class FinanceRejectRefundReq {

        @org.junit.jupiter.api.BeforeEach
        void loginAsAdmin() {
            loginAsPlatformAdmin();
            when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - Admin tu choi hoan tien tren escrow thuong -> REJECTED kem ghi chu quyet dinh")
        void utcid01_rejectNormalRefund() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("400000.00"));
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            financeService.rejectRefundRequest(REFUND_ID, refundDecision(null, "Khong du can cu hoan tien"));

            assertEquals(RefundRequestStatus.REJECTED, refund.getStatus());
            assertNotNull(refund.getProcessedAt());
            assertTrue(refund.getReason().contains("Khong du can cu hoan tien"),
                    "Ly do tu choi phai duoc noi vao reason: " + refund.getReason());
            verify(paymentNotificationService).notifyPayment(
                    any(User.class), eq("Yêu cầu hoàn tiền bị từ chối"), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("UTCID02 (N) - Yeu cau gan phi trung tam dang REFUND_REQUESTED -> tra phi ve trang thai HELD")
        void utcid02_rejectCenterFeeHold() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("200000.00"));
            var hold = centerFeeHold();
            hold.setStatus(com.tcs.module.finance.enums.CenterRequestFeeStatus.REFUND_REQUESTED);
            refund.setCenterRequestFeeHold(hold);
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            financeService.rejectRefundRequest(REFUND_ID, refundDecision(null, "Khong hoan phi"));

            assertEquals(RefundRequestStatus.REJECTED, refund.getStatus());
            assertEquals(com.tcs.module.finance.enums.CenterRequestFeeStatus.HELD, hold.getStatus());
            verify(centerRequestFeeHoldRepository).save(hold);
        }

        @Test
        @DisplayName("UTCID03 (A) - refundId = null -> 'refundId là bắt buộc'")
        void utcid03_nullRefundId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.rejectRefundRequest(null, refundDecision(null, "ly do")));
            assertEquals("refundId là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - refundId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu hoàn tiền'")
        void utcid04_refundNotFound() {
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> financeService.rejectRefundRequest(REFUND_ID, refundDecision(null, "ly do")));
            assertEquals("Không tìm thấy yêu cầu hoàn tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau khong con o trang thai cho -> chan tu choi")
        void utcid05_refundNotPending() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("400000.00"));
            refund.setStatus(RefundRequestStatus.REJECTED);
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.rejectRefundRequest(REFUND_ID, refundDecision(null, "ly do")));
            assertEquals("Chỉ yêu cầu hoàn tiền đang chờ xử lý mới có thể duyệt/từ chối", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Trung tam khong so huu lop cua yeu cau nay -> ForbiddenException")
        void utcid06_centerDoesNotOwnClass() {
            User centerUser = new User();
            centerUser.setUserId(999L);
            when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                    .thenReturn(new com.tcs.security.UserPrincipal(centerUser, UserRole.TUTOR_CENTER));
            when(refundRequestRepository.findById(REFUND_ID))
                    .thenReturn(Optional.of(pendingRefund(centerEscrow(555L), new BigDecimal("400000.00"))));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> financeService.rejectRefundRequest(REFUND_ID, refundDecision(null, "ly do")));
            assertEquals("Bạn chỉ có quyền xử lý yêu cầu hoàn tiền của lớp trung tâm do mình quản lý",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (N) - reason = null -> dung ly do mac dinh 'Từ chối yêu cầu hoàn tiền'")
        void utcid07_defaultReason() {
            RefundRequest refund = pendingRefund(centerEscrow(555L), new BigDecimal("400000.00"));
            when(refundRequestRepository.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            financeService.rejectRefundRequest(REFUND_ID, refundDecision(null, null));

            assertEquals(RefundRequestStatus.REJECTED, refund.getStatus());
            assertTrue(refund.getReason().contains("Từ chối yêu cầu hoàn tiền"),
                    "Phai dung ly do mac dinh khi khong nhap: " + refund.getReason());
        }
    }

    /** Ban ghi giu phi xu ly yeu cau cua trung tam. */
    private com.tcs.module.finance.entity.CenterRequestFeeHold centerFeeHold() {
        var hold = new com.tcs.module.finance.entity.CenterRequestFeeHold();
        hold.setFeeHoldId(60L);
        hold.setStatus(com.tcs.module.finance.enums.CenterRequestFeeStatus.HELD);
        hold.setAmount(new BigDecimal("200000.00"));
        return hold;
    }
    // =====================================================================================
    //  Sheet: financeCreateRefundReq (nguoi thanh toan gui yeu cau hoan tien)
    // =====================================================================================

    @org.junit.jupiter.api.Nested
    @DisplayName("financeCreateRefundReq")
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    class FinanceCreateRefundReq {

        private static final Long REFUND_ESCROW_ID = 91L;

        private User payer;

        @org.junit.jupiter.api.BeforeEach
        void loginAsPayer() {
            payer = new User();
            payer.setUserId(USER_ID);
            payer.setEmail("phuhuynh@tcs.com");

            when(authHelper.currentUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(payer));
            when(platformAdminRepository.findAll()).thenReturn(java.util.List.of());
            when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(i -> {
                RefundRequest saved = i.getArgument(0);
                if (saved.getRefundId() == null) {
                    saved.setRefundId(77L);
                }
                return saved;
            });
        }

        /** Escrow ma nguoi dang nhap chinh la nguoi da thanh toan (enrolledByUser). */
        private EscrowTransaction payerEscrow(EscrowStatus status, String amount) {
            TutoringClass tutoringClass = new TutoringClass();
            tutoringClass.setClassId(3L);
            tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.CENTER);

            ClassStudent classStudent = new ClassStudent();
            classStudent.setClassStudentId(8L);
            classStudent.setTutoringClass(tutoringClass);
            classStudent.setEnrolledByUser(payer);

            EscrowTransaction escrow = new EscrowTransaction();
            escrow.setEscrowId(REFUND_ESCROW_ID);
            escrow.setAmount(new BigDecimal(amount));
            escrow.setStatus(status);
            escrow.setClassStudent(classStudent);
            return escrow;
        }

        private com.tcs.module.finance.dto.request.CreateRefundRequest validRequest() {
            com.tcs.module.finance.dto.request.CreateRefundRequest request = new com.tcs.module.finance.dto.request.CreateRefundRequest();
            request.setEscrowId(REFUND_ESCROW_ID);
            request.setAmount(new BigDecimal("400000.00"));
            request.setReason("Lớp dừng sớm nên xin hoàn phần học phí còn lại");
            request.setBankName("TPBank");
            request.setAccountNo("0123456789");
            request.setAccountHolderName("Nguyen Van A");
            return request;
        }

        private void givenEscrow(EscrowTransaction escrow) {
            when(escrowTransactionRepository.findById(REFUND_ESCROW_ID)).thenReturn(Optional.of(escrow));
        }

        @Test
        @DisplayName("UTCID01 (N) - dung nguoi thanh toan, escrow da khoa tien, thong tin day du -> tao RefundRequest cho duyet")
        void utcid01_createSuccessfully() {
            EscrowTransaction escrow = payerEscrow(EscrowStatus.FUNDED, "1000000.00");
            givenEscrow(escrow);
            when(refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                    REFUND_ESCROW_ID, RefundRequestStatus.PENDING)).thenReturn(false);
            when(escrowService.holdForDispute(eq(REFUND_ESCROW_ID), anyString())).thenReturn(escrow);

            financeService.createRefundRequest(validRequest());

            ArgumentCaptor<RefundRequest> saved = ArgumentCaptor.forClass(RefundRequest.class);
            verify(refundRequestRepository).save(saved.capture());
            assertEquals(RefundRequestStatus.PENDING, saved.getValue().getStatus());
            assertEquals(new BigDecimal("400000.00"), saved.getValue().getAmount());
            assertEquals(payer, saved.getValue().getRequestedBy());
            verify(escrowService).holdForDispute(eq(REFUND_ESCROW_ID), anyString());
        }

        @Test
        @DisplayName("UTCID02 (A) - khong truyen thong tin yeu cau -> 'Thiếu thông tin yêu cầu hoàn tiền'")
        void utcid02_nullRequest() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.createRefundRequest(null));
            assertEquals("Thiếu thông tin yêu cầu hoàn tiền", ex.getMessage());
            verify(refundRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - khong truyen dinh danh escrow nao -> chan")
        void utcid03_noSelector() {
            com.tcs.module.finance.dto.request.CreateRefundRequest request = validRequest();
            request.setEscrowId(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.createRefundRequest(request));
            assertEquals("Cần cung cấp escrowId, assignmentId hoặc classStudentId", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - truyen nhieu hon mot dinh danh -> chan")
        void utcid04_multipleSelectors() {
            com.tcs.module.finance.dto.request.CreateRefundRequest request = validRequest();
            request.setAssignmentId(5L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.createRefundRequest(request));
            assertEquals("Chỉ được chọn một trong escrowId, assignmentId hoặc classStudentId",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - khong tim thay escrow -> ResourceNotFoundException")
        void utcid05_escrowNotFound() {
            when(escrowTransactionRepository.findById(REFUND_ESCROW_ID)).thenReturn(Optional.empty());

            com.tcs.exception.ResourceNotFoundException ex = assertThrows(
                    com.tcs.exception.ResourceNotFoundException.class,
                    () -> financeService.createRefundRequest(validRequest()));
            assertEquals("Không tìm thấy escrow", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - nguoi goi khong phai nguoi thanh toan escrow -> ForbiddenException")
        void utcid06_notThePayer() {
            EscrowTransaction escrow = payerEscrow(EscrowStatus.FUNDED, "1000000.00");
            User stranger = new User();
            stranger.setUserId(999L);
            escrow.getClassStudent().setEnrolledByUser(stranger);
            givenEscrow(escrow);

            com.tcs.exception.ForbiddenException ex = assertThrows(
                    com.tcs.exception.ForbiddenException.class,
                    () -> financeService.createRefundRequest(validRequest()));
            assertEquals("Chỉ người thanh toán escrow mới có quyền gửi yêu cầu hoàn tiền",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - escrow da tat toan -> khong the xu ly hoan tien")
        void utcid07_escrowAlreadySettled() {
            givenEscrow(payerEscrow(EscrowStatus.RELEASED, "1000000.00"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.createRefundRequest(validRequest()));
            assertEquals("Escrow đã tất toán nên không thể xử lý hoàn tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - escrow chua khoa tien -> khong the tao yeu cau hoan tien")
        void utcid08_escrowNotFunded() {
            givenEscrow(payerEscrow(EscrowStatus.PENDING, "1000000.00"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.createRefundRequest(validRequest()));
            assertEquals("Chỉ escrow đã khóa tiền mới có thể tạo yêu cầu hoàn tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (B) - so tien hoan = 0 (ngay tai nguong khong hop le) -> chan")
        void utcid09_zeroAmount() {
            com.tcs.module.finance.dto.request.CreateRefundRequest request = validRequest();
            request.setAmount(BigDecimal.ZERO);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.createRefundRequest(request));
            assertEquals("Số tiền hoàn phải lớn hơn 0", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (B) - so tien hoan vuot so tien escrow dung 0.01 -> chan")
        void utcid10_amountExceedsEscrow() {
            givenEscrow(payerEscrow(EscrowStatus.FUNDED, "1000000.00"));
            com.tcs.module.finance.dto.request.CreateRefundRequest request = validRequest();
            request.setAmount(new BigDecimal("1000000.01"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.createRefundRequest(request));
            assertEquals("Số tiền hoàn không được vượt quá số tiền escrow", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - khong nhap ly do hoan tien -> chan")
        void utcid11_missingReason() {
            com.tcs.module.finance.dto.request.CreateRefundRequest request = validRequest();
            request.setReason("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.createRefundRequest(request));
            assertEquals("Vui lòng nhập lý do yêu cầu hoàn tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID12 (A) - khong nhap ten chu tai khoan nhan hoan tien -> chan")
        void utcid12_missingAccountHolderName() {
            com.tcs.module.finance.dto.request.CreateRefundRequest request = validRequest();
            request.setAccountHolderName("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> financeService.createRefundRequest(request));
            assertEquals("Vui lòng nhập tên chủ tài khoản nhận hoàn tiền", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID13 (A) - escrow da co yeu cau hoan tien dang cho xu ly -> chan tao trung")
        void utcid13_pendingRefundAlreadyExists() {
            givenEscrow(payerEscrow(EscrowStatus.FUNDED, "1000000.00"));
            when(refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                    REFUND_ESCROW_ID, RefundRequestStatus.PENDING)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> financeService.createRefundRequest(validRequest()));
            assertEquals("Escrow này đã có yêu cầu hoàn tiền đang chờ xử lý", ex.getMessage());
            verify(refundRequestRepository, never()).save(any());
        }
    }

    // =====================================================================================
    //  Sheet: financeApproveWithdrawal - cac ca con lai
    // =====================================================================================

    /** Sheet financeApproveWithdrawal - UTCID02 (A): nguoi goi khong phai PLATFORM_ADMIN. */
    @Test
    void approveWithdrawalRejectsNonAdminCaller() {
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenThrow(new com.tcs.exception.ForbiddenException("Không có quyền truy cập"));

        assertThrows(com.tcs.exception.ForbiddenException.class,
                () -> financeService.approveWithdrawal(500L));
        verify(withdrawalRequestRepository, never()).save(any());
    }

    /** Sheet financeApproveWithdrawal - UTCID03 (A): yeu cau rut tien khong ton tai. */
    @Test
    void approveWithdrawalRejectsUnknownRequest() {
        when(withdrawalRequestRepository.findById(500L)).thenReturn(Optional.empty());

        com.tcs.exception.ResourceNotFoundException ex = assertThrows(
                com.tcs.exception.ResourceNotFoundException.class,
                () -> financeService.approveWithdrawal(500L));
        assertEquals("Không tìm thấy yêu cầu rút tiền", ex.getMessage());
    }
    /** Sheet financeApproveWithdrawal - UTCID05 (A): khong tim thay giao dich rut tien khop. */
    @Test
    void approveWithdrawalRejectsWhenNoMatchingTransaction() {
        BigDecimal amount = new BigDecimal("100000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 13, 9, 0);
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, savedPaymentMethod(), amount, requestedAt);

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
                .thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financeService.approveWithdrawal(15L));
        assertEquals("Không xác định được giao dịch rút tiền tương ứng", ex.getMessage());
        verify(withdrawalRequestRepository, never()).save(any());
    }
}
