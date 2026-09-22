package com.tcs.module.finance.service.impl;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.CreateRefundRequest;
import com.tcs.module.finance.dto.request.CreateWithdrawalRequest;
import com.tcs.module.finance.dto.request.DepositRequest;
import com.tcs.module.finance.dto.request.ExecuteRefundRequest;
import com.tcs.module.finance.dto.request.PaymentMethodRequest;
import com.tcs.module.finance.dto.request.RefundDecisionRequest;
import com.tcs.module.finance.dto.request.SepayWebhookRequest;
import com.tcs.module.finance.dto.response.AdminWithdrawalPageResponse;
import com.tcs.module.finance.dto.response.PaymentMethodResponse;
import com.tcs.module.finance.dto.response.PaymentWebhookResponse;
import com.tcs.module.finance.dto.response.RefundExecutionResponse;
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
import com.tcs.module.finance.service.impl.EscrowServiceImpl;
import com.tcs.module.finance.service.impl.FinanceServiceImpl;
import com.tcs.module.finance.service.impl.SettlementServiceImpl;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52SettlementRefundITTest {

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
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private SystemParameterRepository systemParameterRepository;

    @Mock private EscrowService escrowService;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @InjectMocks
    private FinanceServiceImpl financeService;

    private EscrowServiceImpl escrowServiceImpl;

    private Wallet wallet;

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

        refundRequestCaptor = ArgumentCaptor.forClass(RefundRequest.class);

        escrowServiceImpl = new EscrowServiceImpl(
                walletService,
                paymentTransactionRepository,
                escrowTransactionRepository,
                refundRequestRepository,
                userRepository,
                classAssignmentRepository,
                classStudentRepository,
                paymentNotificationService,
                platformAdminRepository,
                systemParameterRepository
        );

        ReflectionTestUtils.setField(financeService, "directDepositEnabled", true);
        ReflectionTestUtils.setField(financeService, "simulateTopupEnabled", true);
    }

    /**
     * Test Case: IT-SET-001
     * Title: Execute an admin partial refund for a private termination and terminate the contract.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: release=200000; refund=300000; valid reason/payout.
     * Steps:
     *   1. Prepare the fixture: Admin has a disputed private escrow 10, approved termination and complete client payout.
     *   2. Use the input: release=200000; refund=300000; valid reason/payout.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_001_AdminPartialRefundDecisionCreatesRefundRecordAndTerminatesPrivateContract.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture ReleaseInstruction and assert all final statuses.
     * Expected: Escrow 10 is split 200000 release/300000 refund; refund completes, termination completes, assignment/contract terminate and processedAt is set.
     * Pre-conditions: Admin has a disputed private escrow 10, approved termination and complete client payout.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-001: Execute an admin partial refund for a private termination and terminate the contract.")
    void IT_SET_001_AdminPartialRefundDecisionCreatesRefundRecordAndTerminatesPrivateContract() {
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(7L);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        EscrowTransaction escrow = disputedPrivateEscrow(10L, new BigDecimal("500000.00"), assignment);
        User admin = adminUser();
        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setTerminationId(20L);
        termination.setAssignment(assignment);
        termination.setStatus(ClassTerminationStatus.APPROVED);
        Contract contract = new Contract();
        contract.setContractId(30L);
        contract.setAssignment(assignment);
        contract.setStatus(ContractStatus.ACTIVE);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin hoàn tiền theo số buổi còn lại");

        when(authHelper.currentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest saved = invocation.getArgument(0);
            if (saved.getRefundId() == null) {
                saved.setRefundId(99L);
            }
            return saved;
        });
        when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(termination));
        when(contractRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.of(contract));

        RefundExecutionResponse response = settlementService.executeRefund(request);

        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verify(escrowService).apply(instructionCaptor.capture());
        assertEquals(10L, instructionCaptor.getValue().escrowId());
        assertEquals(new BigDecimal("200000.00"), instructionCaptor.getValue().releaseToBeneficiary());
        assertEquals(new BigDecimal("300000.00"), instructionCaptor.getValue().refundToPayer());
        assertEquals(RefundRequestStatus.COMPLETED, response.getRefundStatus());
        assertEquals(ClassTerminationStatus.COMPLETED, termination.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        assertNotNull(response.getProcessedAt());
    }

    /**
     * Test Case: IT-SET-002
     * Title: Show a pending refund transfer in the admin withdrawal queue.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.getAdminWithdrawals (GET /api/finance/withdrawals).
     * Input: page=0; size=10; no status filter.
     * Steps:
     *   1. Prepare the fixture: An approved refund request and matching pending REFUND transaction exist.
     *   2. Use the input: page=0; size=10; no status filter.
     *   3. Execute FinanceServiceImpl.getAdminWithdrawals (GET /api/finance/withdrawals). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_002_ShowRefundTransferInAdminWithdrawalQueue.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert row type, bank, holder and transaction status.
     * Expected: The queue includes a REFUND row with TPBank, holder Nguyen Thu Ha and PENDING transaction status.
     * Pre-conditions: An approved refund request and matching pending REFUND transaction exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-002: Show a pending refund transfer in the admin withdrawal queue.")
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

    /**
     * Test Case: IT-SET-003
     * Title: Reject generic settlement execution without an instruction.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.execute (POST /api/finance/settlements/execute).
     * Input: instruction=null.
     * Steps:
     *   1. Prepare the fixture: Settlement endpoint receives a null body/instruction.
     *   2. Use the input: instruction=null.
     *   3. Execute SettlementServiceImpl.execute (POST /api/finance/settlements/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_003_GenericSettlementExecuteRejectsMissingInstruction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no settlement collaborator interaction.
     * Expected: The service returns “Thiếu chỉ dẫn tất toán” and EscrowService is not called.
     * Pre-conditions: Settlement endpoint receives a null body/instruction.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-003: Reject generic settlement execution without an instruction.")
    void IT_SET_003_GenericSettlementExecuteRejectsMissingInstruction() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.execute(null));

        assertEquals("Thiếu chỉ dẫn tất toán", exception.getMessage());
        verifyNoInteractions(escrowService);
    }

    /**
     * Test Case: IT-SET-004
     * Title: Reject a refund decision without complete client payout details.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: release=200000; refund=300000; incomplete TPBank payout.
     * Steps:
     *   1. Prepare the fixture: Disputed escrow 10 exists; payout account holder is blank.
     *   2. Use the input: release=200000; refund=300000; incomplete TPBank payout.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_004_RejectRefundDecisionWithoutCompleteClientPayout.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify EscrowService.apply is not called.
     * Expected: The service returns the payout validation error and does not apply the escrow split.
     * Pre-conditions: Disputed escrow 10 exists; payout account holder is blank.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-004: Reject a refund decision without complete client payout details.")
    void IT_SET_004_RejectRefundDecisionWithoutCompleteClientPayout() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin chưa có đủ tài khoản nhận hoàn");
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", ""));

        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Vui lòng nhập đầy đủ thông tin tài khoản nhận hoàn tiền", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    /**
     * Test Case: IT-SET-005
     * Title: Reject a refund decision whose amounts do not equal the escrow total.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: release=300000; refund=100000.
     * Steps:
     *   1. Prepare the fixture: Disputed escrow 10 totals 500000 and admin access is valid.
     *   2. Use the input: release=300000; refund=100000.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_005_RejectRefundDecisionWhenAmountsDoNotMatchEscrowTotal.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert mismatch error and verify no escrow apply.
     * Expected: A 300000 release plus 100000 refund for a 500000 escrow is rejected before wallet movement.
     * Pre-conditions: Disputed escrow 10 totals 500000 and admin access is valid.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-005: Reject a refund decision whose amounts do not equal the escrow total.")
    void IT_SET_005_RejectRefundDecisionWhenAmountsDoNotMatchEscrowTotal() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("300000.00"),
                new BigDecimal("100000.00"),
                "Admin nhập sai tổng tiền chia");

        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Tổng tiền giải ngân và hoàn tiền phải bằng số tiền escrow", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    /**
     * Test Case: IT-SET-006
     * Title: Block unauthenticated refund approval before changing state.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.approveRefundRequest (POST /api/finance/refund-requests/{refundId}/approve).
     * Input: refundId=77; valid decision.
     * Steps:
     *   1. Prepare the fixture: No authenticated admin/center principal.
     *   2. Use the input: refundId=77; valid decision.
     *   3. Execute FinanceServiceImpl.approveRefundRequest (POST /api/finance/refund-requests/{refundId}/approve). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_006_UnauthenticatedRefundApprovalIsBlockedBeforeStateChange.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify no save/apply.
     * Expected: The request returns “Yêu cầu đăng nhập”; refund and escrow repositories are not loaded.
     * Pre-conditions: No authenticated admin/center principal.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-006: Block unauthenticated refund approval before changing state.")
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

    /**
     * Test Case: IT-SET-007
     * Title: Prevent a center from approving a refund for a private escrow outside its class.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.approveRefundRequest (POST /api/finance/refund-requests/{refundId}/approve).
     * Input: release=300000; reason “Không đúng lớp trung tâm”.
     * Steps:
     *   1. Prepare the fixture: Refund 77 belongs to a private escrow not managed by the current center.
     *   2. Use the input: release=300000; reason “Không đúng lớp trung tâm”.
     *   3. Execute FinanceServiceImpl.approveRefundRequest (POST /api/finance/refund-requests/{refundId}/approve). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_007_CenterCannotApproveRefundForPrivateEscrowOutsideItsClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert permission error and verify no save/apply.
     * Expected: The center receives the ownership error and no refund/escrow state changes.
     * Pre-conditions: Refund 77 belongs to a private escrow not managed by the current center.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-007: Prevent a center from approving a refund for a private escrow outside its class.")
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

    /**
     * Test Case: IT-SET-008
     * Title: Reject a second pending refund request for the same escrow.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createRefundRequest (POST /api/finance/refund-requests).
     * Input: escrowId=10; amount=300000.
     * Steps:
     *   1. Prepare the fixture: Escrow 10 already has a PENDING refund request.
     *   2. Use the input: escrowId=10; amount=300000.
     *   3. Execute FinanceServiceImpl.createRefundRequest (POST /api/finance/refund-requests). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_008_RejectDuplicatePendingRefundForSameEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify no refund save/hold.
     * Expected: The duplicate-pending message is returned and the escrow is not held again.
     * Pre-conditions: Escrow 10 already has a PENDING refund request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-008: Reject a second pending refund request for the same escrow.")
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

    /**
     * Test Case: IT-SET-009
     * Title: Filter admin refund requests by PENDING status.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.getAdminRefundRequests (GET /api/finance/refund-requests?status=PENDING).
     * Input: status=PENDING.
     * Steps:
     *   1. Prepare the fixture: At least one pending refund request exists.
     *   2. Use the input: status=PENDING.
     *   3. Execute FinanceServiceImpl.getAdminRefundRequests (GET /api/finance/refund-requests?status=PENDING). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_009_AdminRefundRequestStatusFilterReturnsPendingRowsOnly.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert count/id/status and role guard.
     * Expected: Only refund request 77 with PENDING status is returned.
     * Pre-conditions: At least one pending refund request exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-009: Filter admin refund requests by PENDING status.")
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

    /**
     * Test Case: IT-SET-010
     * Title: Create a refund request, hold escrow and notify admins and requester.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.createRefundRequest (POST /api/finance/refund-requests).
     * Input: escrowId=10; refund amount 300000; reason.
     * Steps:
     *   1. Prepare the fixture: Client owns funded escrow 10 and no pending refund exists; an admin is active.
     *   2. Use the input: escrowId=10; refund amount 300000; reason.
     *   3. Execute FinanceServiceImpl.createRefundRequest (POST /api/finance/refund-requests). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_010_CreateRefundRequestHoldsEscrowAndNotifiesAdminAndRequester.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response/statuses and capture both notifications.
     * Expected: Refund 77 is PENDING, escrow 10 is ON_HOLD and both admin/requester notifications use REFUND_REQUEST reference.
     * Pre-conditions: Client owns funded escrow 10 and no pending refund exists; an admin is active.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-010: Create a refund request, hold escrow and notify admins and requester.")
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

    /**
     * Test Case: IT-SET-011
     * Title: Reject a refund decision for an escrow that is already released.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: release=200000; refund=300000.
     * Steps:
     *   1. Prepare the fixture: Escrow 10 has status RELEASED.
     *   2. Use the input: release=200000; refund=300000.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_011_RejectRefundDecisionWhenEscrowWasAlreadyReleased.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no EscrowService.apply.
     * Expected: The service returns the already-settled error and does not apply another refund.
     * Pre-conditions: Escrow 10 has status RELEASED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-011: Reject a refund decision for an escrow that is already released.")
    void IT_SET_011_RejectRefundDecisionWhenEscrowWasAlreadyReleased() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.RELEASED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin thử hoàn tiền sau tất toán");

        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Escrow đã được tất toán, không thể hoàn tiền thêm", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    /**
     * Test Case: IT-SET-012
     * Title: Reject a negative release amount in a refund decision.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: release=-1; refund=300000.
     * Steps:
     *   1. Prepare the fixture: Admin request passes authentication and includes a valid escrow selector.
     *   2. Use the input: release=-1; refund=300000.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_012_RejectRefundDecisionWhenReleaseAmountIsNegative.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no settlement call.
     * Expected: The service returns the non-negative amount error and does not touch escrow or wallets.
     * Pre-conditions: Admin request passes authentication and includes a valid escrow selector.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-012: Reject a negative release amount in a refund decision.")
    void IT_SET_012_RejectRefundDecisionWhenReleaseAmountIsNegative() {
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("-1.00"),
                new BigDecimal("300000.00"),
                "Admin nhập số tiền âm");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Số tiền giải ngân/hoàn không được âm", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    /**
     * Test Case: IT-SET-013
     * Title: Complete a refund transfer after the outgoing SePay webhook.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.handleSepayOutgoingWebhook (POST /api/finance/webhooks/sepay/out).
     * Input: Outgoing amount 30000; text contains REFUND-ESCROW-10; external id SEPAY-OUT-990.
     * Steps:
     *   1. Prepare the fixture: Approved refund request and matching pending REFUND transaction exist.
     *   2. Use the input: Outgoing amount 30000; text contains REFUND-ESCROW-10; external id SEPAY-OUT-990.
     *   3. Execute FinanceServiceImpl.handleSepayOutgoingWebhook (POST /api/finance/webhooks/sepay/out). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_013_CompleteRefundTransferAfterOutgoingSepayWebhook.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert webhook response/statuses/reference and notification.
     * Expected: REFUND-ESCROW-10 transaction becomes SUCCESS, refund 77 becomes COMPLETED and the requester is notified.
     * Pre-conditions: Approved refund request and matching pending REFUND transaction exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-013: Complete a refund transfer after the outgoing SePay webhook.")
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

    /**
     * Test Case: IT-SET-014
     * Title: Reject a refund decision with a reason shorter than ten characters.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: Valid split with reason shorter than 10 characters.
     * Steps:
     *   1. Prepare the fixture: A valid disputed escrow exists.
     *   2. Use the input: Valid split with reason shorter than 10 characters.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_014_RejectRefundDecisionWhenReasonIsTooShort.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no EscrowService.apply.
     * Expected: The service returns the minimum-reason message and performs no settlement.
     * Pre-conditions: A valid disputed escrow exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-014: Reject a refund decision with a reason shorter than ten characters.")
    void IT_SET_014_RejectRefundDecisionWhenReasonIsTooShort() {
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "ngắn");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Lý do hoàn tiền phải có ít nhất 10 ký tự", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    /**
     * Test Case: IT-SET-015
     * Title: Reject refund execution when the current admin account cannot be loaded.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: Valid disputed escrow and split.
     * Steps:
     *   1. Prepare the fixture: Authentication returns admin id 1 but UserRepository has no user row.
     *   2. Use the input: Valid disputed escrow and split.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_015_RejectRefundDecisionWhenAdminAccountCannotBeLoaded.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert not-found error and verify no settlement call.
     * Expected: The service returns “Không tìm thấy quản trị viên” before applying the decision.
     * Pre-conditions: Authentication returns admin id 1 but UserRepository has no user row.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-015: Reject refund execution when the current admin account cannot be loaded.")
    void IT_SET_015_RejectRefundDecisionWhenAdminAccountCannotBeLoaded() {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(10L);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);
        ExecuteRefundRequest request = refundRequest(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin duyệt hoàn tiền hợp lệ");

        when(authHelper.currentUserId()).thenReturn(1L);
        when(escrowTransactionRepository.findById(10L)).thenReturn(Optional.of(escrow));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> settlementService.executeRefund(request));

        assertEquals("Không tìm thấy quản trị viên", exception.getMessage());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verifyNoInteractions(escrowService);
    }

    /**
     * Test Case: IT-SET-016
     * Title: Create a pending refund transfer for a direct-bank-paid escrow after applying the split.
     * Procedure: Prepare the stated fixture and input, then execute EscrowServiceImpl.apply (settlement instruction).
     * Input: ReleaseInstruction release=480000; refund=720000.
     * Steps:
     *   1. Prepare the fixture: Escrow 22 was paid directly by bank and contains complete client payout data.
     *   2. Use the input: ReleaseInstruction release=480000; refund=720000.
     *   3. Execute EscrowServiceImpl.apply (settlement instruction). Mapped test: com.tcs.module.finance.service.impl.Report52EscrowServiceITTest#IT_SET_016_CreateRefundTransferRequestForDirectBankPaidEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert tutor credit, refund request fields/status/reference and final escrow status.
     * Expected: Escrow 22 is released with 480000 to the tutor, a PENDING refund request for 720000 is created with reference REFUND-ESCROW-22 and no locked-fund refund API is used.
     * Pre-conditions: Escrow 22 was paid directly by bank and contains complete client payout data.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-016: Create a pending refund transfer for a direct-bank-paid escrow after applying the split.")
    void IT_SET_016_CreateRefundTransferRequestForDirectBankPaidEscrow() {
        BigDecimal releaseAmount = new BigDecimal("480000.00");
        BigDecimal refundAmount = new BigDecimal("720000.00");
        EscrowTransaction escrow = fundedPrivateEscrowPaidThroughQr(22L, new BigDecimal("1200000.00"));
        Wallet systemEscrowWallet = wallet(999L);
        Wallet tutorWallet = wallet(TUTOR_USER_ID);

        when(escrowTransactionRepository.findById(22L)).thenReturn(Optional.of(escrow));
        when(walletService.getOrCreate(TUTOR_USER_ID)).thenReturn(tutorWallet);
        when(walletService.getSystemEscrowWallet()).thenReturn(systemEscrowWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refundRequestRepository.findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(22L))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        escrowServiceImpl.apply(new ReleaseInstruction(
                22L,
                releaseAmount,
                refundAmount,
                "Admin chia tiền chấm dứt sớm",
                new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A")));

        verify(walletService, never()).releaseLockedFunds(any(), any(), any());
        verify(walletService, never()).refundLockedFunds(any(), any(), any());
        verify(walletService).credit(TUTOR_USER_ID, releaseAmount, "ESCROW_RELEASE-22");
        verify(refundRequestRepository).save(refundRequestCaptor.capture());
        RefundRequest refundRequest = refundRequestCaptor.getValue();
        assertSame(escrow, refundRequest.getEscrowTransaction());
        assertEquals(CLIENT_USER_ID, refundRequest.getRequestedBy().getUserId());
        assertEquals(refundAmount, refundRequest.getAmount());
        assertEquals(RefundRequestStatus.APPROVED, refundRequest.getStatus());
        assertEquals("PENDING", refundRequest.getTransferStatus());
        assertEquals("REFUND-ESCROW-22", refundRequest.getRefundReferenceCode());
        assertEquals(EscrowStatus.RELEASED, escrow.getStatus());
    }

    /**
     * Test Case: IT-SET-017
     * Title: Reject a center-request fee refund and restore its held fee status.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.rejectRefundRequest (POST /api/finance/refund-requests/{refundId}/reject).
     * Input: Rejection reason “Trung tâm đã xử lý lại yêu cầu”.
     * Steps:
     *   1. Prepare the fixture: Center-fee refund 77 is pending and its hold is REFUND_REQUESTED.
     *   2. Use the input: Rejection reason “Trung tâm đã xử lý lại yêu cầu”.
     *   3. Execute FinanceServiceImpl.rejectRefundRequest (POST /api/finance/refund-requests/{refundId}/reject). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_017_RejectCenterRequestFeeRefundRestoresHeldFeeHold.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert refund/hold statuses and notification.
     * Expected: Refund 77 becomes REJECTED, the fee hold returns to HELD and the requester is notified.
     * Pre-conditions: Center-fee refund 77 is pending and its hold is REFUND_REQUESTED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-017: Reject a center-request fee refund and restore its held fee status.")
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

    /**
     * Test Case: IT-SET-018
     * Title: Forward a generic release instruction to the escrow settlement service.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.execute (POST /api/finance/settlements/execute).
     * Input: escrowId=10; release=200000; refund=300000; valid reason.
     * Steps:
     *   1. Prepare the fixture: Settlement service has an instruction object.
     *   2. Use the input: escrowId=10; release=200000; refund=300000; valid reason.
     *   3. Execute SettlementServiceImpl.execute (POST /api/finance/settlements/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_018_GenericSettlementExecuteDelegatesReleaseInstructionToEscrowService.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture/verify the same instruction object.
     * Expected: The exact ReleaseInstruction object is passed to EscrowService.apply.
     * Pre-conditions: Settlement service has an instruction object.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-018: Forward a generic release instruction to the escrow settlement service.")
    void IT_SET_018_GenericSettlementExecuteDelegatesReleaseInstructionToEscrowService() {
        ReleaseInstruction instruction = new ReleaseInstruction(
                10L,
                new BigDecimal("200000.00"),
                new BigDecimal("300000.00"),
                "Admin tất toán theo quyết định");

        settlementService.execute(instruction);

        verify(escrowService).apply(instruction);
    }

    /**
     * Test Case: IT-SET-019
     * Title: Approve a center-request fee refund and create a pending admin transfer.
     * Procedure: Prepare the stated fixture and input, then execute FinanceServiceImpl.approveRefundRequest (POST /api/finance/refund-requests/{refundId}/approve).
     * Input: refund amount 30000; reason “Duyệt hoàn phí trung tâm”.
     * Steps:
     *   1. Prepare the fixture: Center-fee refund 77 is REFUND_REQUESTED and the system wallet exists.
     *   2. Use the input: refund amount 30000; reason “Duyệt hoàn phí trung tâm”.
     *   3. Execute FinanceServiceImpl.approveRefundRequest (POST /api/finance/refund-requests/{refundId}/approve). Mapped test: com.tcs.module.finance.service.impl.Report52FinanceServiceITTest#IT_SET_019_ApproveCenterRequestFeeRefundCreatesPendingAdminTransfer.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert refund status/transfer status and captured transaction fields.
     * Expected: Refund 77 becomes APPROVED, its transfer remains PENDING and a PENDING REFUND transaction REFUND-CREQFEE-60 is saved.
     * Pre-conditions: Center-fee refund 77 is REFUND_REQUESTED and the system wallet exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-019: Approve a center-request fee refund and create a pending admin transfer.")
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

    /**
     * Test Case: IT-SET-020
     * Title: Apply a partial refund to a center student and terminate that enrollment contract.
     * Procedure: Prepare the stated fixture and input, then execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute).
     * Input: release=240000; refund=360000.
     * Steps:
     *   1. Prepare the fixture: Admin has a disputed center-student escrow, approved termination and complete payout.
     *   2. Use the input: release=240000; refund=360000.
     *   3. Execute SettlementServiceImpl.executeRefund (POST /api/finance/refunds/execute). Mapped test: com.tcs.module.finance.service.impl.Report52SettlementServiceITTest#IT_SET_020_AdminPartialRefundDecisionDropsCenterEnrollmentAndTerminatesCenterContract.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert release instruction and all enrollment/contract/refund statuses.
     * Expected: Escrow 11 is split 240000/360000; refund completes, class student becomes DROPPED, termination COMPLETED and contract TERMINATED.
     * Pre-conditions: Admin has a disputed center-student escrow, approved termination and complete payout.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-SET-020: Apply a partial refund to a center student and terminate that enrollment contract.")
    void IT_SET_020_AdminPartialRefundDecisionDropsCenterEnrollmentAndTerminatesCenterContract() {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(9L);
        classStudent.setStatus(ClassStudentStatus.ENROLLED);
        EscrowTransaction escrow = disputedCenterEscrow(11L, new BigDecimal("600000.00"), classStudent);
        User admin = adminUser();
        ClassTerminationRequest termination = new ClassTerminationRequest();
        termination.setTerminationId(21L);
        termination.setClassStudent(classStudent);
        termination.setStatus(ClassTerminationStatus.APPROVED);
        Contract contract = new Contract();
        contract.setContractId(31L);
        contract.setClassStudent(classStudent);
        contract.setStatus(ContractStatus.ACTIVE);
        ExecuteRefundRequest request = refundRequest(
                11L,
                new BigDecimal("240000.00"),
                new BigDecimal("360000.00"),
                "Admin hoàn tiền cho học viên lớp trung tâm");

        when(authHelper.currentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(escrowTransactionRepository.findById(11L)).thenReturn(Optional.of(escrow));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classTerminationRequestRepository.findFirstByClassStudent_ClassStudentIdOrderByCreatedAtDesc(9L))
                .thenReturn(Optional.of(termination));
        when(contractRepository.findByClassStudent_ClassStudentId(9L)).thenReturn(Optional.of(contract));

        RefundExecutionResponse response = settlementService.executeRefund(request);

        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
        verify(escrowService).apply(any(ReleaseInstruction.class));
        assertEquals(RefundRequestStatus.COMPLETED, response.getRefundStatus());
        assertEquals(ClassTerminationStatus.COMPLETED, termination.getStatus());
        assertEquals(ClassStudentStatus.DROPPED, classStudent.getStatus());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
    }

    

    private ExecuteRefundRequest refundRequest(
            Long escrowId,
            BigDecimal releaseAmount,
            BigDecimal refundAmount,
            String reason) {

        ExecuteRefundRequest request = new ExecuteRefundRequest();
        request.setEscrowId(escrowId);
        request.setReleaseToBeneficiary(releaseAmount);
        request.setRefundToPayer(refundAmount);
        request.setReason(reason);
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));
        return request;
    }

    

    private EscrowTransaction disputedPrivateEscrow(Long escrowId, BigDecimal amount, ClassAssignment assignment) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setAssignment(assignment);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.DISPUTED);
        return escrow;
    }

    

    private EscrowTransaction disputedCenterEscrow(Long escrowId, BigDecimal amount, ClassStudent classStudent) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setClassStudent(classStudent);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.DISPUTED);
        return escrow;
    }

    

    private User adminUser() {
        User admin = new User();
        admin.setUserId(1L);
        admin.setEmail("admin.it@tcs.test");
        return admin;
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

    

    private RefundDecisionRequest refundDecision(BigDecimal approvedAmount, String reason) {
        RefundDecisionRequest request = new RefundDecisionRequest();
        request.setApprovedAmount(approvedAmount);
        request.setReason(reason);
        return request;
    }

    

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPasswordHash("hash");
        return user;
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

    

    private EscrowTransaction privateFundedEscrow(Long escrowId, BigDecimal amount) {
        PaymentTransaction payment = pendingEscrowPayment("ESCROW-" + escrowId, amount);
        payment.setWallet(wallet);
        EscrowTransaction escrow = privateEscrow(escrowId, payment, amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
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

    

    private SepayWebhookRequest outgoingWebhook(Long id, BigDecimal amount, String content) {
        SepayWebhookRequest request = new SepayWebhookRequest();
        request.setId(id);
        request.setTransferType("out");
        request.setTransferAmount(amount);
        request.setContent(content);
        request.setAccountNumber("02660559201");
        return request;
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

    

    private Wallet wallet(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(userId);
        wallet.setAvailableBalance(new BigDecimal("1000000.00"));
        wallet.setFrozenBalance(BigDecimal.ZERO);
        return wallet;
    }
}
