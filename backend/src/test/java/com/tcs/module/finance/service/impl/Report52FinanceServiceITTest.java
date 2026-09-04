package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.finance.dto.request.CreateRefundRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.PaymentMethodRequest;
import com.tcs.module.finance.dto.request.RefundDecisionRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.AdminWithdrawalPageResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.TopupSessionResponse;
import com.tcs.module.finance.dto.response.WalletTransactionsResponse;
import com.tcs.module.finance.dto.response.WithdrawalResponse;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentMethod;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.DisputeRepository;
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
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52FinanceServiceITTest {

    private static final Long USER_ID = 7L;

    @Mock private AuthHelper authHelper;
    @Mock private WalletService walletService;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private WithdrawalRequestRepository withdrawalRequestRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private CenterRequestFeeHoldRepository centerRequestFeeHoldRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private EscrowService escrowService;
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private PaymentNotificationService paymentNotificationService;
    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ClassRequestStore classRequestStore;

    @InjectMocks
    private FinanceServiceImpl financeService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setWalletId(USER_ID);
        User owner = new User();
        owner.setUserId(USER_ID);
        owner.setEmail("finance.it@tcs.test");
        wallet.setUser(owner);
        wallet.setAvailableBalance(new BigDecimal("250000.00"));
        wallet.setFrozenBalance(new BigDecimal("50000.00"));
        wallet.setStatus(WalletStatus.ACTIVE);
        lenient().when(tutorRepository.existsByUser_UserIdAndVerificationStatus(
                        USER_ID,
                        ProfileVerificationStatus.VERIFIED))
                .thenReturn(true);
        lenient().when(tutorCenterRepository.existsByUser_UserIdAndVerificationStatus(
                        USER_ID,
                        ProfileVerificationStatus.VERIFIED))
                .thenReturn(true);
        ReflectionTestUtils.setField(financeService, "directDepositEnabled", true);
        ReflectionTestUtils.setField(financeService, "simulateTopupEnabled", true);
    }

    @Test
    void SUPPORT_FIN_CreateCenterWalletTopupQrPaymentSession() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100000"));
        request.setDescription("Nạp tiền IT");

        TopupSessionResponse response = financeService.createTopup(request);

        assertEquals(new BigDecimal("100000"), response.getAmount());
        assertEquals("PENDING", response.getStatus());
        assertTrue(response.getReference().startsWith("TOPUP-"));
        assertTrue(response.getQrUrl().contains("img.vietqr.io"));
        assertEquals("02660559201", response.getAccountNumber());
        assertEquals(response.getReference(), response.getTransferContent());
        verify(authHelper).requireRole(UserRole.TUTOR_CENTER);

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        PaymentTransaction saved = txCaptor.getValue();
        assertEquals(wallet, saved.getWallet());
        assertEquals(PaymentTransactionType.DEPOSIT, saved.getType());
        assertEquals(PaymentTransactionStatus.PENDING, saved.getStatus());
        assertEquals(new BigDecimal("100000"), saved.getAmount());
    }

    @Test
    void SUPPORT_FIN_ConfirmTopupBySepayIncomingWebhookCreditsWallet() {
        PaymentTransaction tx = pendingTopup("TOPUP-ABC", new BigDecimal("100000"));
        SepayWebhookRequest request = incomingWebhook(123L, new BigDecimal("100000"), "Chuyen khoan TOPUP-ABC");

        when(paymentTransactionRepository.findByExternalTransactionId("123")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("100000"))).thenReturn(List.of());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("100000"))).thenReturn(List.of(tx));
        when(walletService.getOrCreate(USER_ID)).thenReturn(wallet);

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("TOPUP-ABC", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("123", tx.getExternalTransactionId());
        verify(walletService).credit(USER_ID, new BigDecimal("100000"), "TOPUP-ABC");
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_009_IgnoreDuplicateIncomingWebhookWithoutSecondCredit() {
        SepayWebhookRequest request = incomingWebhook(123L, new BigDecimal("100000"), "TOPUP-ABC");
        when(paymentTransactionRepository.findByExternalTransactionId("123"))
                .thenReturn(Optional.of(pendingTopup("TOPUP-ABC", new BigDecimal("100000"))));

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        verify(walletService, never()).credit(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_ESC_001_FundEscrowAndPublishClassActivationEventAfterPaymentWebhook() {
        BigDecimal amount = new BigDecimal("500000");
        PaymentTransaction tx = pendingEscrowPayment("ESCROW-A7", amount);
        EscrowTransaction escrow = privateEscrow(5L, tx, amount);
        SepayWebhookRequest request = incomingWebhook(456L, amount, "Thanh toan hoc phi ESCROW-A7");

        when(paymentTransactionRepository.findByExternalTransactionId("456")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                amount)).thenReturn(List.of(tx));
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

    @Test
    @Tag("report52-it")
    void IT_WLT_001_CreateWithdrawalOnlyWithSavedPayoutAccount() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("100000.00"), 3L);
        PaymentMethod paymentMethod = savedPaymentMethod();

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any())).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest withdrawal = invocation.getArgument(0);
            withdrawal.setWithdrawalId(15L);
            return withdrawal;
        });

        WithdrawalResponse response = financeService.createWithdrawal(request);

        assertEquals(15L, response.getWithdrawalId());
        assertEquals(WithdrawalRequestStatus.PENDING, response.getStatus());
        assertEquals(3L, response.getPaymentMethodId());
        assertEquals("TPBank", response.getBankName());
        assertEquals("****7890", response.getAccountNoMasked());
        assertTrue(response.getReferenceCode().startsWith("WITHDRAW-"));
        verify(authHelper).requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER);
        verify(paymentMethodRepository).findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE");
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_002_AdminWithdrawalListFiltersPendingStatusAndShowsMaskedAccount() {
        BigDecimal amount = new BigDecimal("200000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 31, 9, 0);
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, amount, requestedAt);
        PaymentTransaction tx = pendingWithdrawalTransaction(method, amount, "WITHDRAW-ABC", requestedAt);

        when(withdrawalRequestRepository.findAdminList(WithdrawalRequestStatus.PENDING)).thenReturn(List.of(withdrawal));
        when(refundRequestRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of());
        when(paymentTransactionRepository
                .findByWallet_WalletIdAndTypeAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
                        USER_ID,
                        PaymentTransactionType.WITHDRAWAL,
                        amount,
                        requestedAt.minusMinutes(5),
                        requestedAt.plusMinutes(5)))
                .thenReturn(List.of(tx));

        AdminWithdrawalPageResponse response = financeService.getAdminWithdrawals(0, 10, "PENDING");

        assertEquals(1, response.getTotalElements());
        assertEquals("WITHDRAWAL", response.getContent().get(0).getRequestType());
        assertEquals("TPBank", response.getContent().get(0).getBankName());
        assertEquals("****7890", response.getContent().get(0).getAccountNoMasked());
        assertEquals("WITHDRAW-ABC", response.getContent().get(0).getReferenceCode());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_003_ListSavedPayoutMethodsMarksMostRecentAsDefault() {
        PaymentMethod latest = savedPaymentMethod();
        latest.setPaymentMethodId(4L);
        latest.setLastUsedAt(LocalDateTime.of(2026, 8, 31, 10, 0));
        PaymentMethod older = savedPaymentMethod();
        older.setPaymentMethodId(3L);
        older.setAccountNo("9999888877776666");
        older.setLastUsedAt(LocalDateTime.of(2026, 8, 30, 10, 0));

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByLastUsedAtDescPaymentMethodIdAsc(
                USER_ID,
                "ACTIVE"))
                .thenReturn(List.of(latest, older));

        List<PaymentMethodResponse> responses = financeService.getPaymentMethods();

        assertEquals(2, responses.size());
        assertEquals(4L, responses.get(0).getPaymentMethodId());
        assertEquals(Boolean.TRUE, responses.get(0).getIsDefault());
        assertEquals(Boolean.FALSE, responses.get(1).getIsDefault());
        assertEquals("****7890", responses.get(0).getAccountNoMasked());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_004_RejectWithdrawalWithoutSavedPayoutAccount() {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(new BigDecimal("100000.00"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> financeService.createWithdrawal(request));

        assertEquals("Vui lòng thêm và chọn tài khoản nhận tiền trước khi rút tiền", exception.getMessage());
        verifyNoInteractions(withdrawalRequestRepository);
        verify(walletService, never()).lockFunds(any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_005_CenterPayoutCoolingOffBlocksWithdrawal() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("100000.00"), 3L);
        PaymentMethod paymentMethod = savedPaymentMethod();
        paymentMethod.setCooldownUntil(LocalDateTime.now().plusMinutes(3));

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(paymentMethod));
        when(tutorCenterRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.of(new TutorCenter()));

        BusinessException exception = assertThrows(BusinessException.class, () -> financeService.createWithdrawal(request));

        assertEquals("Tài khoản nhận tiền mới cần chờ một thời gian trước khi rút tiền", exception.getMessage());
        verify(walletService, never()).lockFunds(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_014_CreateWithdrawalLocksWalletBalanceAndStoresPendingTransactionAmount() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("150000.00"), 3L);
        PaymentMethod paymentMethod = savedPaymentMethod();

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("150000.00")), any())).thenAnswer(invocation -> {
            wallet.setAvailableBalance(new BigDecimal("100000.00"));
            wallet.setFrozenBalance(new BigDecimal("200000.00"));
            return wallet;
        });
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest withdrawal = invocation.getArgument(0);
            withdrawal.setWithdrawalId(17L);
            return withdrawal;
        });

        WithdrawalResponse response = financeService.createWithdrawal(request);

        assertEquals(WithdrawalRequestStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("100000.00"), wallet.getAvailableBalance());
        assertEquals(new BigDecimal("200000.00"), wallet.getFrozenBalance());
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        assertEquals(PaymentTransactionType.WITHDRAWAL, txCaptor.getValue().getType());
        assertEquals(PaymentTransactionStatus.PENDING, txCaptor.getValue().getStatus());
        assertEquals(new BigDecimal("150000.00"), txCaptor.getValue().getAmount());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_006_UnauthenticatedWithdrawalRequestIsBlockedBeforeWalletMutation() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("100000.00"), 3L);

        when(authHelper.requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER))
                .thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> financeService.createWithdrawal(request));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(walletService, never()).getRequired(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_007_ClientRoleCannotCreateWithdrawal() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("100000.00"), 3L);

        when(authHelper.requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER))
                .thenThrow(new ForbiddenException("Yêu cầu quyền TUTOR hoặc TUTOR_CENTER"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> financeService.createWithdrawal(request));

        assertEquals("Yêu cầu quyền TUTOR hoặc TUTOR_CENTER", exception.getMessage());
        verify(walletService, never()).getRequired(any());
        verify(withdrawalRequestRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_008_CreatePaymentMethodReusesExistingBankAccountWithoutDuplicateRow() {
        PaymentMethod existing = savedPaymentMethod();
        PaymentMethodRequest request = paymentMethodRequest(" TPBank ", " 1234567890 ", " Nguyễn  Văn  A ");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(paymentMethodRepository.findByWallet_WalletIdAndStatusOrderByLastUsedAtDescPaymentMethodIdAsc(
                USER_ID,
                "ACTIVE"))
                .thenReturn(List.of(existing));
        when(paymentMethodRepository.findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
                USER_ID,
                "TPBank",
                "1234567890",
                "ACTIVE"))
                .thenReturn(Optional.of(existing));

        PaymentMethodResponse response = financeService.createPaymentMethod(request);

        assertEquals(3L, response.getPaymentMethodId());
        assertEquals("TPBank", response.getBankName());
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_010_CreateWithdrawalNotifiesAdminsAndRequester() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("100000.00"), 3L);
        PaymentMethod paymentMethod = savedPaymentMethod();
        User adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setEmail("admin.it@tcs.test");
        PlatformAdmin admin = new PlatformAdmin();
        admin.setUser(adminUser);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any())).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest withdrawal = invocation.getArgument(0);
            withdrawal.setWithdrawalId(15L);
            return withdrawal;
        });
        when(platformAdminRepository.findAll()).thenReturn(List.of(admin));

        WithdrawalResponse response = financeService.createWithdrawal(request);

        assertEquals(15L, response.getWithdrawalId());
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu rút tiền mới"),
                any(),
                eq("WITHDRAWAL_REQUEST"),
                eq(15L));
        verify(paymentNotificationService).notifyPayment(
                eq(USER_ID),
                eq("Đã tạo yêu cầu rút tiền"),
                any(),
                eq("WITHDRAWAL_REQUEST"),
                eq(15L));
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_011_CreateWithdrawalNotificationUsesWithdrawalRequestReferenceForAdminQueue() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("120000.00"), 3L);
        PaymentMethod paymentMethod = savedPaymentMethod();
        User adminUser = user(1L, "admin.it@tcs.test");
        PlatformAdmin admin = new PlatformAdmin();
        admin.setUser(adminUser);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("120000.00")), any())).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest withdrawal = invocation.getArgument(0);
            withdrawal.setWithdrawalId(16L);
            return withdrawal;
        });
        when(platformAdminRepository.findAll()).thenReturn(List.of(admin));

        WithdrawalResponse response = financeService.createWithdrawal(request);

        assertEquals(16L, response.getWithdrawalId());
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu rút tiền mới"),
                any(),
                eq("WITHDRAWAL_REQUEST"),
                eq(16L));
        verify(paymentNotificationService).notifyPayment(
                eq(USER_ID),
                eq("Đã tạo yêu cầu rút tiền"),
                any(),
                eq("WITHDRAWAL_REQUEST"),
                eq(16L));
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_012_RejectTransactionHistoryWhenDateRangeIsInvalid() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> financeService.getMyTransactions(
                        0,
                        20,
                        null,
                        LocalDate.of(2026, 8, 31),
                        LocalDate.of(2026, 8, 1)));

        assertEquals("Ngày bắt đầu không được sau ngày kết thúc", exception.getMessage());
        verify(paymentTransactionRepository, never()).findByWalletIdWithFilters(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_019_WithdrawalApprovalNotificationTargetsWalletOwner() {
        BigDecimal amount = new BigDecimal("100000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 31, 9, 0);
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, amount, requestedAt);
        PaymentTransaction tx = pendingWithdrawalTransaction(method, amount, "WITHDRAW-ABC", requestedAt);

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

        financeService.approveWithdrawal(15L);

        verify(paymentNotificationService).notifyPayment(
                eq(USER_ID),
                eq("Yêu cầu rút tiền đã được duyệt"),
                any(),
                eq("WITHDRAWAL_REQUEST"),
                eq(15L));
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_020_CreateWalletChecksPenaltyAccessBeforeOpeningBalance() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.create(USER_ID)).thenReturn(wallet);

        var response = financeService.createMyWallet();

        assertEquals(USER_ID, response.getWalletId());
        verify(authHelper).requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER);
        verify(penaltyAccessService).requireFeature(USER_ID, "WITHDRAWAL");
        verify(walletService).create(USER_ID);
    }

    @Test
    void SUPPORT_FIN_TutorPayoutDoesNotUseCenterCoolingOffRule() {
        CreateWithdrawalRequest request = withdrawalRequest(new BigDecimal("100000.00"), 3L);
        PaymentMethod paymentMethod = savedPaymentMethod();
        paymentMethod.setCooldownUntil(LocalDateTime.now().plusMinutes(3));

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);
        when(walletService.lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any())).thenReturn(wallet);
        when(paymentMethodRepository.findByPaymentMethodIdAndWallet_WalletIdAndStatus(3L, USER_ID, "ACTIVE"))
                .thenReturn(Optional.of(paymentMethod));
        when(tutorCenterRepository.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalResponse response = financeService.createWithdrawal(request);

        assertEquals(WithdrawalRequestStatus.PENDING, response.getStatus());
        verify(walletService).lockFunds(eq(USER_ID), eq(new BigDecimal("100000.00")), any());
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_016_AdminApprovalKeepsWithdrawalPendingUntilManualTransferIsConfirmed() {
        BigDecimal amount = new BigDecimal("100000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 31, 9, 0);
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, amount, requestedAt);
        PaymentTransaction tx = pendingWithdrawalTransaction(method, amount, "WITHDRAW-ABC", requestedAt);

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

        WithdrawalResponse response = financeService.approveWithdrawal(15L);

        assertEquals(WithdrawalRequestStatus.APPROVED, response.getStatus());
        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
        assertEquals("Yêu cầu rút tiền đã được duyệt, chờ đối soát SePay", tx.getDescription());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(paymentTransactionRepository).save(tx);
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_013_CompleteWithdrawalOnlyAfterOutgoingSepayWebhook() {
        BigDecimal amount = new BigDecimal("100000.00");
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 31, 9, 0);
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(15L, method, amount, requestedAt);
        PaymentTransaction tx = pendingWithdrawalTransaction(method, amount, "WITHDRAW-ABC", requestedAt);
        SepayWebhookRequest request = outgoingWebhook(987L, amount, "Chuyen tien rut vi WITHDRAW-ABC");

        when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-987")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.WITHDRAWAL,
                PaymentTransactionStatus.PENDING,
                amount)).thenReturn(List.of(tx));
        when(withdrawalRequestRepository
                .findByWallet_WalletIdAndStatusAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
                        USER_ID,
                        WithdrawalRequestStatus.PENDING,
                        amount,
                        requestedAt.minusMinutes(5),
                        requestedAt.plusMinutes(5)))
                .thenReturn(List.of(withdrawal));
        when(walletService.releaseLockedFunds(USER_ID, amount, "WITHDRAW-ABC")).thenReturn(wallet);
        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("WITHDRAW-ABC", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("SEPAY-OUT-987", tx.getExternalTransactionId());
        assertEquals(WithdrawalRequestStatus.COMPLETED, withdrawal.getStatus());
        verify(walletService).releaseLockedFunds(USER_ID, amount, "WITHDRAW-ABC");
    }

    @Test
    @Tag("report52-it")
    void IT_WLT_015_ListWalletTransactionHistoryWithTypeAndDateFilter() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(99L);
        tx.setType(PaymentTransactionType.DEPOSIT);
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setAmount(new BigDecimal("100000.00"));
        tx.setDescription("Nạp tiền ví");
        tx.setReferenceCode("TOPUP-1");
        tx.setCreatedAt(LocalDateTime.of(2026, 8, 20, 9, 30));

        when(paymentTransactionRepository.findByWalletIdWithFilters(
                eq(USER_ID),
                eq(PaymentTransactionType.DEPOSIT),
                eq(LocalDate.of(2026, 8, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 8, 31).atTime(23, 59, 59, 999999999)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        WalletTransactionsResponse response = financeService.getMyTransactions(
                0, 20, "deposit", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTransactions().size());
        assertEquals(PaymentTransactionType.DEPOSIT, response.getTransactions().get(0).getType());
    }

    @Test
    void SUPPORT_FIN_RejectUnknownWalletTransactionType() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.getRequired(USER_ID)).thenReturn(wallet);

        assertThrows(IllegalArgumentException.class,
                () -> financeService.getMyTransactions(0, 20, "UNKNOWN", null, null));
        verify(paymentTransactionRepository, never()).findByWalletIdWithFilters(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_002_ShowRefundTransferInAdminWithdrawalQueue() {
        RefundRequest refund = approvedRefundTransfer(77L, new BigDecimal("30000.00"));
        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setType(PaymentTransactionType.REFUND);
        refundTx.setStatus(PaymentTransactionStatus.PENDING);
        refundTx.setReferenceCode("REFUND-ESCROW-10");

        when(withdrawalRequestRepository.findAdminList(isNull())).thenReturn(List.of());
        when(refundRequestRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of(refund));
        when(paymentTransactionRepository.findByReferenceCode("REFUND-ESCROW-10")).thenReturn(Optional.of(refundTx));

        AdminWithdrawalPageResponse response = financeService.getAdminWithdrawals(0, 10, null);

        assertEquals(1, response.getTotalElements());
        assertEquals("REFUND", response.getContent().get(0).getRequestType());
        assertEquals("TPBank", response.getContent().get(0).getBankName());
        assertEquals("Nguyen Thu Ha", response.getContent().get(0).getAccountHolderName());
        assertEquals(PaymentTransactionStatus.PENDING, response.getContent().get(0).getTransactionStatus());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
    }

    @Test
    @Tag("report52-it")
    void IT_SET_013_CompleteRefundTransferAfterOutgoingSepayWebhook() {
        BigDecimal amount = new BigDecimal("30000.00");
        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setWallet(wallet);
        refundTx.setType(PaymentTransactionType.REFUND);
        refundTx.setStatus(PaymentTransactionStatus.PENDING);
        refundTx.setAmount(amount);
        refundTx.setReferenceCode("REFUND-ESCROW-10");
        RefundRequest refund = approvedRefundTransfer(77L, amount);
        SepayWebhookRequest request = outgoingWebhook(990L, amount, "Hoan tien REFUND-ESCROW-10");

        when(paymentTransactionRepository.findByExternalTransactionId("SEPAY-OUT-990")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.WITHDRAWAL,
                PaymentTransactionStatus.PENDING,
                amount)).thenReturn(List.of());
        when(paymentTransactionRepository.findByTypeAndStatusAndAmount(
                PaymentTransactionType.REFUND,
                PaymentTransactionStatus.PENDING,
                amount)).thenReturn(List.of(refundTx));
        when(refundRequestRepository.findByRefundReferenceCode("REFUND-ESCROW-10")).thenReturn(Optional.of(refund));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentWebhookResponse response = financeService.handleSepayOutgoingWebhook(request);

        assertEquals("success", response.getStatus());
        assertEquals("REFUND-ESCROW-10", response.getReference());
        assertEquals(PaymentTransactionStatus.SUCCESS, refundTx.getStatus());
        assertEquals(RefundRequestStatus.COMPLETED, refund.getStatus());
        assertEquals("SUCCESS", refund.getTransferStatus());
        verify(paymentNotificationService).notifyPayment(
                any(User.class),
                eq("Hoàn tiền đã chuyển khoản"),
                eq("Khoản hoàn 30000 đ đã được xác nhận qua SePay."),
                eq("REFUND_REQUEST"),
                eq(77L));
    }

    @Test
    @Tag("report52-it")
    void IT_SET_006_UnauthenticatedRefundApprovalIsBlockedBeforeStateChange() {
        RefundDecisionRequest request = refundDecision(new BigDecimal("30000.00"), "Duyệt hoàn tiền");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> financeService.approveRefundRequest(77L, request));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(refundRequestRepository, never()).findById(any());
        verify(escrowService, never()).apply(any());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_007_CenterCannotApproveRefundForPrivateEscrowOutsideItsClass() {
        User centerUser = user(USER_ID, "center.it@tcs.test");
        RefundRequest refund = pendingRefund(77L, privateFundedEscrow(10L, new BigDecimal("500000.00")));

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(centerUser, UserRole.TUTOR_CENTER));
        when(refundRequestRepository.findById(77L)).thenReturn(Optional.of(refund));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> financeService.approveRefundRequest(77L, refundDecision(new BigDecimal("300000.00"), "Không đúng lớp trung tâm")));

        assertEquals("Bạn chỉ có quyền xử lý yêu cầu hoàn tiền của lớp trung tâm do mình quản lý", exception.getMessage());
        verify(refundRequestRepository, never()).save(any());
        verify(escrowService, never()).apply(any());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_008_RejectDuplicatePendingRefundForSameEscrow() {
        EscrowTransaction escrow = privateFundedEscrow(10L, new BigDecimal("500000.00"));
        CreateRefundRequest request = createRefundRequest(10L, new BigDecimal("300000.00"));

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(wallet.getUser()));
        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));
        when(refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(10L, RefundRequestStatus.PENDING))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> financeService.createRefundRequest(request));

        assertEquals("Escrow này đã có yêu cầu hoàn tiền đang chờ xử lý", exception.getMessage());
        verify(refundRequestRepository, never()).save(any());
        verify(escrowService, never()).holdForDispute(any(), any());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_009_AdminRefundRequestStatusFilterReturnsPendingRowsOnly() {
        RefundRequest pending = pendingRefund(77L, privateFundedEscrow(10L, new BigDecimal("500000.00")));
        User admin = user(1L, "admin.it@tcs.test");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(refundRequestRepository.findByStatusOrderByRequestedAtDesc(RefundRequestStatus.PENDING))
                .thenReturn(List.of(pending));

        var responses = financeService.getAdminRefundRequests("PENDING");

        assertEquals(1, responses.size());
        assertEquals(77L, responses.get(0).getRefundId());
        assertEquals(RefundRequestStatus.PENDING, responses.get(0).getStatus());
    }

    @Test
    @Tag("report52-it")
    void IT_SET_010_CreateRefundRequestHoldsEscrowAndNotifiesAdminAndRequester() {
        EscrowTransaction escrow = privateFundedEscrow(10L, new BigDecimal("500000.00"));
        CreateRefundRequest request = createRefundRequest(10L, new BigDecimal("300000.00"));
        User adminUser = user(1L, "admin.it@tcs.test");
        PlatformAdmin admin = new PlatformAdmin();
        admin.setUser(adminUser);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(wallet.getUser()));
        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));
        when(refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(10L, RefundRequestStatus.PENDING))
                .thenReturn(false);
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refund = invocation.getArgument(0);
            refund.setRefundId(77L);
            return refund;
        });
        when(escrowService.holdForDispute(10L, "Yêu cầu hoàn tiền #77")).thenAnswer(invocation -> {
            escrow.setStatus(EscrowStatus.ON_HOLD);
            return escrow;
        });
        when(platformAdminRepository.findAll()).thenReturn(List.of(admin));

        var response = financeService.createRefundRequest(request);

        assertEquals(77L, response.getRefundId());
        assertEquals(RefundRequestStatus.PENDING, response.getStatus());
        assertEquals(EscrowStatus.ON_HOLD, response.getEscrowStatus());
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu hoàn tiền mới"),
                any(),
                eq("REFUND_REQUEST"),
                eq(77L));
        verify(paymentNotificationService).notifyPayment(
                eq(wallet.getUser()),
                eq("Đã gửi yêu cầu hoàn tiền"),
                any(),
                eq("REFUND_REQUEST"),
                eq(77L));
    }

    @Test
    @Tag("report52-it")
    void IT_SET_017_RejectCenterRequestFeeRefundRestoresHeldFeeHold() {
        User admin = user(1L, "admin.it@tcs.test");
        CenterRequestFeeHold hold = centerFeeHold(60L, CenterRequestFeeStatus.REFUND_REQUESTED);
        RefundRequest refund = pendingCenterFeeRefund(77L, hold);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(refundRequestRepository.findById(77L)).thenReturn(Optional.of(refund));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classRequestStore.find("REQ-CF")).thenReturn(Optional.empty());

        var response = financeService.rejectRefundRequest(77L, refundDecision(null, "Trung tâm đã xử lý lại yêu cầu"));

        assertEquals(RefundRequestStatus.REJECTED, response.getStatus());
        assertEquals(CenterRequestFeeStatus.HELD, hold.getStatus());
        verify(centerRequestFeeHoldRepository).save(hold);
        verify(paymentNotificationService).notifyPayment(
                eq(refund.getRequestedBy()),
                eq("Yêu cầu hoàn phí trung tâm bị từ chối"),
                eq("Trung tâm đã xử lý lại yêu cầu"),
                eq("REFUND_REQUEST"),
                eq(77L));
    }

    @Test
    @Tag("report52-it")
    void IT_SET_019_ApproveCenterRequestFeeRefundCreatesPendingAdminTransfer() {
        User admin = user(1L, "admin.it@tcs.test");
        CenterRequestFeeHold hold = centerFeeHold(60L, CenterRequestFeeStatus.REFUND_REQUESTED);
        RefundRequest refund = pendingCenterFeeRefund(77L, hold);
        Wallet systemWallet = new Wallet();
        systemWallet.setWalletId(999L);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(refundRequestRepository.findById(77L)).thenReturn(Optional.of(refund));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.getSystemEscrowWallet()).thenReturn(systemWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classRequestStore.find("REQ-CF")).thenReturn(Optional.empty());

        var response = financeService.approveRefundRequest(
                77L,
                refundDecision(new BigDecimal("30000.00"), "Duyệt hoàn phí trung tâm"));

        assertEquals(RefundRequestStatus.APPROVED, response.getStatus());
        assertEquals("PENDING", response.getTransferStatus());
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        PaymentTransaction tx = txCaptor.getValue();
        assertEquals(PaymentTransactionType.REFUND, tx.getType());
        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
        assertEquals("REFUND-CREQFEE-60", tx.getReferenceCode());
        verify(paymentNotificationService).notifyPayment(
                eq(refund.getRequestedBy()),
                eq("Yêu cầu hoàn phí trung tâm đã được duyệt"),
                any(),
                eq("REFUND_REQUEST"),
                eq(77L));
    }

    private RefundDecisionRequest refundDecision(BigDecimal approvedAmount, String reason) {
        RefundDecisionRequest request = new RefundDecisionRequest();
        request.setApprovedAmount(approvedAmount);
        request.setReason(reason);
        return request;
    }

    private CreateRefundRequest createRefundRequest(Long escrowId, BigDecimal amount) {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setEscrowId(escrowId);
        request.setAmount(amount);
        request.setReason("Phụ huynh yêu cầu hoàn tiền theo thỏa thuận xử lý");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Thu Ha");
        return request;
    }

    private EscrowTransaction privateFundedEscrow(Long escrowId, BigDecimal amount) {
        PaymentTransaction payment = pendingEscrowPayment("ESCROW-" + escrowId, amount);
        payment.setWallet(wallet);
        EscrowTransaction escrow = privateEscrow(escrowId, payment, amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
    }

    private RefundRequest pendingRefund(Long refundId, EscrowTransaction escrow) {
        RefundRequest refund = new RefundRequest();
        refund.setRefundId(refundId);
        refund.setEscrowTransaction(escrow);
        refund.setRequestedBy(wallet.getUser());
        refund.setAmount(new BigDecimal("300000.00"));
        refund.setBankName("TPBank");
        refund.setAccountNo("0123456789");
        refund.setAccountHolderName("Nguyen Thu Ha");
        refund.setReason("""
                Phụ huynh yêu cầu hoàn tiền

                Thông tin nhận hoàn tiền:
                - Tên chủ tài khoản: Nguyen Thu Ha
                - Ngân hàng: TPBank
                - Số tài khoản: 0123456789
                """);
        refund.setTransferStatus("PENDING");
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setRequestedAt(LocalDateTime.of(2026, 8, 31, 9, 30));
        return refund;
    }

    private CenterRequestFeeHold centerFeeHold(Long holdId, CenterRequestFeeStatus status) {
        CenterRequestFeeHold hold = new CenterRequestFeeHold();
        hold.setFeeHoldId(holdId);
        hold.setRequestId("REQ-CF");
        hold.setClientUserId(USER_ID);
        hold.setCenterUserId(22L);
        hold.setCenterName("Trung tâm Minh Tâm");
        hold.setProjectedEscrowAmount(new BigDecimal("500000.00"));
        hold.setAmount(new BigDecimal("30000.00"));
        hold.setReferenceCode("CENTERREQ-ABC");
        hold.setPayoutBankName("TPBank");
        hold.setPayoutAccountNo("0123456789");
        hold.setPayoutAccountHolderName("Nguyen Thu Ha");
        hold.setStatus(status);
        return hold;
    }

    private RefundRequest pendingCenterFeeRefund(Long refundId, CenterRequestFeeHold hold) {
        RefundRequest refund = new RefundRequest();
        refund.setRefundId(refundId);
        refund.setCenterRequestFeeHold(hold);
        refund.setRequestedBy(wallet.getUser());
        refund.setAmount(new BigDecimal("30000.00"));
        refund.setBankName("TPBank");
        refund.setAccountNo("0123456789");
        refund.setAccountHolderName("Nguyen Thu Ha");
        refund.setRefundReferenceCode("REFUND-CREQFEE-" + hold.getFeeHoldId());
        refund.setTransferStatus("PENDING");
        refund.setReason("Hoàn phí nhờ trung tâm");
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setRequestedAt(LocalDateTime.of(2026, 8, 31, 9, 30));
        return refund;
    }

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPasswordHash("hash");
        return user;
    }

    private DepositRequest topupRequest(String amount) {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private CreateWithdrawalRequest withdrawalRequest(BigDecimal amount, Long paymentMethodId) {
        CreateWithdrawalRequest request = new CreateWithdrawalRequest();
        request.setAmount(amount);
        request.setPaymentMethodId(paymentMethodId);
        return request;
    }

    private PaymentMethodRequest paymentMethodRequest(String bankName, String accountNo, String accountHolderName) {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setBankName(bankName);
        request.setAccountNo(accountNo);
        request.setAccountHolderName(accountHolderName);
        return request;
    }

    private SepayWebhookRequest incomingWebhook(Long id, BigDecimal amount, String content) {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(id);
        request.setTransferType("in");
        request.setTransferAmount(amount);
        request.setContent(content);
        request.setAccountNumber("02660559201");
        return request;
    }

    private SepayWebhookRequest outgoingWebhook(Long id, BigDecimal amount, String content) {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(id);
        request.setTransferType("out");
        request.setTransferAmount(amount);
        request.setContent(content);
        request.setAccountNumber("02660559201");
        return request;
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

    private RefundRequest approvedRefundTransfer(Long refundId, BigDecimal amount) {
        User client = new User();
        client.setUserId(USER_ID);
        client.setEmail("client@tcs.com");

        RefundRequest refund = new RefundRequest();
        refund.setRefundId(refundId);
        refund.setRequestedBy(client);
        refund.setAmount(amount);
        refund.setBankName("TPBank");
        refund.setAccountNo("0123456789");
        refund.setAccountHolderName("Nguyen Thu Ha");
        refund.setReason("""
                Hoàn tiền theo quyết định xử lý

                Thông tin nhận hoàn tiền:
                - Tên chủ tài khoản: Nguyen Thu Ha
                - Ngân hàng: TPBank
                - Số tài khoản: 0123456789
                """);
        refund.setRefundReferenceCode("REFUND-ESCROW-10");
        refund.setTransferStatus("PENDING");
        refund.setStatus(RefundRequestStatus.APPROVED);
        refund.setProcessedAt(LocalDateTime.of(2026, 8, 31, 10, 0));
        refund.setRequestedAt(LocalDateTime.of(2026, 8, 31, 9, 30));
        return refund;
    }
}
