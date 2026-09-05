package com.tcs.module.finance.service.impl;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.ReleaseInstruction;
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
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52EscrowPaymentITTest {

    private static final Long USER_ID = 7L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;
    private static final Long CENTER_USER_ID = 33L;

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
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private PaymentNotificationService paymentNotificationService;
    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private SystemParameterRepository systemParameterRepository;

    @Spy
    @InjectMocks
    private EscrowServiceImpl escrowService;

    @InjectMocks
    private FinanceServiceImpl financeService;

    private PaymentReconciliationService reconciliationService;

    private Wallet wallet;

    private ArgumentCaptor<PaymentTransaction> paymentCaptor;
    private ArgumentCaptor<EscrowTransaction> escrowCaptor;
    private ArgumentCaptor<RefundRequest> refundRequestCaptor;

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

        SystemParameter defaultFee = new SystemParameter();
        defaultFee.setParamValue("0.00");
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(defaultFee));

        Wallet platformWallet = new Wallet();
        platformWallet.setWalletId(999L);
        User platformUser = new User();
        platformUser.setUserId(999L);
        platformWallet.setUser(platformUser);
        when(walletService.getSystemEscrowWallet()).thenReturn(platformWallet);

        ReflectionTestUtils.setField(financeService, "escrowService", escrowService);

        paymentCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        escrowCaptor = ArgumentCaptor.forClass(EscrowTransaction.class);
        refundRequestCaptor = ArgumentCaptor.forClass(RefundRequest.class);
        reconciliationService = new PaymentReconciliationService(
                paymentTransactionRepository,
                withdrawalRequestRepository,
                walletService,
                paymentNotificationService);
    }

    /**
     * Test Case: IT-ESC-001
     * Title: Fund an escrow from an incoming payment webhook and publish class activation.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.handleSepayWebhook -> EscrowServiceImpl.fundConfirmedPayment (POST /api/finance/webhooks/sepay).
     * Input: SePay payload external id 456 and reference ESCROW-A7.
     * Steps:
     *   1. Prepare the fixture: A pending escrow payment matches the incoming amount/reference and is not a center-request fee.
     *   2. Use the input: SePay payload external id 456 and reference ESCROW-A7.
     *   3. Execute FinanceServiceImpl.handleSepayWebhook -> EscrowServiceImpl.fundConfirmedPayment (POST /api/finance/webhooks/sepay). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_ESC_001_FundEscrowAndPublishClassActivationEventAfterPaymentWebhook.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert webhook response/payment/escrow status and event.
     * Expected: A matched payment becomes SUCCESS, escrow A7 becomes FUNDED and an EscrowFunded event is published.
     * Pre-conditions: A pending escrow payment matches the incoming amount/reference and is not a center-request fee.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-001: Fund an escrow from an incoming payment webhook and publish class activation.")
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
        doAnswer(invocation -> {
            escrow.setStatus(EscrowStatus.FUNDED);
            return escrow;
        }).when(escrowService).fundConfirmedPayment(tx);

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

    /**
     * Test Case: IT-ESC-002
     * Title: Prepare a pending QR payment for a private assignment escrow.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.preparePayment (called after contract signing).
     * Input: EscrowLockCommand payer/client, amount 500000, assignmentId=7.
     * Steps:
     *   1. Prepare the fixture: Assignment 7 exists, has no escrow and has no existing payment reference.
     *   2. Use the input: EscrowLockCommand payer/client, amount 500000, assignmentId=7.
     *   3. Execute EscrowServiceImpl.preparePayment (called after contract signing). Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_002_PreparePrivateAssignmentQrPaymentTransaction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture payment and assert wallet/type/status/amount/reference; no escrow row yet.
     * Expected: One ESCROW_DEPOSIT transaction is saved in the system escrow wallet with PENDING status, amount and reference ESCROW-A7.
     * Pre-conditions: Assignment 7 exists, has no escrow and has no existing payment reference.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-002: Prepare a pending QR payment for a private assignment escrow.")
    void IT_ESC_002_PreparePrivateAssignmentQrPaymentTransaction() {
        BigDecimal amount = new BigDecimal("500000.00");
        Wallet systemEscrowWallet = wallet(999L);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of());
        when(paymentTransactionRepository.findEscrowReferenceFamily("ESCROW-A7")).thenReturn(List.of());
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, 7L, null));

        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        PaymentTransaction savedPayment = paymentCaptor.getValue();
        assertSame(savedPayment, result);
        assertSame(systemEscrowWallet, savedPayment.getWallet());
        assertEquals(PaymentTransactionType.ESCROW_DEPOSIT, savedPayment.getType());
        assertEquals(PaymentTransactionStatus.PENDING, savedPayment.getStatus());
        assertEquals(amount, savedPayment.getAmount());
        assertEquals("ESCROW-A7", savedPayment.getReferenceCode());
        verify(walletService, never()).lockFunds(any(), any(), any());
        verify(escrowTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-003
     * Title: Prepare a pending QR payment for a center student escrow.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.preparePayment (called after student contract signing).
     * Input: EscrowLockCommand payer/client, amount 600000, classStudentId=9.
     * Steps:
     *   1. Prepare the fixture: Class student 9 has no escrow and no prior payment reference.
     *   2. Use the input: EscrowLockCommand payer/client, amount 600000, classStudentId=9.
     *   3. Execute EscrowServiceImpl.preparePayment (called after student contract signing). Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_003_PrepareCenterStudentQrPaymentTransaction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert transaction type/status/amount/reference and no duplicate escrow.
     * Expected: One ESCROW_DEPOSIT transaction is saved in the system escrow wallet with classStudentId=9 and reference ESCROW-CS9.
     * Pre-conditions: Class student 9 has no escrow and no prior payment reference.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-003: Prepare a pending QR payment for a center student escrow.")
    void IT_ESC_003_PrepareCenterStudentQrPaymentTransaction() {
        BigDecimal amount = new BigDecimal("600000.00");
        Wallet systemEscrowWallet = wallet(999L);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);

        when(escrowTransactionRepository.findByClassStudent_ClassStudentId(9L)).thenReturn(Optional.empty());
        when(classStudentRepository.findById(9L)).thenReturn(Optional.of(classStudent));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-CS9",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of());
        when(paymentTransactionRepository.findEscrowReferenceFamily("ESCROW-CS9")).thenReturn(List.of());
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, null, 9L));

        assertSame(systemEscrowWallet, result.getWallet());
        assertEquals(PaymentTransactionType.ESCROW_DEPOSIT, result.getType());
        assertEquals(PaymentTransactionStatus.PENDING, result.getStatus());
        assertEquals(amount, result.getAmount());
        assertEquals("ESCROW-CS9", result.getReferenceCode());
        verify(escrowTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-004
     * Title: Reject an escrow command that does not select exactly one target.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.preparePayment.
     * Input: Command with both assignmentId and classStudentId, or with neither.
     * Steps:
     *   1. Prepare the fixture: Escrow preparation is called with invalid selectors.
     *   2. Use the input: Command with both assignmentId and classStudentId, or with neither.
     *   3. Execute EscrowServiceImpl.preparePayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_004_RejectEscrowCommandWhenTargetSelectorIsInvalid.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify payment save is never called.
     * Expected: The service returns the selector error and does not save a payment.
     * Pre-conditions: Escrow preparation is called with invalid selectors.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-004: Reject an escrow command that does not select exactly one target.")
    void IT_ESC_004_RejectEscrowCommandWhenTargetSelectorIsInvalid() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.preparePayment(new EscrowLockCommand(
                        CLIENT_USER_ID,
                        new BigDecimal("500000.00"),
                        7L,
                        9L)));

        assertEquals("Escrow phải gắn đúng một trong assignmentId hoặc classStudentId", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-005
     * Title: Prevent a RELEASED escrow from being put on hold for a dispute.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.holdForDispute.
     * Input: escrowId=5; reason “Yêu cầu đến sau khi đã giải ngân”.
     * Steps:
     *   1. Prepare the fixture: Escrow 5 has status RELEASED.
     *   2. Use the input: escrowId=5; reason “Yêu cầu đến sau khi đã giải ngân”.
     *   3. Execute EscrowServiceImpl.holdForDispute. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_005_RejectHoldingReleasedEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify no wallet settlement call.
     * Expected: The service returns the already-settled message and does not release/refund wallet funds.
     * Pre-conditions: Escrow 5 has status RELEASED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-005: Prevent a RELEASED escrow from being put on hold for a dispute.")
    void IT_ESC_005_RejectHoldingReleasedEscrow() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.RELEASED);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.holdForDispute(5L, "Yêu cầu đến sau khi đã giải ngân"));

        assertEquals("Escrow đã tất toán nên không thể chuyển sang tranh chấp", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
    }

    /**
     * Test Case: IT-ESC-006
     * Title: Require a successful payment before locking/funding an escrow.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.lock.
     * Input: Valid assignment escrow lock command.
     * Steps:
     *   1. Prepare the fixture: The payment-reference family has no SUCCESS transaction.
     *   2. Use the input: Valid assignment escrow lock command.
     *   3. Execute EscrowServiceImpl.lock. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_006_LockEscrowRequiresSuccessfulPaymentBeforeFunding.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify escrow save is never called.
     * Expected: Without a successful matching payment, the service returns “Chưa có giao dịch thanh toán escrow” and saves no escrow.
     * Pre-conditions: The payment-reference family has no SUCCESS transaction.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-006: Require a successful payment before locking/funding an escrow.")
    void IT_ESC_006_LockEscrowRequiresSuccessfulPaymentBeforeFunding() {
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.SUCCESS))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.lock(new EscrowLockCommand(
                        CLIENT_USER_ID,
                        new BigDecimal("500000.00"),
                        7L,
                        null)));

        assertEquals("Chưa có giao dịch thanh toán escrow", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-007
     * Title: Reject funding when the matched payment is still pending.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.fundConfirmedPayment.
     * Input: Pending ESCROW_DEPOSIT payment.
     * Steps:
     *   1. Prepare the fixture: Payment transaction has status PENDING.
     *   2. Use the input: Pending ESCROW_DEPOSIT payment.
     *   3. Execute EscrowServiceImpl.fundConfirmedPayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_007_FundConfirmedPaymentRejectsPendingTransaction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify escrow save is never called.
     * Expected: The service returns the successful-payment prerequisite error and creates no escrow.
     * Pre-conditions: Payment transaction has status PENDING.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-007: Reject funding when the matched payment is still pending.")
    void IT_ESC_007_FundConfirmedPaymentRejectsPendingTransaction() {
        PaymentTransaction pendingPayment = successfulEscrowPayment(88L, "ESCROW-A7", new BigDecimal("500000.00"));
        pendingPayment.setStatus(PaymentTransactionStatus.PENDING);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.fundConfirmedPayment(pendingPayment));

        assertEquals("Chỉ giao dịch đã thanh toán thành công mới sinh escrow", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-008
     * Title: Do not create a duplicate escrow when the same successful payment is replayed.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.fundConfirmedPayment.
     * Input: Replay the same successful payment webhook.
     * Steps:
     *   1. Prepare the fixture: Payment 88 is SUCCESS and already linked to an escrow.
     *   2. Use the input: Replay the same successful payment webhook.
     *   3. Execute EscrowServiceImpl.fundConfirmedPayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_008_PreventDuplicateEscrowForSuccessfulPaymentWebhook.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert same escrow instance and verify no save.
     * Expected: The existing escrow is returned and no second escrow or class lookup is performed.
     * Pre-conditions: Payment 88 is SUCCESS and already linked to an escrow.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-008: Do not create a duplicate escrow when the same successful payment is replayed.")
    void IT_ESC_008_PreventDuplicateEscrowForSuccessfulPaymentWebhook() {
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(88L, "ESCROW-A7", new BigDecimal("500000.00"));
        EscrowTransaction existingEscrow = new EscrowTransaction();
        existingEscrow.setEscrowId(100L);
        existingEscrow.setStatus(EscrowStatus.FUNDED);

        when(escrowTransactionRepository.findByPayment_TransactionId(88L)).thenReturn(Optional.of(existingEscrow));

        EscrowTransaction result = escrowService.fundConfirmedPayment(paidEscrowPayment);

        assertSame(existingEscrow, result);
        verify(escrowTransactionRepository, never()).save(any());
        verify(classAssignmentRepository, never()).findById(any());
    }

    /**
     * Test Case: IT-ESC-009
     * Title: Reuse an existing pending payment reference when preparing escrow payment again.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.preparePayment.
     * Input: Same assignment lock command as the first attempt.
     * Steps:
     *   1. Prepare the fixture: Assignment has no funded escrow but its reference family contains a pending payment.
     *   2. Use the input: Same assignment lock command as the first attempt.
     *   3. Execute EscrowServiceImpl.preparePayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_009_PreparePaymentReusesExistingPendingEscrowReference.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert same payment and verify payment save is never called.
     * Expected: The existing PENDING payment is returned without inserting another payment transaction.
     * Pre-conditions: Assignment has no funded escrow but its reference family contains a pending payment.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-009: Reuse an existing pending payment reference when preparing escrow payment again.")
    void IT_ESC_009_PreparePaymentReusesExistingPendingEscrowReference() {
        BigDecimal amount = new BigDecimal("500000.00");
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        PaymentTransaction pendingPayment = successfulEscrowPayment(55L, "ESCROW-A7", amount);
        pendingPayment.setStatus(PaymentTransactionStatus.PENDING);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of(pendingPayment));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, 7L, null));

        assertSame(pendingPayment, result);
        verify(paymentTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-010
     * Title: Record release, platform fee and platform income as separate transactions.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.apply (settlement instruction).
     * Input: ReleaseInstruction escrow 15, release 500000, refund 0.
     * Steps:
     *   1. Prepare the fixture: Escrow 15 is funded; platform fee parameter is 0.10; tutor and system wallets exist.
     *   2. Use the input: ReleaseInstruction escrow 15, release 500000, refund 0.
     *   3. Execute EscrowServiceImpl.apply (settlement instruction). Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_010_DeductConfiguredPlatformFeeAsSeparateTransactions.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Verify wallet credit/debit, three saved transaction types and fee description.
     * Expected: A 500000 release, 50000 platform fee and 50000 platform income are recorded as distinct transaction types with the configured 10% rate.
     * Pre-conditions: Escrow 15 is funded; platform fee parameter is 0.10; tutor and system wallets exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-010: Record release, platform fee and platform income as separate transactions.")
    void IT_ESC_010_DeductConfiguredPlatformFeeAsSeparateTransactions() {
        BigDecimal escrowAmount = new BigDecimal("500000.00");
        EscrowTransaction escrow = fundedPrivateEscrow(15L, escrowAmount);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);
        Wallet platformWallet = wallet(99L);
        User platformUser = new User();
        platformUser.setUserId(99L);
        platformWallet.setUser(platformUser);
        SystemParameter parameter = new SystemParameter();
        parameter.setParamValue("0.10");

        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(parameter));
        when(escrowTransactionRepository.findById(15L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.getSystemEscrowWallet()).thenReturn(platformWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        escrowService.apply(new ReleaseInstruction(15L, escrowAmount, BigDecimal.ZERO, "Hoàn thành lớp"));

        verify(walletService).credit(TUTOR_USER_ID, escrowAmount, "ESCROW_RELEASE-15");
        verify(walletService).debit(TUTOR_USER_ID, new BigDecimal("50000.00"), "PLATFORM_FEE-15");
        verify(walletService).credit(99L, new BigDecimal("50000.00"), "PLATFORM_FEE-INCOME-15");
        verify(paymentTransactionRepository, times(3)).save(paymentCaptor.capture());
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, paymentCaptor.getAllValues().get(0).getType());
        assertEquals(PaymentTransactionType.PLATFORM_FEE, paymentCaptor.getAllValues().get(1).getType());
        assertEquals(PaymentTransactionType.DEPOSIT, paymentCaptor.getAllValues().get(2).getType());
        assertTrue(paymentCaptor.getAllValues().get(1).getDescription().contains("10%"));
    }

    /**
     * Test Case: IT-ESC-011
     * Title: Apply a release/refund split to the payer and beneficiary wallets.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.apply (settlement instruction).
     * Input: ReleaseInstruction release 400000, refund 100000.
     * Steps:
     *   1. Prepare the fixture: Escrow 5 is funded and both wallet operations are available.
     *   2. Use the input: ReleaseInstruction release 400000, refund 100000.
     *   3. Execute EscrowServiceImpl.apply (settlement instruction). Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_011_ApplySettlementSplitsReleaseAndRefundToClientWallet.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Verify wallet calls, transaction types and final escrow status.
     * Expected: For escrow 5, 400000 is released to the tutor, 100000 is refunded to the client and escrow becomes RELEASED.
     * Pre-conditions: Escrow 5 is funded and both wallet operations are available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-011: Apply a release/refund split to the payer and beneficiary wallets.")
    void IT_ESC_011_ApplySettlementSplitsReleaseAndRefundToClientWallet() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        Wallet payerWallet = payerWallet();
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.refundLockedFunds(CLIENT_USER_ID, new BigDecimal("100000.00"), "REFUND-ESCROW-5"))
                .thenReturn(payerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        escrowService.apply(new ReleaseInstruction(
                5L,
                new BigDecimal("400000.00"),
                new BigDecimal("100000.00"),
                "Admin chia tiền sau chấm dứt sớm"));

        verify(walletService).releaseLockedFunds(CLIENT_USER_ID, new BigDecimal("400000.00"), "ESCROW_RELEASE-5");
        verify(walletService).credit(TUTOR_USER_ID, new BigDecimal("400000.00"), "ESCROW_RELEASE-5");
        verify(walletService).refundLockedFunds(CLIENT_USER_ID, new BigDecimal("100000.00"), "REFUND-ESCROW-5");
        verify(paymentTransactionRepository, times(2)).save(paymentCaptor.capture());
        assertEquals(PaymentTransactionType.ESCROW_RELEASE, paymentCaptor.getAllValues().get(0).getType());
        assertEquals(PaymentTransactionType.REFUND, paymentCaptor.getAllValues().get(1).getType());
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    /**
     * Test Case: IT-ESC-012
     * Title: Reject a settlement whose release and refund do not equal the escrow amount.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.apply (settlement instruction).
     * Input: Release 400000 plus refund 50000.
     * Steps:
     *   1. Prepare the fixture: Escrow 5 is funded for 500000.
     *   2. Use the input: Release 400000 plus refund 50000.
     *   3. Execute EscrowServiceImpl.apply (settlement instruction). Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_012_RejectSettlementWhenReleaseAndRefundDoNotEqualEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify release/refund calls are never made.
     * Expected: The service returns the total-mismatch error and performs no wallet movement.
     * Pre-conditions: Escrow 5 is funded for 500000.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-012: Reject a settlement whose release and refund do not equal the escrow amount.")
    void IT_ESC_012_RejectSettlementWhenReleaseAndRefundDoNotEqualEscrow() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.apply(new ReleaseInstruction(
                        5L,
                        new BigDecimal("400000.00"),
                        BigDecimal.ZERO,
                        "Sai tổng chia tiền")));

        assertEquals("Tổng tiền giải ngân/hoàn phải bằng số tiền escrow", exception.getMessage());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
    }

    /**
     * Test Case: IT-ESC-013
     * Title: Treat a repeated hold request for an already disputed escrow as idempotent.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.holdForDispute.
     * Input: escrowId=5; reason “Tranh chấp đang xử lý”.
     * Steps:
     *   1. Prepare the fixture: Escrow 5 already has status DISPUTED.
     *   2. Use the input: escrowId=5; reason “Tranh chấp đang xử lý”.
     *   3. Execute EscrowServiceImpl.holdForDispute. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_013_HoldAlreadyDisputedEscrowIsIdempotent.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert same object/status and verify no save.
     * Expected: The same DISPUTED escrow is returned without another save or wallet movement.
     * Pre-conditions: Escrow 5 already has status DISPUTED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-013: Treat a repeated hold request for an already disputed escrow as idempotent.")
    void IT_ESC_013_HoldAlreadyDisputedEscrowIsIdempotent() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));

        EscrowTransaction result = escrowService.holdForDispute(5L, "Tranh chấp đang xử lý");

        assertSame(escrow, result);
        assertEquals(EscrowStatus.DISPUTED, result.getStatus());
        verify(escrowTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-014
     * Title: Create a FUNDED escrow for a confirmed center-student payment.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.fundConfirmedPayment.
     * Input: Confirmed center-student payment.
     * Steps:
     *   1. Prepare the fixture: Payment 89 is SUCCESS with a valid ESCROW-CS9 reference and no existing escrow.
     *   2. Use the input: Confirmed center-student payment.
     *   3. Execute EscrowServiceImpl.fundConfirmedPayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_014_FundConfirmedCenterPaymentCreatesFundedStudentEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture escrow and assert student/payment/status/amount.
     * Expected: The new escrow links class student 9 and payment 89, has status FUNDED and preserves the paid amount.
     * Pre-conditions: Payment 89 is SUCCESS with a valid ESCROW-CS9 reference and no existing escrow.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-014: Create a FUNDED escrow for a confirmed center-student payment.")
    void IT_ESC_014_FundConfirmedCenterPaymentCreatesFundedStudentEscrow() {
        BigDecimal amount = new BigDecimal("600000.00");
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(89L, "ESCROW-CS9", amount);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);

        when(escrowTransactionRepository.findByPayment_TransactionId(89L)).thenReturn(Optional.empty());
        when(classStudentRepository.findById(9L)).thenReturn(Optional.of(classStudent));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.fundConfirmedPayment(paidEscrowPayment);

        verify(escrowTransactionRepository).save(escrowCaptor.capture());
        EscrowTransaction savedEscrow = escrowCaptor.getValue();
        assertSame(savedEscrow, result);
        assertSame(classStudent, savedEscrow.getClassStudent());
        assertSame(paidEscrowPayment, savedEscrow.getPayment());
        assertEquals(EscrowStatus.FUNDED, savedEscrow.getStatus());
        assertEquals(amount, savedEscrow.getAmount());
    }

    /**
     * Test Case: IT-ESC-015
     * Title: Reject a confirmed payment whose escrow reference cannot identify a target.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.fundConfirmedPayment.
     * Input: Confirmed payment 90 with malformed reference.
     * Steps:
     *   1. Prepare the fixture: Payment is SUCCESS but its reference is malformed.
     *   2. Use the input: Confirmed payment 90 with malformed reference.
     *   3. Execute EscrowServiceImpl.fundConfirmedPayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_015_RejectConfirmedPaymentWhenEscrowReferenceIsMalformed.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify escrow save is never called.
     * Expected: The service returns the unknown-target error and does not save an escrow.
     * Pre-conditions: Payment is SUCCESS but its reference is malformed.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-015: Reject a confirmed payment whose escrow reference cannot identify a target.")
    void IT_ESC_015_RejectConfirmedPaymentWhenEscrowReferenceIsMalformed() {
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(90L, "ESCROW-UNKNOWN", new BigDecimal("500000.00"));

        when(escrowTransactionRepository.findByPayment_TransactionId(90L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> escrowService.fundConfirmedPayment(paidEscrowPayment));

        assertEquals("Giao dịch thanh toán không xác định được đối tượng escrow", exception.getMessage());
        verify(escrowTransactionRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ESC-016
     * Title: Put funded escrow on hold when a dispute or termination starts.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.holdForDispute.
     * Input: escrowId=5; reason “Client báo sự cố lớp học”.
     * Steps:
     *   1. Prepare the fixture: Escrow 5 is FUNDED and associated with the class under review.
     *   2. Use the input: escrowId=5; reason “Client báo sự cố lớp học”.
     *   3. Execute EscrowServiceImpl.holdForDispute. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_016_HoldFundedEscrowWhenDisputeOrTerminationStarts.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert DISPUTED and verify no wallet settlement calls.
     * Expected: Funded escrow 5 changes to DISPUTED and no release/refund occurs while the case is open.
     * Pre-conditions: Escrow 5 is FUNDED and associated with the class under review.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-016: Put funded escrow on hold when a dispute or termination starts.")
    void IT_ESC_016_HoldFundedEscrowWhenDisputeOrTerminationStarts() {
        EscrowTransaction escrow = fundedPrivateEscrow(5L, new BigDecimal("500000.00"));

        when(escrowTransactionRepository.findById(5L)).thenReturn(Optional.of(escrow));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.holdForDispute(5L, "Client báo sự cố lớp học");

        assertSame(escrow, result);
        assertEquals(EscrowStatus.DISPUTED, escrow.getStatus());
        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
    }

    /**
     * Test Case: IT-ESC-017
     * Title: Create a new payment reference after a cancelled escrow payment session.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.preparePayment.
     * Input: Same assignment lock command.
     * Steps:
     *   1. Prepare the fixture: The previous payment session for assignment 7 is CANCELLED and no active pending payment remains.
     *   2. Use the input: Same assignment lock command.
     *   3. Execute EscrowServiceImpl.preparePayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_017_ReopenPaymentAfterCancelledEscrowSession.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert new reference/status and distinguish it from ESCROW-A7.
     * Expected: A new PENDING payment uses an ESCROW-A7 suffixed reference rather than reusing the cancelled base reference.
     * Pre-conditions: The previous payment session for assignment 7 is CANCELLED and no active pending payment remains.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-017: Create a new payment reference after a cancelled escrow payment session.")
    void IT_ESC_017_ReopenPaymentAfterCancelledEscrowSession() {
        BigDecimal amount = new BigDecimal("500000.00");
        Wallet systemEscrowWallet = wallet(999L);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        PaymentTransaction cancelledPayment = new PaymentTransaction();
        cancelledPayment.setReferenceCode("ESCROW-A7");
        cancelledPayment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        cancelledPayment.setStatus(PaymentTransactionStatus.CANCELLED);

        when(escrowTransactionRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(paymentTransactionRepository.findEscrowReferenceFamilyByTypeAndStatus(
                "ESCROW-A7",
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING))
                .thenReturn(List.of());
        when(paymentTransactionRepository.findEscrowReferenceFamily("ESCROW-A7"))
                .thenReturn(List.of(cancelledPayment));
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransaction result = escrowService.preparePayment(
                new EscrowLockCommand(CLIENT_USER_ID, amount, 7L, null));

        assertTrue(result.getReferenceCode().startsWith("ESCROW-A7-"));
        assertNotEquals("ESCROW-A7", result.getReferenceCode());
        assertEquals(PaymentTransactionStatus.PENDING, result.getStatus());
    }

    /**
     * Test Case: IT-ESC-018
     * Title: Cancel an expired pending class-escrow payment session.
     * Procedure: Prepare the stated fixture and input, then execute PaymentReconciliationService.expirePendingEscrowDeposits (scheduled reconciliation).
     * Input: Current time 2026-08-31 10:00.
     * Steps:
     *   1. Prepare the fixture: Escrow payment was created 20 minutes before the reconciliation time.
     *   2. Use the input: Current time 2026-08-31 10:00.
     *   3. Execute PaymentReconciliationService.expirePendingEscrowDeposits (scheduled reconciliation). Mapped test: com.tcs.module.finance.service.impl.Report52PaymentReconciliationITTest#IT_ESC_018_CancelExpiredClassEscrowPaymentSession.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert changed count, cancelled status, timestamp/reason and saveAll.
     * Expected: An old pending ESCROW_DEPOSIT becomes CANCELLED with processedAt and a failure reason.
     * Pre-conditions: Escrow payment was created 20 minutes before the reconciliation time.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-018: Cancel an expired pending class-escrow payment session.")
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

    /**
     * Test Case: IT-ESC-019
     * Title: Refund a funded escrow paid from the client wallet.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.refund.
     * Input: escrowId=19; reason “Admin hoàn toàn bộ do hủy lớp”.
     * Steps:
     *   1. Prepare the fixture: Escrow 19 is funded and its payer wallet can receive the refund.
     *   2. Use the input: escrowId=19; reason “Admin hoàn toàn bộ do hủy lớp”.
     *   3. Execute EscrowServiceImpl.refund. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_019_RefundFundedWalletPaidEscrowMarksEscrowRefunded.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert escrow status, wallet refund reference and transaction type.
     * Expected: Escrow 19 becomes REFUNDED, the client locked funds are restored and a REFUND transaction is saved.
     * Pre-conditions: Escrow 19 is funded and its payer wallet can receive the refund.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-019: Refund a funded escrow paid from the client wallet.")
    void IT_ESC_019_RefundFundedWalletPaidEscrowMarksEscrowRefunded() {
        EscrowTransaction escrow = fundedPrivateEscrow(19L, new BigDecimal("500000.00"));
        Wallet payerWallet = payerWallet();

        when(escrowTransactionRepository.findById(19L)).thenReturn(Optional.of(escrow));
        when(walletService.refundLockedFunds(CLIENT_USER_ID, escrow.getAmount(), "REFUND-ESCROW-19"))
                .thenReturn(payerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.refund(19L, "Admin hoàn toàn bộ do hủy lớp");

        assertSame(escrow, result);
        assertEquals(EscrowStatus.REFUNDED, escrow.getStatus());
        verify(walletService).refundLockedFunds(CLIENT_USER_ID, escrow.getAmount(), "REFUND-ESCROW-19");
        verify(paymentTransactionRepository).save(paymentCaptor.capture());
        assertEquals(PaymentTransactionType.REFUND, paymentCaptor.getValue().getType());
    }

    /**
     * Test Case: IT-ESC-020
     * Title: Fund a private assignment escrow after its payment is confirmed.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.fundConfirmedPayment.
     * Input: Confirmed private payment.
     * Steps:
     *   1. Prepare the fixture: Payment 88 is SUCCESS with a valid private-assignment reference and no escrow exists.
     *   2. Use the input: Confirmed private payment.
     *   3. Execute EscrowServiceImpl.fundConfirmedPayment. Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_ESC_020_FundConfirmedPrivatePaymentCreatesFundedEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture escrow and assert assignment/payment/status/amount.
     * Expected: The new escrow links assignment 7 and payment 88, has status FUNDED and amount equal to the payment.
     * Pre-conditions: Payment 88 is SUCCESS with a valid private-assignment reference and no escrow exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ESC-020: Fund a private assignment escrow after its payment is confirmed.")
    void IT_ESC_020_FundConfirmedPrivatePaymentCreatesFundedEscrow() {
        BigDecimal amount = new BigDecimal("500000.00");
        PaymentTransaction paidEscrowPayment = successfulEscrowPayment(88L, "ESCROW-A7", amount);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);

        when(escrowTransactionRepository.findByPayment_TransactionId(88L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowTransaction result = escrowService.fundConfirmedPayment(paidEscrowPayment);

        verify(escrowTransactionRepository).save(escrowCaptor.capture());
        EscrowTransaction savedEscrow = escrowCaptor.getValue();
        assertSame(savedEscrow, result);
        assertSame(assignment, savedEscrow.getAssignment());
        assertSame(paidEscrowPayment, savedEscrow.getPayment());
        assertEquals(EscrowStatus.FUNDED, savedEscrow.getStatus());
        assertEquals(amount, savedEscrow.getAmount());
    }



    private PaymentTransaction successfulEscrowPayment(Long transactionId, String reference, BigDecimal amount) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(transactionId);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setAmount(amount);
        payment.setReferenceCode(reference);
        return payment;
    }

    private Wallet payerWallet() {
        Wallet wallet = wallet(CLIENT_USER_ID);
        wallet.setFrozenBalance(new BigDecimal("1000000.00"));
        return wallet;
    }

    private Wallet wallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(userId);
        wallet.setAvailableBalance(new BigDecimal("1000000.00"));
        wallet.setFrozenBalance(BigDecimal.ZERO);
        return wallet;
    }

    private EscrowTransaction fundedPrivateEscrow(Long escrowId, BigDecimal amount) {
        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        Tutor tutor = new Tutor();
        tutor.setUser(tutorUser);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setTutor(tutor);

        EscrowTransaction escrow = fundedEscrow(escrowId, amount);
        escrow.setAssignment(assignment);
        return escrow;
    }

    private EscrowTransaction fundedPrivateEscrowPaidThroughQr(Long escrowId, BigDecimal amount) {
        User payer = new User();
        payer.setUserId(CLIENT_USER_ID);
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(payer);
        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);

        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        Tutor tutor = new Tutor();
        tutor.setUser(tutorUser);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setTutor(tutor);
        assignment.setApplication(application);

        PaymentTransaction payment = new PaymentTransaction();
        payment.setWallet(wallet(999L));
        payment.setAmount(amount);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setPayment(payment);
        escrow.setAssignment(assignment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
    }

    private EscrowTransaction fundedEscrow(Long escrowId, BigDecimal amount) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setWallet(payerWallet());
        payment.setAmount(amount);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);

        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setPayment(payment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
    }

    @SuppressWarnings("unused")
    private EscrowTransaction fundedCenterEscrow(Long escrowId, BigDecimal amount) {
        User centerUser = new User();
        centerUser.setUserId(CENTER_USER_ID);
        TutorCenter center = new TutorCenter();
        center.setUser(centerUser);
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCenter(center);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);
        classStudent.setTutoringClass(tutoringClass);

        EscrowTransaction escrow = fundedEscrow(escrowId, amount);
        escrow.setClassStudent(classStudent);
        return escrow;
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

    

    private SepayWebhookRequest incomingWebhook(Long id, BigDecimal amount, String content) {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(id);
        request.setTransferType("in");
        request.setTransferAmount(amount);
        request.setContent(content);
        request.setAccountNumber("02660559201");
        return request;
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
}
