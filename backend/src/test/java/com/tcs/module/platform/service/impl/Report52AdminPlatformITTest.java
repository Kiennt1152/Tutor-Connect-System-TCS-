package com.tcs.module.platform.service.impl;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.messaging.enums.NotificationType;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52AdminPlatformITTest {

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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_ADM_003_TaskItemsCarryAdminTargetRouteAndEntityReference() {
        when(withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus.PENDING))
                .thenReturn(List.of(withdrawal(40L, new BigDecimal("200000"), LocalDateTime.now().minusHours(1))));

        var page = taskQueueService.listTasks("WITHDRAWAL", null, null, 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals("/platform/withdrawals", page.getContent().get(0).getTargetRoute());
        assertEquals("?id=40", page.getContent().get(0).getTargetQuery());
        assertEquals(40L, page.getContent().get(0).getEntityId());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_ADM_012_OverdueTaskQueueItemsAreFlaggedBySla() {
        when(disputeRepository.findByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(dispute(60L, new BigDecimal("500000"), LocalDateTime.now().minusHours(25))));

        var page = taskQueueService.listTasks("DISPUTE", "URGENT", true, 0, 10);

        assertEquals(1, page.getTotalElements());
        assertEquals("DISPUTE", page.getContent().get(0).getTaskType());
        assertTrue(page.getContent().get(0).getSlaBreached());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
}
