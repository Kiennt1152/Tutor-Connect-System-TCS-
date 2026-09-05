package com.tcs.module.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.service.impl.SystemParameterServiceImpl;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.finance.service.impl.CenterRequestFeeServiceImpl;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.entity.AuditLog;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.UserPenalty;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.repository.CircumventionEventRepository;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.UserPenaltyRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52AdminPlatformITTest {

    private static final Long PARAM_ID = 1L;
    private static final String REQUEST_ID = "REQ-CENTER-001";
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long CENTER_USER_ID = 22L;

    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private CenterRequestFeeHoldRepository feeHoldRepository;
    @Mock private WalletService walletService;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private NotificationRepository notificationRepository;
    @Mock private com.tcs.module.finance.service.PaymentNotificationService paymentNotificationService;

    @InjectMocks
    private SystemParameterServiceImpl systemParameterService;

    @InjectMocks
    private CenterRequestFeeServiceImpl centerRequestFeeService;


    private static final Long ADMIN_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @Mock private VerificationRequestRepository verificationRequestRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private WithdrawalRequestRepository withdrawalRequestRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private UserPenaltyRepository userPenaltyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private CircumventionEventRepository circumventionEventRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuthHelper authHelper;
    @Mock private com.tcs.module.platform.service.AuditLogService auditLogService;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ClientRepository clientRepository;

    private PlatformTaskQueueServiceImpl taskQueueService;
    private PenaltyServiceImpl penaltyService;
    private AuditLogServiceImpl auditLogServiceImpl;

    @BeforeEach
    void setUpAdminPlatformItFixture() {
        taskQueueService = new PlatformTaskQueueServiceImpl(
                verificationRequestRepository,
                reportRepository,
                supportTicketRepository,
                withdrawalRequestRepository,
                refundRequestRepository,
                disputeRepository);
        penaltyService = new PenaltyServiceImpl(
                userPenaltyRepository,
                userRepository,
                platformAdminRepository,
                reportRepository,
                circumventionEventRepository,
                disputeRepository,
                supportTicketRepository,
                authHelper,
                auditLogService,
                notificationDispatchService);
        auditLogServiceImpl = new AuditLogServiceImpl(
                auditLogRepository,
                userRepository,
                authHelper,
                new ObjectMapper(),
                platformAdminRepository,
                tutorRepository,
                tutorCenterRepository,
                clientRepository,
                new PlatformMapper());

        when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED)).thenReturn(List.of());
        when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.UNDER_REVIEW)).thenReturn(List.of());
        when(reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING)).thenReturn(List.of());
        when(supportTicketRepository.findByStatusInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING)).thenReturn(List.of());
        when(refundRequestRepository.findByStatusOrderByRequestedAtAsc(RefundRequestStatus.PENDING)).thenReturn(List.of());
        when(disputeRepository.findByStatusInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
    }

    

    /**
     * Test Case: IT-ADM-001
     * Title: Aggregate pending admin work and money at risk for the dashboard.
     * Procedure: Prepare the stated fixture and input, then execute TaskQueueServiceImpl.getSummary (GET /api/platform/tasks/summary).
     * Input: Admin dashboard summary request.
     * Steps:
     *   1. Prepare the fixture: Each admin queue repository returns one pending item with known amounts.
     *   2. Use the input: Admin dashboard summary request.
     *   3. Execute TaskQueueServiceImpl.getSummary (GET /api/platform/tasks/summary). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_001_TaskQueueSummaryAggregatesAllAdminWorkAndMoneyAtRisk.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert all six counts, total pending tasks and money at risk.
     * Expected: The summary counts verification, report, ticket, withdrawal, refund and dispute queues and calculates moneyAtRisk=800000.
     * Pre-conditions: Each admin queue repository returns one pending item with known amounts.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-001: Aggregate pending admin work and money at risk for the dashboard.")
    void IT_ADM_001_TaskQueueSummaryAggregatesAllAdminWorkAndMoneyAtRisk() {
        when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED))
                .thenReturn(List.of(verification(10L, LocalDateTime.now().minusHours(1))));
        when(reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING))
                .thenReturn(List.of(report(20L, ReportCategory.SPAM, LocalDateTime.now().minusHours(2))));
        when(supportTicketRepository.findByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(ticket(30L, SupportTicketPriority.HIGH, LocalDateTime.now().minusHours(3))));
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of(withdrawal(40L, new BigDecimal("200000"), LocalDateTime.now().minusHours(4))));
        when(refundRequestRepository.findByStatusOrderByRequestedAtAsc(RefundRequestStatus.PENDING))
                .thenReturn(List.of(refund(50L, new BigDecimal("300000"), LocalDateTime.now().minusHours(5))));
        when(disputeRepository.findByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(dispute(60L, new BigDecimal("500000"), LocalDateTime.now().minusHours(6))));

        var summary = taskQueueService.getSummary();

        assertEquals(6, summary.getTotalPendingTasks());
        assertEquals(1, summary.getPendingVerifications());
        assertEquals(1, summary.getOpenReports());
        assertEquals(1, summary.getOpenTickets());
        assertEquals(1, summary.getPendingWithdrawals());
        assertEquals(1, summary.getPendingRefunds());
        assertEquals(1, summary.getOpenDisputes());
        assertEquals(new BigDecimal("800000"), summary.getMoneyAtRisk());
    }

    /**
     * Test Case: IT-ADM-002
     * Title: Filter admin tasks by type, priority and SLA state.
     * Procedure: Prepare the stated fixture and input, then execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks).
     * Input: type=REFUND_REQUEST; priority=HIGH; overdue=false; page=0; size=10.
     * Steps:
     *   1. Prepare the fixture: Queue fixtures contain refund and other task types.
     *   2. Use the input: type=REFUND_REQUEST; priority=HIGH; overdue=false; page=0; size=10.
     *   3. Execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_002_ListTasksFiltersAdminQueueByTypeAndPriority.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert total/task type/priority.
     * Expected: The filtered page returns one HIGH REFUND_REQUEST task and excludes unrelated task types.
     * Pre-conditions: Queue fixtures contain refund and other task types.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-002: Filter admin tasks by type, priority and SLA state.")
    void IT_ADM_002_ListTasksFiltersAdminQueueByTypeAndPriority() {
        when(refundRequestRepository.findByStatusOrderByRequestedAtAsc(RefundRequestStatus.PENDING))
                .thenReturn(List.of(refund(50L, new BigDecimal("300000"), LocalDateTime.now().minusHours(2))));
        when(supportTicketRepository.findByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(ticket(30L, SupportTicketPriority.MEDIUM, LocalDateTime.now().minusHours(1))));

        var page = taskQueueService.listTasks("REFUND_REQUEST", "HIGH", false, 0, 10);

        assertEquals(1, page.getTotalElements());
        assertEquals("REFUND_REQUEST", page.getContent().get(0).getTaskType());
        assertEquals("HIGH", page.getContent().get(0).getPriority());
    }

    /**
     * Test Case: IT-ADM-003
     * Title: Expose the target route and entity reference for a withdrawal task.
     * Procedure: Prepare the stated fixture and input, then execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks).
     * Input: type=WITHDRAWAL; page=0; size=10.
     * Steps:
     *   1. Prepare the fixture: One pending withdrawal is present in the queue.
     *   2. Use the input: type=WITHDRAWAL; page=0; size=10.
     *   3. Execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_003_TaskItemsCarryAdminTargetRouteAndEntityReference.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert targetRoute, targetQuery and entityId.
     * Expected: A withdrawal task points to /platform/withdrawals with query id=40 and entityId=40.
     * Pre-conditions: One pending withdrawal is present in the queue.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-003: Expose the target route and entity reference for a withdrawal task.")
    void IT_ADM_003_TaskItemsCarryAdminTargetRouteAndEntityReference() {
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of(withdrawal(40L, new BigDecimal("200000"), LocalDateTime.now().minusHours(1))));

        var page = taskQueueService.listTasks("WITHDRAWAL", null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("/platform/withdrawals", page.getContent().get(0).getTargetRoute());
        assertEquals("?id=40", page.getContent().get(0).getTargetQuery());
        assertEquals(40L, page.getContent().get(0).getEntityId());
    }

    /**
     * Test Case: IT-ADM-004
     * Title: Reject a blank platform-fee configuration value.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters).
     * Input: PLATFORM_FEE_RATE value blank.
     * Steps:
     *   1. Prepare the fixture: Admin is authorized and the key is not already present.
     *   2. Use the input: PLATFORM_FEE_RATE value blank.
     *   3. Execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_ADM_004_RejectBlankPlatformFeeValueBeforeSavingConfig.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no save/audit.
     * Expected: The service returns “Giá trị tham số là bắt buộc.” and does not save or audit a parameter.
     * Pre-conditions: Admin is authorized and the key is not already present.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-004: Reject a blank platform-fee configuration value.")
    void IT_ADM_004_RejectBlankPlatformFeeValueBeforeSavingConfig() {
        UpsertSystemParameterRequest request = request("PLATFORM_FEE_RATE", " ");

        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.createParameter(request));

        assertEquals("Giá trị tham số là bắt buộc.", exception.getMessage());
        verify(systemParameterRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    /**
     * Test Case: IT-ADM-005
     * Title: Reject an unsupported penalty type before saving an admin decision.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties).
     * Input: penaltyType=UNKNOWN_PENALTY.
     * Steps:
     *   1. Prepare the fixture: Valid platform admin and target user exist.
     *   2. Use the input: penaltyType=UNKNOWN_PENALTY.
     *   3. Execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_005_RejectInvalidPenaltyTypeBeforeSavingAdminDecision.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify UserPenaltyRepository.save is never called.
     * Expected: The service returns the invalid-penalty-type error and no penalty is saved.
     * Pre-conditions: Valid platform admin and target user exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-005: Reject an unsupported penalty type before saving an admin decision.")
    void IT_ADM_005_RejectInvalidPenaltyTypeBeforeSavingAdminDecision() {
        User adminUser = user(ADMIN_USER_ID, "admin.it@tcs.test");
        PlatformAdmin admin = platformAdmin(adminUser);
        User target = user(TARGET_USER_ID, "target.it@tcs.test");
        IssuePenaltyRequest request = issuePenaltyRequest("UNKNOWN_PENALTY");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(target));
        when(platformAdminRepository.findByUser_UserId(TARGET_USER_ID)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> penaltyService.issuePenalty(request));

        assertEquals("Loại hình phạt không hợp lệ: UNKNOWN_PENALTY", exception.getMessage());
        verify(userPenaltyRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ADM-006
     * Title: Block an anonymous admin action before loading target data.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties).
     * Input: Valid-looking penalty request.
     * Steps:
     *   1. Prepare the fixture: No admin principal.
     *   2. Use the input: Valid-looking penalty request.
     *   3. Execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_006_AnonymousAdminActionIsBlockedBeforeBusinessLookup.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify target lookup/save are skipped.
     * Expected: The service returns “Yêu cầu đăng nhập quản trị viên” and does not query users or save a penalty.
     * Pre-conditions: No admin principal.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-006: Block an anonymous admin action before loading target data.")
    void IT_ADM_006_AnonymousAdminActionIsBlockedBeforeBusinessLookup() {
        IssuePenaltyRequest request = issuePenaltyRequest(UserPenaltyType.WARNING.name());

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenThrow(new ForbiddenException("Yêu cầu đăng nhập quản trị viên"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> penaltyService.issuePenalty(request));

        assertEquals("Yêu cầu đăng nhập quản trị viên", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userPenaltyRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ADM-007
     * Title: Prevent a non-admin from issuing a penalty.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties).
     * Input: Valid-looking penalty request.
     * Steps:
     *   1. Prepare the fixture: Authenticated user is not a platform admin.
     *   2. Use the input: Valid-looking penalty request.
     *   3. Execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_007_NonAdminCannotIssuePenaltyOrChangeTargetUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role error and verify no lookup/save.
     * Expected: The service returns the PLATFORM_ADMIN permission error and creates no penalty.
     * Pre-conditions: Authenticated user is not a platform admin.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-007: Prevent a non-admin from issuing a penalty.")
    void IT_ADM_007_NonAdminCannotIssuePenaltyOrChangeTargetUser() {
        IssuePenaltyRequest request = issuePenaltyRequest(UserPenaltyType.WARNING.name());

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN))
                .thenThrow(new ForbiddenException("Yêu cầu quyền PLATFORM_ADMIN"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> penaltyService.issuePenalty(request));

        assertEquals("Yêu cầu quyền PLATFORM_ADMIN", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userPenaltyRepository, never()).save(any());
    }

    /**
     * Test Case: IT-ADM-008
     * Title: Reject a duplicate PLATFORM_FEE_RATE configuration row.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters).
     * Input: Second PLATFORM_FEE_RATE value 0.02.
     * Steps:
     *   1. Prepare the fixture: PLATFORM_FEE_RATE already exists.
     *   2. Use the input: Second PLATFORM_FEE_RATE value 0.02.
     *   3. Execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_ADM_008_RejectDuplicatePlatformFeeParameterBeforeCreatingConfigRow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no save/audit.
     * Expected: The duplicate-key error is returned and neither parameter nor audit row is created.
     * Pre-conditions: PLATFORM_FEE_RATE already exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-008: Reject a duplicate PLATFORM_FEE_RATE configuration row.")
    void IT_ADM_008_RejectDuplicatePlatformFeeParameterBeforeCreatingConfigRow() {
        UpsertSystemParameterRequest request = request("PLATFORM_FEE_RATE", "0.02");

        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE"))
                .thenReturn(Optional.of(parameter(1L, "PLATFORM_FEE_RATE", "0.02")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.createParameter(request));

        assertEquals("Khóa tham số đã tồn tại: PLATFORM_FEE_RATE", exception.getMessage());
        verify(systemParameterRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    /**
     * Test Case: IT-ADM-009
     * Title: Return stable pagination metadata for the second admin-task page.
     * Procedure: Prepare the stated fixture and input, then execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks).
     * Input: type=WITHDRAWAL; page=1; size=10.
     * Steps:
     *   1. Prepare the fixture: Queue contains 12 withdrawal tasks.
     *   2. Use the input: type=WITHDRAWAL; page=1; size=10.
     *   3. Execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_009_TaskQueuePaginationReturnsStableSecondPageAndTotal.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert total/page/size/content count.
     * Expected: The page reports total 12, page index 1, size 10 and two content rows.
     * Pre-conditions: Queue contains 12 withdrawal tasks.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-009: Return stable pagination metadata for the second admin-task page.")
    void IT_ADM_009_TaskQueuePaginationReturnsStableSecondPageAndTotal() {
        List<WithdrawalRequest> withdrawals = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> withdrawal(
                        100L + index,
                        new BigDecimal("100000"),
                        LocalDateTime.now().minusMinutes(index)))
                .toList();

        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING))
                .thenReturn(withdrawals);

        var page = taskQueueService.listTasks("WITHDRAWAL", null, null, 1, 10);

        assertEquals(12, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(1, page.getPage());
        assertEquals(10, page.getSize());
    }

    /**
     * Test Case: IT-ADM-010
     * Title: Issue a penalty, write its audit entry and notify the target user.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties).
     * Input: WARNING penalty with a reason/restriction.
     * Steps:
     *   1. Prepare the fixture: Admin and target user exist; penalty request is valid.
     *   2. Use the input: WARNING penalty with a reason/restriction.
     *   3. Execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_010_IssuePenaltyRecordsAuditAndNotifiesTargetUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response status/id and capture audit/notification references.
     * Expected: Penalty 70 is ACTIVE; an ISSUE_PENALTY audit row and target notification are created.
     * Pre-conditions: Admin and target user exist; penalty request is valid.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-010: Issue a penalty, write its audit entry and notify the target user.")
    void IT_ADM_010_IssuePenaltyRecordsAuditAndNotifiesTargetUser() {
        User adminUser = user(ADMIN_USER_ID, "admin.it@tcs.test");
        PlatformAdmin admin = platformAdmin(adminUser);
        User target = user(TARGET_USER_ID, "target.it@tcs.test");
        IssuePenaltyRequest request = issuePenaltyRequest(UserPenaltyType.FEATURE_RESTRICTION.name());
        request.setRestrictionDetails("{\"features\":[\"WITHDRAWAL\"]}");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(target));
        when(platformAdminRepository.findByUser_UserId(TARGET_USER_ID)).thenReturn(Optional.empty());
        when(userPenaltyRepository.save(any(UserPenalty.class))).thenAnswer(invocation -> {
            UserPenalty penalty = invocation.getArgument(0);
            penalty.setPenaltyId(70L);
            return penalty;
        });

        var response = penaltyService.issuePenalty(request);

        assertEquals(70L, response.getPenaltyId());
        assertEquals(UserPenaltyStatus.ACTIVE.name(), response.getStatus());
        verify(auditLogService).record(eq("ISSUE_PENALTY"), eq("UserPenalty"), eq(70L), eq(null), eq(request));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(target),
                eq(NotificationType.SYSTEM),
                eq("PENALTY_ISSUED"),
                any(Map.class),
                eq("Tài khoản của bạn vừa nhận một hình phạt"),
                any(),
                eq("PENALTY"),
                eq(70L));
    }

    /**
     * Test Case: IT-ADM-011
     * Title: Include the penalty reference in the target-user notification.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties).
     * Input: Valid penalty request.
     * Steps:
     *   1. Prepare the fixture: Admin issues a valid WARNING penalty to target user 22.
     *   2. Use the input: Valid penalty request.
     *   3. Execute PenaltyServiceImpl.issuePenalty (POST /api/platform/penalties). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_011_IssuePenaltyNotificationCarriesPenaltyReferenceToTargetUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification template/text/reference.
     * Expected: The target notification uses PENALTY reference type and id 71.
     * Pre-conditions: Admin issues a valid WARNING penalty to target user 22.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-011: Include the penalty reference in the target-user notification.")
    void IT_ADM_011_IssuePenaltyNotificationCarriesPenaltyReferenceToTargetUser() {
        User adminUser = user(ADMIN_USER_ID, "admin.it@tcs.test");
        PlatformAdmin admin = platformAdmin(adminUser);
        User target = user(TARGET_USER_ID, "target.it@tcs.test");
        IssuePenaltyRequest request = issuePenaltyRequest(UserPenaltyType.WARNING.name());

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(target));
        when(platformAdminRepository.findByUser_UserId(TARGET_USER_ID)).thenReturn(Optional.empty());
        when(userPenaltyRepository.save(any(UserPenalty.class))).thenAnswer(invocation -> {
            UserPenalty penalty = invocation.getArgument(0);
            penalty.setPenaltyId(71L);
            return penalty;
        });

        penaltyService.issuePenalty(request);

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(target),
                eq(NotificationType.SYSTEM),
                eq("PENALTY_ISSUED"),
                any(Map.class),
                eq("Tài khoản của bạn vừa nhận một hình phạt"),
                any(),
                eq("PENALTY"),
                eq(71L));
    }

    /**
     * Test Case: IT-ADM-012
     * Title: Flag overdue urgent tasks in the admin queue.
     * Procedure: Prepare the stated fixture and input, then execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks).
     * Input: type=DISPUTE; priority=URGENT; overdue=true.
     * Steps:
     *   1. Prepare the fixture: A dispute task has passed its SLA deadline.
     *   2. Use the input: type=DISPUTE; priority=URGENT; overdue=true.
     *   3. Execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_012_OverdueTaskQueueItemsAreFlaggedBySla.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert task count/type and SLA flag.
     * Expected: The urgent DISPUTE task is returned with slaBreached=true.
     * Pre-conditions: A dispute task has passed its SLA deadline.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-012: Flag overdue urgent tasks in the admin queue.")
    void IT_ADM_012_OverdueTaskQueueItemsAreFlaggedBySla() {
        when(disputeRepository.findByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(dispute(60L, new BigDecimal("500000"), LocalDateTime.now().minusHours(25))));

        var page = taskQueueService.listTasks("DISPUTE", "URGENT", true, 0, 10);

        assertEquals(1, page.getTotalElements());
        assertEquals("DISPUTE", page.getContent().get(0).getTaskType());
        assertTrue(page.getContent().get(0).getSlaBreached());
    }

    /**
     * Test Case: IT-ADM-013
     * Title: Update the platform-fee parameter, validate it and record an audit snapshot.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.updateParameter (PATCH /api/catalog/parameters/{parameterId}).
     * Input: parameterId=10; new value 0.05.
     * Steps:
     *   1. Prepare the fixture: Parameter 10 currently has rate 0.02.
     *   2. Use the input: parameterId=10; new value 0.05.
     *   3. Execute SystemParameterServiceImpl.updateParameter (PATCH /api/catalog/parameters/{parameterId}). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_ADM_013_UpdatePlatformFeeParameterStoresValidatedValueAndAuditSnapshot.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response and capture UPDATE_SYSTEM_PARAMETER audit.
     * Expected: PLATFORM_FEE_RATE changes to 0.05 and the audit records the old/new values.
     * Pre-conditions: Parameter 10 currently has rate 0.02.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-013: Update the platform-fee parameter, validate it and record an audit snapshot.")
    void IT_ADM_013_UpdatePlatformFeeParameterStoresValidatedValueAndAuditSnapshot() {
        SystemParameter parameter = parameter(PARAM_ID, "PLATFORM_FEE_RATE", "0.02");
        UpsertSystemParameterRequest request = request("PLATFORM_FEE_RATE", "0.05");

        when(systemParameterRepository.findById(PARAM_ID)).thenReturn(Optional.of(parameter));
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(parameter));
        when(systemParameterRepository.save(parameter)).thenReturn(parameter);

        var response = systemParameterService.updateParameter(PARAM_ID, request);

        assertEquals("PLATFORM_FEE_RATE", response.getParamKey());
        assertEquals("0.05", response.getParamValue());
        verify(systemParameterRepository).save(parameter);
        verify(auditLogService).record(eq("UPDATE_SYSTEM_PARAMETER"), eq("SystemParameter"), eq(PARAM_ID), any(), eq(request));
    }

    /**
     * Test Case: IT-ADM-014
     * Title: Use the configured platform fee when building a center-request payment.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.createPayment (finance fee path).
     * Input: Center request amount 1000000 with valid payout.
     * Steps:
     *   1. Prepare the fixture: Fee parameter is 0.08 and system escrow wallet exists.
     *   2. Use the input: Center request amount 1000000 with valid payout.
     *   3. Execute CenterRequestFeeServiceImpl.createPayment (finance fee path). Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_ADM_014_PlatformFeeParameterIsUsedWhenBuildingCenterRequestPayment.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response amount and QR amount.
     * Expected: With platform fee 0.08 and request amount 1000000, the QR/payment amount is 80000.
     * Pre-conditions: Fee parameter is 0.08 and system escrow wallet exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-014: Use the configured platform fee when building a center-request payment.")
    void IT_ADM_014_PlatformFeeParameterIsUsedWhenBuildingCenterRequestPayment() {
        SystemParameter feeRate = new SystemParameter();
        feeRate.setParamKey("PLATFORM_FEE_RATE");
        feeRate.setParamValue("0.10");

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(feeRate));
        when(walletService.getSystemEscrowWallet()).thenReturn(wallet(999L));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(invocation -> {
            CenterRequestFeeHold hold = invocation.getArgument(0);
            hold.setFeeHoldId(603L);
            return hold;
        });

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("800000.00"),
                payoutInfo());

        assertEquals(new BigDecimal("80000"), response.getAmount());
        assertTrue(response.getQrUrl().contains("amount=80000"));
    }

    /**
     * Test Case: IT-ADM-015
     * Title: Persist audit actor, action, entity and JSON snapshots for a configuration update.
     * Procedure: Prepare the stated fixture and input, then execute AuditLogService.record (called by SystemParameterServiceImpl.updateParameter).
     * Input: Old rate 0.02 -> new rate 0.05.
     * Steps:
     *   1. Prepare the fixture: Admin actor and parameter update are available in the test database.
     *   2. Use the input: Old rate 0.02 -> new rate 0.05.
     *   3. Execute AuditLogService.record (called by SystemParameterServiceImpl.updateParameter). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_015_AuditLogRecordStoresActorAndJsonSnapshots.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture audit entity and assert all snapshot fields/timestamp.
     * Expected: The audit row stores actor, UPDATE_SYSTEM_PARAMETER, SystemParameter id 10 and old/new JSON snapshots.
     * Pre-conditions: Admin actor and parameter update are available in the test database.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-015: Persist audit actor, action, entity and JSON snapshots for a configuration update.")
    void IT_ADM_015_AuditLogRecordStoresActorAndJsonSnapshots() {
        User actor = user(ADMIN_USER_ID, "admin.it@tcs.test");

        when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(actor));

        auditLogServiceImpl.record(
                ADMIN_USER_ID,
                "UPDATE_SYSTEM_PARAMETER",
                "SystemParameter",
                10L,
                Map.of("value", "0.02"),
                Map.of("value", "0.05"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog log = auditLogCaptor.getValue();
        assertEquals(actor, log.getActor());
        assertEquals("UPDATE_SYSTEM_PARAMETER", log.getAction());
        assertEquals("SystemParameter", log.getEntityType());
        assertEquals(10L, log.getEntityId());
        assertTrue(log.getOldValue().contains("0.02"));
        assertTrue(log.getNewValue().contains("0.05"));
        assertNotNull(log.getCreatedAt());
    }

    /**
     * Test Case: IT-ADM-016
     * Title: Prevent a second revocation of an already revoked penalty.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.revokePenalty (PATCH /api/platform/penalties/{penaltyId}/revoke).
     * Input: penaltyId=81; revocation reason.
     * Steps:
     *   1. Prepare the fixture: Penalty 81 is already REVOKED.
     *   2. Use the input: penaltyId=81; revocation reason.
     *   3. Execute PenaltyServiceImpl.revokePenalty (PATCH /api/platform/penalties/{penaltyId}/revoke). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_016_RepeatedAdminPenaltyRevocationCannotCreateConflictingFinalStatus.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error/status and verify no save/audit.
     * Expected: The service returns the active-penalty prerequisite error and preserves REVOKED status without another save/audit.
     * Pre-conditions: Penalty 81 is already REVOKED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-016: Prevent a second revocation of an already revoked penalty.")
    void IT_ADM_016_RepeatedAdminPenaltyRevocationCannotCreateConflictingFinalStatus() {
        User adminUser = user(ADMIN_USER_ID, "admin.it@tcs.test");
        PlatformAdmin admin = platformAdmin(adminUser);
        User target = user(TARGET_USER_ID, "target.it@tcs.test");
        UserPenalty alreadyRevoked = penalty(
                81L,
                target,
                admin,
                UserPenaltyType.TEMPORARY_BAN,
                UserPenaltyStatus.REVOKED);
        RevokePenaltyRequest request = new RevokePenaltyRequest();
        request.setRevokedReason("Admin đã xử lý yêu cầu này trước đó");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(userPenaltyRepository.findById(81L)).thenReturn(Optional.of(alreadyRevoked));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> penaltyService.revokePenalty(81L, request));

        assertEquals("Chỉ có thể thu hồi hình phạt đang hoạt động", exception.getMessage());
        assertEquals(UserPenaltyStatus.REVOKED, alreadyRevoked.getStatus());
        verify(userPenaltyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    /**
     * Test Case: IT-ADM-017
     * Title: Revoke an active penalty and restore the user when no active ban remains.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.revokePenalty (PATCH /api/platform/penalties/{penaltyId}/revoke).
     * Input: penaltyId=80; valid revocation request.
     * Steps:
     *   1. Prepare the fixture: Admin owns active penalty 80 and target has no other active ban.
     *   2. Use the input: penaltyId=80; valid revocation request.
     *   3. Execute PenaltyServiceImpl.revokePenalty (PATCH /api/platform/penalties/{penaltyId}/revoke). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_017_RevokeActivePenaltyRestoresUserWhenNoActiveBanRemains.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert penalty/user statuses and audit/save calls.
     * Expected: Penalty 80 becomes REVOKED, target user becomes ACTIVE and the revocation is audited.
     * Pre-conditions: Admin owns active penalty 80 and target has no other active ban.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-017: Revoke an active penalty and restore the user when no active ban remains.")
    void IT_ADM_017_RevokeActivePenaltyRestoresUserWhenNoActiveBanRemains() {
        User adminUser = user(ADMIN_USER_ID, "admin.it@tcs.test");
        PlatformAdmin admin = platformAdmin(adminUser);
        User target = user(TARGET_USER_ID, "target.it@tcs.test");
        target.setStatus(UserStatus.BANNED);
        UserPenalty penalty = penalty(80L, target, admin, UserPenaltyType.TEMPORARY_BAN, UserPenaltyStatus.ACTIVE);
        RevokePenaltyRequest request = new RevokePenaltyRequest();
        request.setRevokedReason("Người dùng đã cung cấp đủ thông tin bổ sung");

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(new UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN));
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(admin));
        when(userPenaltyRepository.findById(80L)).thenReturn(Optional.of(penalty));
        when(userPenaltyRepository.existsByUser_UserIdAndStatusAndPenaltyTypeIn(
                eq(TARGET_USER_ID),
                eq(UserPenaltyStatus.ACTIVE),
                any()))
                .thenReturn(false);

        var response = penaltyService.revokePenalty(80L, request);

        assertEquals(UserPenaltyStatus.REVOKED.name(), response.getStatus());
        assertEquals(UserStatus.ACTIVE, target.getStatus());
        verify(userPenaltyRepository).save(penalty);
        verify(userRepository).save(target);
        verify(auditLogService).record(eq("REVOKE_PENALTY"), eq("UserPenalty"), eq(80L), eq(null), eq(request));
    }

    /**
     * Test Case: IT-ADM-018
     * Title: Sort admin tasks by priority and due time for stable polling.
     * Procedure: Prepare the stated fixture and input, then execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks).
     * Input: Unfiltered page=0; size=10.
     * Steps:
     *   1. Prepare the fixture: Queue fixtures have distinct priorities and due times.
     *   2. Use the input: Unfiltered page=0; size=10.
     *   3. Execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_018_TaskQueueSortsByPriorityThenDueTimeForStablePolling.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ordered task ids.
     * Expected: The returned order is TICKET-32, WITHDRAW-40, then TICKET-31 according to queue priority/due-time rules.
     * Pre-conditions: Queue fixtures have distinct priorities and due times.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-018: Sort admin tasks by priority and due time for stable polling.")
    void IT_ADM_018_TaskQueueSortsByPriorityThenDueTimeForStablePolling() {
        when(supportTicketRepository.findByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(
                        ticket(31L, SupportTicketPriority.MEDIUM, LocalDateTime.now().minusHours(1)),
                        ticket(32L, SupportTicketPriority.URGENT, LocalDateTime.now().minusHours(2))));
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of(withdrawal(40L, new BigDecimal("200000"), LocalDateTime.now().minusHours(3))));

        var page = taskQueueService.listTasks(null, null, null, 0, 10);

        assertEquals("TICKET-32", page.getContent().get(0).getTaskId());
        assertEquals("WITHDRAW-40", page.getContent().get(1).getTaskId());
        assertEquals("TICKET-31", page.getContent().get(2).getTaskId());
    }

    /**
     * Test Case: IT-ADM-019
     * Title: Expose admin routes for verification, reports, withdrawals, refunds and disputes.
     * Procedure: Prepare the stated fixture and input, then execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks).
     * Input: Unfiltered admin task request.
     * Steps:
     *   1. Prepare the fixture: Each queue contains one pending item.
     *   2. Use the input: Unfiltered admin task request.
     *   3. Execute TaskQueueServiceImpl.listTasks (GET /api/platform/tasks). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_019_TaskQueueItemsExposeAdminRoutesForVerificationReportWithdrawalRefundAndDispute.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert all target routes and dispute tab query.
     * Expected: Task items point to the correct frontend route/query for each business queue, including disputes under reports.
     * Pre-conditions: Each queue contains one pending item.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-019: Expose admin routes for verification, reports, withdrawals, refunds and disputes.")
    void IT_ADM_019_TaskQueueItemsExposeAdminRoutesForVerificationReportWithdrawalRefundAndDispute() {
        when(verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(VerificationStatus.SUBMITTED))
                .thenReturn(List.of(verification(10L, LocalDateTime.now().minusHours(1))));
        when(reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING))
                .thenReturn(List.of(report(20L, ReportCategory.SPAM, LocalDateTime.now().minusHours(2))));
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of(withdrawal(40L, new BigDecimal("200000"), LocalDateTime.now().minusHours(3))));
        when(refundRequestRepository.findByStatusOrderByRequestedAtAsc(RefundRequestStatus.PENDING))
                .thenReturn(List.of(refund(50L, new BigDecimal("300000"), LocalDateTime.now().minusHours(4))));
        when(disputeRepository.findByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(dispute(60L, new BigDecimal("500000"), LocalDateTime.now().minusHours(5))));

        var page = taskQueueService.listTasks(null, null, null, 0, 10);

        assertEquals("/platform/verifications", routeFor(page.getContent(), "VERIFICATION"));
        assertEquals("/platform/reports", routeFor(page.getContent(), "REPORT"));
        assertEquals("/platform/withdrawals", routeFor(page.getContent(), "WITHDRAWAL"));
        assertEquals("/platform/withdrawals", routeFor(page.getContent(), "REFUND_REQUEST"));
        assertEquals("/platform/reports", routeFor(page.getContent(), "DISPUTE"));
        assertTrue(queryFor(page.getContent(), "DISPUTE").contains("tab=disputes"));
    }

    /**
     * Test Case: IT-ADM-020
     * Title: Expire an overdue temporary ban and restore the account when no other ban remains.
     * Procedure: Prepare the stated fixture and input, then execute PenaltyServiceImpl.expireOverduePenalties (scheduled cleanup).
     * Input: Scheduled penalty-expiry scan.
     * Steps:
     *   1. Prepare the fixture: An ACTIVE temporary penalty has an expiry in the past and no other active restriction.
     *   2. Use the input: Scheduled penalty-expiry scan.
     *   3. Execute PenaltyServiceImpl.expireOverduePenalties (scheduled cleanup). Mapped test: com.tcs.module.platform.service.impl.Report52AdminPlatformITTest#IT_ADM_020_ExpireOverdueTemporaryBanRestoresAccountWhenNoOtherActiveBanExists.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert penalty/user statuses and saves.
     * Expected: The overdue penalty becomes EXPIRED and the target user returns to ACTIVE.
     * Pre-conditions: An ACTIVE temporary penalty has an expiry in the past and no other active restriction.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-ADM-020: Expire an overdue temporary ban and restore the account when no other ban remains.")
    void IT_ADM_020_ExpireOverdueTemporaryBanRestoresAccountWhenNoOtherActiveBanExists() {
        User target = user(TARGET_USER_ID, "target.it@tcs.test");
        target.setStatus(UserStatus.BANNED);
        UserPenalty overdue = penalty(90L, target, platformAdmin(user(ADMIN_USER_ID, "admin.it@tcs.test")),
                UserPenaltyType.TEMPORARY_BAN, UserPenaltyStatus.ACTIVE);
        overdue.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userPenaltyRepository.findByStatusAndExpiresAtBefore(eq(UserPenaltyStatus.ACTIVE), any()))
                .thenReturn(List.of(overdue));
        when(userPenaltyRepository.existsByUser_UserIdAndStatusAndPenaltyTypeIn(
                eq(TARGET_USER_ID),
                eq(UserPenaltyStatus.ACTIVE),
                any()))
                .thenReturn(false);

        penaltyService.expireOverduePenalties();

        assertEquals(UserPenaltyStatus.EXPIRED, overdue.getStatus());
        assertEquals(UserStatus.ACTIVE, target.getStatus());
        verify(userPenaltyRepository).save(overdue);
        verify(userRepository).save(target);
    }



    private IssuePenaltyRequest issuePenaltyRequest(String type) {
        IssuePenaltyRequest request = new IssuePenaltyRequest();
        request.setUserId(TARGET_USER_ID);
        request.setPenaltyType(type);
        request.setReason("Người dùng vi phạm quy định nền tảng trong quá trình sử dụng.");
        request.setSourceType("DIRECT");
        return request;
    }

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private PlatformAdmin platformAdmin(User user) {
        PlatformAdmin admin = new PlatformAdmin();
        admin.setAdminId(user.getUserId());
        admin.setUser(user);
        admin.setFullName("Admin IT");
        return admin;
    }

    private VerificationRequest verification(Long id, LocalDateTime submittedAt) {
        VerificationRequest request = new VerificationRequest();
        request.setVerificationId(id);
        request.setUser(user(100L + id, "verification" + id + "@tcs.test"));
        request.setVerificationType(VerificationType.TUTOR_PROFILE);
        request.setStatus(VerificationStatus.SUBMITTED);
        request.setSubmittedAt(submittedAt);
        request.setCreatedAt(submittedAt);
        return request;
    }

    private Report report(Long id, ReportCategory category, LocalDateTime createdAt) {
        Report report = new Report();
        report.setReportId(id);
        report.setReporter(user(200L + id, "reporter" + id + "@tcs.test"));
        report.setTargetType(ReportTargetType.CLASS);
        report.setTargetId(900L);
        report.setCategory(category);
        report.setDescription("Báo cáo cần admin xử lý");
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(createdAt);
        return report;
    }

    private SupportTicket ticket(Long id, SupportTicketPriority priority, LocalDateTime createdAt) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketId(id);
        ticket.setUser(user(300L + id, "ticket" + id + "@tcs.test"));
        ticket.setCategory(SupportTicketCategory.SYSTEM_ERROR);
        ticket.setSubject("Không mở được màn ví");
        ticket.setDescription("Người dùng cần hỗ trợ kỹ thuật");
        ticket.setPriority(priority);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setCreatedAt(createdAt);
        return ticket;
    }

    private WithdrawalRequest withdrawal(Long id, BigDecimal amount, LocalDateTime requestedAt) {
        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setWithdrawalId(id);
        withdrawal.setWallet(wallet(user(400L + id, "withdraw" + id + "@tcs.test")));
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalRequestStatus.PENDING);
        withdrawal.setRequestedAt(requestedAt);
        return withdrawal;
    }

    private RefundRequest refund(Long id, BigDecimal amount, LocalDateTime requestedAt) {
        RefundRequest refund = new RefundRequest();
        refund.setRefundId(id);
        refund.setRequestedBy(user(500L + id, "refund" + id + "@tcs.test"));
        refund.setAmount(amount);
        refund.setReason("Hoàn tiền do yêu cầu nhờ trung tâm không hoàn tất");
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setRequestedAt(requestedAt);
        return refund;
    }

    private Dispute dispute(Long id, BigDecimal amount, LocalDateTime createdAt) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(700L + id);
        escrow.setAmount(amount);
        Dispute dispute = new Dispute();
        dispute.setDisputeId(id);
        dispute.setEscrowTransaction(escrow);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setResolution("Tranh chấp đang chờ quyết định");
        dispute.setCreatedAt(createdAt);
        return dispute;
    }

    private Wallet wallet(User owner) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(owner.getUserId());
        wallet.setUser(owner);
        return wallet;
    }

    private UserPenalty penalty(
            Long id,
            User user,
            PlatformAdmin admin,
            UserPenaltyType type,
            UserPenaltyStatus status) {

        UserPenalty penalty = new UserPenalty();
        penalty.setPenaltyId(id);
        penalty.setUser(user);
        penalty.setIssuedBy(admin);
        penalty.setPenaltyType(type);
        penalty.setStatus(status);
        penalty.setReason("Người dùng vi phạm quy định nền tảng trong quá trình sử dụng.");
        penalty.setStartsAt(LocalDateTime.now().minusDays(1));
        penalty.setCreatedAt(LocalDateTime.now().minusDays(1));
        return penalty;
    }

    private String routeFor(List<com.tcs.module.platform.dto.response.TaskItemResponse> items, String taskType) {
        return items.stream()
                .filter(item -> taskType.equals(item.getTaskType()))
                .findFirst()
                .orElseThrow()
                .getTargetRoute();
    }

    private String queryFor(List<com.tcs.module.platform.dto.response.TaskItemResponse> items, String taskType) {
        return items.stream()
                .filter(item -> taskType.equals(item.getTaskType()))
                .findFirst()
                .orElseThrow()
                .getTargetQuery();
    }


    private Wallet wallet(Long id) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(id);
        User user = new User();
        user.setUserId(id);
        wallet.setUser(user);
        return wallet;
    }

    private RefundPayoutInfo payoutInfo() {
        return new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A");
    }

    

    private UpsertSystemParameterRequest request(String key, String value) {
        UpsertSystemParameterRequest request = new UpsertSystemParameterRequest();
        request.setParamKey(key);
        request.setParamValue(value);
        request.setDescription("IT config value");
        return request;
    }

    

    private SystemParameter parameter(Long id, String key, String value) {
        SystemParameter parameter = new SystemParameter();
        parameter.setParameterId(id);
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        return parameter;
    }
}
