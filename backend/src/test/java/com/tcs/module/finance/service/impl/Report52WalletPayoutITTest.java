package com.tcs.module.finance.service.impl;

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
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52WalletPayoutITTest {


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
    private PaymentReconciliationService reconciliationService;

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
        ReflectionTestUtils.setField(financeService, "directDepositEnabled", true);
        ReflectionTestUtils.setField(financeService, "simulateTopupEnabled", true);
        reconciliationService = new PaymentReconciliationService(
                paymentTransactionRepository,
                withdrawalRequestRepository,
                walletService,
                paymentNotificationService);
    }

    
    /**
     * Test Case: IT-WLT-001
     * Title: Create a withdrawal request only with an active saved payout account.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: amount=100000; paymentMethodId=3.
     * Steps:
     *   1. Prepare the fixture: Authenticated tutor/center wallet has enough available balance and active payment method 3.
     *   2. Use the input: amount=100000; paymentMethodId=3.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_001_CreateWithdrawalOnlyWithSavedPayoutAccount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response fields, role guard and owner-scoped payment-method lookup.
     * Expected: A PENDING withdrawal 15 is created for payout method 3, the account is masked as ****7890 and a WITHDRAW reference is returned.
     * Pre-conditions: Authenticated tutor/center wallet has enough available balance and active payment method 3.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-001: Create a withdrawal request only with an active saved payout account.")
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

    /**
     * Test Case: IT-WLT-002
     * Title: Show pending withdrawals in the admin list with a masked bank account.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.getAdminWithdrawals (GET /api/finance/withdrawals?status=PENDING).
     * Input: page=0; size=10; status=PENDING.
     * Steps:
     *   1. Prepare the fixture: A pending withdrawal and matching pending transaction exist.
     *   2. Use the input: page=0; size=10; status=PENDING.
     *   3. Execute FinanceServiceImpl.getAdminWithdrawals (GET /api/finance/withdrawals?status=PENDING). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_002_AdminWithdrawalListFiltersPendingStatusAndShowsMaskedAccount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert total, request type, bank, masked account and reference.
     * Expected: The admin response contains one WITHDRAWAL row for 200000, TPBank, masked account ****7890 and reference WITHDRAW-ABC.
     * Pre-conditions: A pending withdrawal and matching pending transaction exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-002: Show pending withdrawals in the admin list with a masked bank account.")
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

    /**
     * Test Case: IT-WLT-003
     * Title: List saved payout methods with the most recently used method marked default.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.getPaymentMethods (GET /api/finance/payment-methods).
     * Input: Authenticated tutor/center session.
     * Steps:
     *   1. Prepare the fixture: The current wallet has active methods 4 and 3 with different lastUsedAt values.
     *   2. Use the input: Authenticated tutor/center session.
     *   3. Execute FinanceServiceImpl.getPaymentMethods (GET /api/finance/payment-methods). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_003_ListSavedPayoutMethodsMarksMostRecentAsDefault.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert order, default flags and masked account.
     * Expected: Two active methods are returned in last-used order; method 4 isDefault=true and its account is masked.
     * Pre-conditions: The current wallet has active methods 4 and 3 with different lastUsedAt values.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-003: List saved payout methods with the most recently used method marked default.")
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

    /**
     * Test Case: IT-WLT-004
     * Title: Reject a withdrawal when no payout account is selected.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: amount=100000; paymentMethodId=null.
     * Steps:
     *   1. Prepare the fixture: Authenticated earning user has no selected payout method.
     *   2. Use the input: amount=100000; paymentMethodId=null.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_004_RejectWithdrawalWithoutSavedPayoutAccount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify wallet lock/withdrawal repository interactions are absent.
     * Expected: The service returns the add/select-account message and does not lock funds or create a withdrawal.
     * Pre-conditions: Authenticated earning user has no selected payout method.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-004: Reject a withdrawal when no payout account is selected.")
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

    /**
     * Test Case: IT-WLT-005
     * Title: Apply cooling-off to a new center payout account.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: amount=100000; paymentMethodId=3.
     * Steps:
     *   1. Prepare the fixture: Center profile exists; selected payment method cooldownUntil is three minutes in the future.
     *   2. Use the input: amount=100000; paymentMethodId=3.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_005_CenterPayoutCoolingOffBlocksWithdrawal.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the cooling-off message and verify no transaction save or wallet lock.
     * Expected: A center withdrawal using a payout account whose cooldown has not ended is rejected before funds are locked.
     * Pre-conditions: Center profile exists; selected payment method cooldownUntil is three minutes in the future.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-005: Apply cooling-off to a new center payout account.")
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

    /**
     * Test Case: IT-WLT-006
     * Title: Block an unauthenticated withdrawal before touching the wallet.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: Valid withdrawal payload.
     * Steps:
     *   1. Prepare the fixture: No authenticated tutor/center principal.
     *   2. Use the input: Valid withdrawal payload.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_006_UnauthenticatedWithdrawalRequestIsBlockedBeforeWalletMutation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify wallet/payment writes are skipped.
     * Expected: The service returns “Yêu cầu đăng nhập” and does not load or mutate the wallet.
     * Pre-conditions: No authenticated tutor/center principal.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-006: Block an unauthenticated withdrawal before touching the wallet.")
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

    /**
     * Test Case: IT-WLT-007
     * Title: Prevent a client from creating a withdrawal.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: amount=100000; paymentMethodId=3.
     * Steps:
     *   1. Prepare the fixture: Authenticated user has CLIENT role.
     *   2. Use the input: amount=100000; paymentMethodId=3.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_007_ClientRoleCannotCreateWithdrawal.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role error and verify wallet/withdrawal access is skipped.
     * Expected: The role guard returns the TUTOR/TUTOR_CENTER permission error and no withdrawal is saved.
     * Pre-conditions: Authenticated user has CLIENT role.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-007: Prevent a client from creating a withdrawal.")
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

    /**
     * Test Case: IT-WLT-008
     * Title: Reuse an existing bank payout account instead of inserting a duplicate.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createPaymentMethod (POST /api/finance/payment-methods).
     * Input: “ TPBank ” / “ 1234567890 ” / “ Nguyễn Văn A ”.
     * Steps:
     *   1. Prepare the fixture: The current wallet already has the same active bank/account number.
     *   2. Use the input: “ TPBank ” / “ 1234567890 ” / “ Nguyễn Văn A ”.
     *   3. Execute FinanceServiceImpl.createPaymentMethod (POST /api/finance/payment-methods). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_008_CreatePaymentMethodReusesExistingBankAccountWithoutDuplicateRow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert normalized response and no duplicate save.
     * Expected: Trimmed TPBank/account/holder data resolves to method 3 and PaymentMethodRepository.save is not called.
     * Pre-conditions: The current wallet already has the same active bank/account number.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-008: Reuse an existing bank payout account instead of inserting a duplicate.")
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

    /**
     * Test Case: IT-WLT-009
     * Title: Ignore a repeated incoming wallet webhook without a second credit.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.handleSepayWebhook (POST /api/finance/webhooks/sepay).
     * Input: Incoming amount 100000; reference TOPUP-ABC; external id 123.
     * Steps:
     *   1. Prepare the fixture: A transaction with external id 123 already exists.
     *   2. Use the input: Incoming amount 100000; reference TOPUP-ABC; external id 123.
     *   3. Execute FinanceServiceImpl.handleSepayWebhook (POST /api/finance/webhooks/sepay). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_009_IgnoreDuplicateIncomingWebhookWithoutSecondCredit.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response and verify no second credit/save.
     * Expected: The webhook returns success while wallet credit and payment save are both skipped for the duplicate external id.
     * Pre-conditions: A transaction with external id 123 already exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-009: Ignore a repeated incoming wallet webhook without a second credit.")
    void IT_WLT_009_IgnoreDuplicateIncomingWebhookWithoutSecondCredit() {
        SepayWebhookRequest request = incomingWebhook(123L, new BigDecimal("100000"), "TOPUP-ABC");
        when(paymentTransactionRepository.findByExternalTransactionId("123"))
                .thenReturn(Optional.of(pendingTopup("TOPUP-ABC", new BigDecimal("100000"))));

        PaymentWebhookResponse response = financeService.handleSepayWebhook(request);

        assertEquals("success", response.getStatus());
        verify(walletService, never()).credit(any(), any(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-WLT-010
     * Title: Notify administrators and the requester when a withdrawal is created.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: amount=100000; paymentMethodId=3.
     * Steps:
     *   1. Prepare the fixture: Tutor/center wallet and payout method are valid; an active platform admin exists.
     *   2. Use the input: amount=100000; paymentMethodId=3.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_010_CreateWithdrawalNotifiesAdminsAndRequester.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture both recipients, Vietnamese messages, template and reference id.
     * Expected: The new withdrawal 15 triggers one admin notification and one requester notification with WITHDRAWAL_REQUEST reference.
     * Pre-conditions: Tutor/center wallet and payout method are valid; an active platform admin exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-010: Notify administrators and the requester when a withdrawal is created.")
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

    /**
     * Test Case: IT-WLT-011
     * Title: Use the withdrawal request reference in the admin notification.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: amount=120000; paymentMethodId=3.
     * Steps:
     *   1. Prepare the fixture: A valid withdrawal can be created for the current earning user.
     *   2. Use the input: amount=120000; paymentMethodId=3.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_011_CreateWithdrawalNotificationUsesWithdrawalRequestReferenceForAdminQueue.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture admin notification reference type/id.
     * Expected: The notification points to withdrawal request 16 with reference type WITHDRAWAL_REQUEST so the admin queue can open it.
     * Pre-conditions: A valid withdrawal can be created for the current earning user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-011: Use the withdrawal request reference in the admin notification.")
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

    /**
     * Test Case: IT-WLT-012
     * Title: Reject wallet transaction history when the start date is after the end date.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.getMyTransactions (GET /api/finance/wallet/transactions).
     * Input: from=2026-08-31; to=2026-08-01.
     * Steps:
     *   1. Prepare the fixture: Authenticated wallet owner exists.
     *   2. Use the input: from=2026-08-31; to=2026-08-01.
     *   3. Execute FinanceServiceImpl.getMyTransactions (GET /api/finance/wallet/transactions). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_012_RejectTransactionHistoryWhenDateRangeIsInvalid.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify filtered transaction query is never called.
     * Expected: The service returns the date-range validation message without querying transactions.
     * Pre-conditions: Authenticated wallet owner exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-012: Reject wallet transaction history when the start date is after the end date.")
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

    /**
     * Test Case: IT-WLT-013
     * Title: Complete a withdrawal only after the outgoing SePay webhook is matched.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.handleSepayOutgoingWebhook (POST /api/finance/webhooks/sepay/out).
     * Input: Outgoing amount 100000; transfer text contains WITHDRAW-ABC; external id SEPAY-OUT-987.
     * Steps:
     *   1. Prepare the fixture: Pending withdrawal and pending withdrawal transaction match amount/time/reference.
     *   2. Use the input: Outgoing amount 100000; transfer text contains WITHDRAW-ABC; external id SEPAY-OUT-987.
     *   3. Execute FinanceServiceImpl.handleSepayOutgoingWebhook (POST /api/finance/webhooks/sepay/out). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_013_CompleteWithdrawalOnlyAfterOutgoingSepayWebhook.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert webhook response/statuses and wallet release.
     * Expected: Transaction WITHDRAW-ABC and request 15 become SUCCESS/COMPLETED, locked funds are released and external id is stored.
     * Pre-conditions: Pending withdrawal and pending withdrawal transaction match amount/time/reference.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-013: Complete a withdrawal only after the outgoing SePay webhook is matched.")
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

    /**
     * Test Case: IT-WLT-014
     * Title: Move the withdrawal amount from available to frozen balance and save a pending transaction.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals).
     * Input: amount=150000; paymentMethodId=3.
     * Steps:
     *   1. Prepare the fixture: Wallet has enough available balance and an active payout method.
     *   2. Use the input: amount=150000; paymentMethodId=3.
     *   3. Execute FinanceServiceImpl.createWithdrawal (POST /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_014_CreateWithdrawalLocksWalletBalanceAndStoresPendingTransactionAmount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert balances, transaction type/status/amount and withdrawal status.
     * Expected: The wallet balances change to 100000 available/200000 frozen and a PENDING WITHDRAWAL transaction for 150000 is saved.
     * Pre-conditions: Wallet has enough available balance and an active payout method.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-014: Move the withdrawal amount from available to frozen balance and save a pending transaction.")
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

    /**
     * Test Case: IT-WLT-015
     * Title: Filter wallet history by transaction type and date range.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.getMyTransactions (GET /api/finance/wallet/transactions).
     * Input: type=deposit; from=2026-08-01; to=2026-08-31; page=0; size=20.
     * Steps:
     *   1. Prepare the fixture: Wallet owner has a successful DEPOSIT on 2026-08-20.
     *   2. Use the input: type=deposit; from=2026-08-01; to=2026-08-31; page=0; size=20.
     *   3. Execute FinanceServiceImpl.getMyTransactions (GET /api/finance/wallet/transactions). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_015_ListWalletTransactionHistoryWithTypeAndDateFilter.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert total/row type and verify repository receives normalized filters.
     * Expected: The response contains only the matching DEPOSIT transaction for the requested August range.
     * Pre-conditions: Wallet owner has a successful DEPOSIT on 2026-08-20.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-015: Filter wallet history by transaction type and date range.")
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

    /**
     * Test Case: IT-WLT-016
     * Title: Approve a withdrawal without marking the bank transfer complete.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.approveWithdrawal (POST /api/finance/withdrawals/{withdrawalId}/approve).
     * Input: withdrawalId=15.
     * Steps:
     *   1. Prepare the fixture: Admin reviews a pending withdrawal with a matching pending transaction.
     *   2. Use the input: withdrawalId=15.
     *   3. Execute FinanceServiceImpl.approveWithdrawal (POST /api/finance/withdrawals/{withdrawalId}/approve). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_016_AdminApprovalKeepsWithdrawalPendingUntilManualTransferIsConfirmed.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert statuses/description and verify releaseLockedFunds is not called.
     * Expected: Request 15 becomes APPROVED, its transaction remains PENDING with the SePay reconciliation description, and frozen funds remain locked.
     * Pre-conditions: Admin reviews a pending withdrawal with a matching pending transaction.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-016: Approve a withdrawal without marking the bank transfer complete.")
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

    /**
     * Test Case: IT-WLT-017
     * Title: Refund a stale pending withdrawal and cancel its transaction.
     * Procedure: Prepare the stated fixture and input, then execute PaymentReconciliationService.refundStaleWithdrawals (scheduled reconciliation).
     * Input: Reconciliation time 2026-08-31 10:00.
     * Steps:
     *   1. Prepare the fixture: Pending withdrawal and transaction are 49 hours old.
     *   2. Use the input: Reconciliation time 2026-08-31 10:00.
     *   3. Execute PaymentReconciliationService.refundStaleWithdrawals (scheduled reconciliation). Mapped test: com.tcs.module.finance.service.impl.Report52PaymentReconciliationITTest#IT_WLT_017_RefundStalePendingWithdrawalAndCancelTransaction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert changed count/statuses and wallet refund reference.
     * Expected: A pending withdrawal older than 48 hours becomes REJECTED, its transaction CANCELLED and the locked amount is refunded.
     * Pre-conditions: Pending withdrawal and transaction are 49 hours old.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-017: Refund a stale pending withdrawal and cancel its transaction.")
    void IT_WLT_017_RefundStalePendingWithdrawalAndCancelTransaction() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        BigDecimal amount = new BigDecimal("100000.00");
        PaymentMethod method = savedPaymentMethod();
        WithdrawalRequest withdrawal = pendingWithdrawal(11L, method, amount, now.minusHours(49));
        PaymentTransaction transaction = pendingWithdrawalTransaction(method, amount, "WITHDRAW-ABC", withdrawal.getRequestedAt());

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

    /**
     * Test Case: IT-WLT-018
     * Title: Cancel an expired pending wallet top-up session.
     * Procedure: Prepare the stated fixture and input, then execute PaymentReconciliationService.expirePendingTopups (scheduled reconciliation).
     * Input: Current time 2026-08-31 10:00.
     * Steps:
     *   1. Prepare the fixture: Top-up transaction TOPUP-ABC is pending and 20 minutes old.
     *   2. Use the input: Current time 2026-08-31 10:00.
     *   3. Execute PaymentReconciliationService.expirePendingTopups (scheduled reconciliation). Mapped test: com.tcs.module.finance.service.impl.Report52PaymentReconciliationITTest#IT_WLT_018_CancelExpiredWalletTopupSession.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert count/status/timestamp/reason and saveAll.
     * Expected: An old pending DEPOSIT becomes CANCELLED with processedAt set to the reconciliation time and a failure reason.
     * Pre-conditions: Top-up transaction TOPUP-ABC is pending and 20 minutes old.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-018: Cancel an expired pending wallet top-up session.")
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

    /**
     * Test Case: IT-WLT-019
     * Title: Send withdrawal-approval notification to the wallet owner.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.approveWithdrawal (POST /api/finance/withdrawals/{withdrawalId}/approve).
     * Input: withdrawalId=15.
     * Steps:
     *   1. Prepare the fixture: Admin approval matches a pending withdrawal and transaction.
     *   2. Use the input: withdrawalId=15.
     *   3. Execute FinanceServiceImpl.approveWithdrawal (POST /api/finance/withdrawals/{withdrawalId}/approve). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_019_WithdrawalApprovalNotificationTargetsWalletOwner.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification recipient/text/reference.
     * Expected: The wallet owner receives “Yêu cầu rút tiền đã được duyệt” with reference type WITHDRAWAL_REQUEST and id 15.
     * Pre-conditions: Admin approval matches a pending withdrawal and transaction.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-019: Send withdrawal-approval notification to the wallet owner.")
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

    /**
     * Test Case: IT-WLT-020
     * Title: Require the earning profile checks before creating a wallet.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createMyWallet (POST /api/finance/wallet).
     * Input: POST with no body.
     * Steps:
     *   1. Prepare the fixture: Authenticated tutor/center has an eligible profile and no wallet.
     *   2. Use the input: POST with no body.
     *   3. Execute FinanceServiceImpl.createMyWallet (POST /api/finance/wallet). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_WLT_020_CreateWalletChecksPenaltyAccessBeforeOpeningBalance.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert wallet id and verify role, penalty access and wallet creation order.
     * Expected: The role guard and withdrawal-eligibility check run before WalletService.create, and the response belongs to user 7.
     * Pre-conditions: Authenticated tutor/center has an eligible profile and no wallet.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-WLT-020: Require the earning profile checks before creating a wallet.")
    void IT_WLT_020_CreateWalletChecksPenaltyAccessBeforeOpeningBalance() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(walletService.create(USER_ID)).thenReturn(wallet);

        var response = financeService.createMyWallet();

        assertEquals(USER_ID, response.getWalletId());
        verify(authHelper).requireRole(UserRole.TUTOR, UserRole.TUTOR_CENTER);
        verify(penaltyAccessService).requireFeature(USER_ID, "WITHDRAWAL");
        verify(walletService).create(USER_ID);
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

    private PaymentTransaction transaction(
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            BigDecimal amount,
            String referenceCode,
            LocalDateTime createdAt) {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setCreatedAt(createdAt);
        return transaction;
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
