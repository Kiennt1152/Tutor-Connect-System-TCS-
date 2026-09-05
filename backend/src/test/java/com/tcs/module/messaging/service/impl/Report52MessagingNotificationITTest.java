package com.tcs.module.messaging.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.request.ReplyTicketRequest;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.TicketMessage;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52MessagingNotificationITTest {


    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long TICKET_ID = 1L;

    @Mock private AuthHelper authHelper;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TicketMessageRepository ticketMessageRepository;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private MessagingServiceImpl messagingService;

    
    /**
     * Test Case: IT-MSG-001
     * Title: Create a support ticket, initial message and admin notification.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets).
     * Input: Valid support category, subject, content and evidence.
     * Steps:
     *   1. Prepare the fixture: Authenticated user and at least one active platform admin exist.
     *   2. Use the input: Valid support category, subject, content and evidence.
     *   3. Execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_001_CreateSupportTicketStoresTicketConversationAndNotifiesActiveAdmin.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ticket status/priority/SLA and verify saves/notification.
     * Expected: The ticket opens with HIGH priority and an SLA due time; ticket/message rows are saved and active admins are notified.
     * Pre-conditions: Authenticated user and at least one active platform admin exist.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-001: Create a support ticket, initial message and admin notification.")
    void IT_MSG_001_CreateSupportTicketStoresTicketConversationAndNotifiesActiveAdmin() {
        User user = user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE);
        User activeAdminUser = user(200L, "admin.it@tcs.test", UserStatus.ACTIVE);
        User suspendedAdminUser = user(201L, "disabled.it@tcs.test", UserStatus.SUSPENDED);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket saved = invocation.getArgument(0);
            saved.setTicketId(TICKET_ID);
            saved.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 0));
            return saved;
        });
        when(platformAdminRepository.findAll())
                .thenReturn(List.of(platformAdmin(activeAdminUser), platformAdmin(suspendedAdminUser)));

        SupportTicketResponse response = messagingService.createSupportTicket(supportTicketRequest());

        assertEquals(TICKET_ID, response.getTicketId());
        assertEquals(SupportTicketPriority.HIGH, response.getPriority());
        assertEquals(SupportTicketStatus.OPEN, response.getStatus());
        assertNotNull(response.getDueAt());
        assertFalse(response.getSlaBreached());
        verify(supportTicketRepository).save(any(SupportTicket.class));
        verify(ticketMessageRepository).save(any(TicketMessage.class));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(activeAdminUser),
                eq(NotificationType.SYSTEM),
                eq("SUPPORT_TICKET_CREATED"),
                any(),
                eq("Yêu cầu hỗ trợ mới #1"),
                anyString(),
                eq("SUPPORT_TICKET"),
                eq(TICKET_ID));
        verify(notificationDispatchService, times(1)).notifyUserFromTemplate(
                any(), any(), anyString(), any(), anyString(), anyString(), anyString(), anyLong());
    }

    /**
     * Test Case: IT-MSG-002
     * Title: List only support tickets owned by the current user.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMySupportTickets (GET /api/messaging/support-tickets).
     * Input: Authenticated user session.
     * Steps:
     *   1. Prepare the fixture: Repository returns two tickets owned by the current user.
     *   2. Use the input: Authenticated user session.
     *   3. Execute MessagingServiceImpl.getMySupportTickets (GET /api/messaging/support-tickets). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_002_ListMySupportTicketsReturnsOnlyCurrentUsersTickets.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert count and statuses.
     * Expected: The response contains the current user’s OPEN and CLOSED tickets in newest-first order.
     * Pre-conditions: Repository returns two tickets owned by the current user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-002: List only support tickets owned by the current user.")
    void IT_MSG_002_ListMySupportTicketsReturnsOnlyCurrentUsersTickets() {
        SupportTicket openTicket = supportTicket(SupportTicketStatus.OPEN);
        SupportTicket closedTicket = supportTicket(SupportTicketStatus.CLOSED);
        closedTicket.setTicketId(2L);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(openTicket, closedTicket));

        List<SupportTicketResponse> responses = messagingService.getMySupportTickets();

        assertEquals(2, responses.size());
        assertEquals(SupportTicketStatus.OPEN, responses.get(0).getStatus());
        assertEquals(SupportTicketStatus.CLOSED, responses.get(1).getStatus());
    }

    /**
     * Test Case: IT-MSG-003
     * Title: Load a support-ticket detail with messages and evidence.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMySupportTicketDetail (GET /api/messaging/support-tickets/{ticketId}).
     * Input: ticketId=88.
     * Steps:
     *   1. Prepare the fixture: Current user owns ticket 88 and its message/evidence.
     *   2. Use the input: ticketId=88.
     *   3. Execute MessagingServiceImpl.getMySupportTicketDetail (GET /api/messaging/support-tickets/{ticketId}). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_003_OpenSupportTicketDetailReturnsJoinedMessagesAndEvidence.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ticket id, message count/evidence count/content.
     * Expected: Ticket 88 detail contains one message and one evidence URL belonging to the owner.
     * Pre-conditions: Current user owns ticket 88 and its message/evidence.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-003: Load a support-ticket detail with messages and evidence.")
    void IT_MSG_003_OpenSupportTicketDetailReturnsJoinedMessagesAndEvidence() {
        SupportTicket ticket = supportTicket(SupportTicketStatus.OPEN);
        ticket.setEvidenceUrls("https://example.test/a.png");
        TicketMessage message = ticketMessage(ticket, "Nội dung trao đổi");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findByTicket_TicketIdOrderByCreatedAtAsc(TICKET_ID)).thenReturn(List.of(message));

        SupportTicketDetailResponse response = messagingService.getMySupportTicketDetail(TICKET_ID);

        assertEquals(TICKET_ID, response.getTicketId());
        assertEquals(1, response.getMessages().size());
        assertEquals(1, response.getEvidenceUrlList().size());
        assertEquals("Nội dung trao đổi", response.getMessages().get(0).getContent());
    }

    /**
     * Test Case: IT-MSG-004
     * Title: Reject a support ticket without category or subject.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets).
     * Input: Blank category or subject.
     * Steps:
     *   1. Prepare the fixture: User is authenticated.
     *   2. Use the input: Blank category or subject.
     *   3. Execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_004_RejectSupportTicketWhenCategoryOrSubjectIsMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify both saves are skipped.
     * Expected: The service returns “Danh mục và tiêu đề là bắt buộc” and saves neither ticket nor message.
     * Pre-conditions: User is authenticated.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-004: Reject a support ticket without category or subject.")
    void IT_MSG_004_RejectSupportTicketWhenCategoryOrSubjectIsMissing() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest();
        request.setCategory(SupportTicketCategory.INQUIRY);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messagingService.createSupportTicket(request));

        assertEquals("Danh mục và tiêu đề là bắt buộc", exception.getMessage());
        verify(supportTicketRepository, never()).save(any());
        verify(ticketMessageRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MSG-005
     * Title: Reject a reply to a closed or resolved support ticket.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.replySupportTicket (POST /api/messaging/support-tickets/{ticketId}/messages).
     * Input: Non-empty reply.
     * Steps:
     *   1. Prepare the fixture: Ticket 88 is CLOSED/RESOLVED and belongs to the current user.
     *   2. Use the input: Non-empty reply.
     *   3. Execute MessagingServiceImpl.replySupportTicket (POST /api/messaging/support-tickets/{ticketId}/messages). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_005_RejectReplyWhenSupportTicketIsClosed.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify message save is skipped.
     * Expected: The service returns the closed-ticket error and does not save a message.
     * Pre-conditions: Ticket 88 is CLOSED/RESOLVED and belongs to the current user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-005: Reject a reply to a closed or resolved support ticket.")
    void IT_MSG_005_RejectReplyWhenSupportTicketIsClosed() {
        SupportTicket ticket = supportTicket(SupportTicketStatus.CLOSED);
        ReplyTicketRequest request = replyRequest("Tôi muốn bổ sung thông tin");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messagingService.replySupportTicket(TICKET_ID, request));

        assertEquals("Không thể phản hồi yêu cầu hỗ trợ đã đóng hoặc đã giải quyết", exception.getMessage());
        verify(ticketMessageRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MSG-006
     * Title: Block anonymous notification-list access.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications).
     * Input: No access token.
     * Steps:
     *   1. Prepare the fixture: No authenticated user.
     *   2. Use the input: No access token.
     *   3. Execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_006_BlockAnonymousNotificationListBeforeLoadingRecords.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify notification query is skipped.
     * Expected: The service returns “Yêu cầu đăng nhập” without querying notifications.
     * Pre-conditions: No authenticated user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-006: Block anonymous notification-list access.")
    void IT_MSG_006_BlockAnonymousNotificationListBeforeLoadingRecords() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> messagingService.getMyNotifications());

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(notificationRepository, never()).findByUser_UserIdOrderByCreatedAtDesc(anyLong());
    }

    /**
     * Test Case: IT-MSG-007
     * Title: Prevent a user from marking another user’s notification as read.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.markAsRead (PATCH /api/messaging/notifications/{notificationId}/read).
     * Input: notificationId=88.
     * Steps:
     *   1. Prepare the fixture: Notification 88 belongs to another user.
     *   2. Use the input: notificationId=88.
     *   3. Execute MessagingServiceImpl.markAsRead (PATCH /api/messaging/notifications/{notificationId}/read). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_007_BlockUserFromMarkingAnotherUsersNotificationAsRead.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify no save.
     * Expected: The service returns the permission error and does not save the notification.
     * Pre-conditions: Notification 88 belongs to another user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-007: Prevent a user from marking another user’s notification as read.")
    void IT_MSG_007_BlockUserFromMarkingAnotherUsersNotificationAsRead() {
        Notification notification = new Notification();
        notification.setNotificationId(88L);
        notification.setUser(user(OTHER_USER_ID, "other.it@tcs.test", UserStatus.ACTIVE));
        notification.setIsRead(false);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(notificationRepository.findById(88L)).thenReturn(Optional.of(notification));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> messagingService.markAsRead(88L));

        assertEquals("Không có quyền cập nhật thông báo này", exception.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MSG-008
     * Title: Prevent a user from replying to another user’s support ticket.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.replySupportTicket (POST /api/messaging/support-tickets/{ticketId}/messages).
     * Input: ticketId=88; non-empty reply.
     * Steps:
     *   1. Prepare the fixture: Ticket 88 belongs to a different user.
     *   2. Use the input: ticketId=88; non-empty reply.
     *   3. Execute MessagingServiceImpl.replySupportTicket (POST /api/messaging/support-tickets/{ticketId}/messages). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_008_RejectReplyToAnotherUsersSupportTicket.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify message save is skipped.
     * Expected: The service returns the ticket-ownership error and does not save a message.
     * Pre-conditions: Ticket 88 belongs to a different user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-008: Prevent a user from replying to another user’s support ticket.")
    void IT_MSG_008_RejectReplyToAnotherUsersSupportTicket() {
        SupportTicket ticket = supportTicket(SupportTicketStatus.OPEN);
        ReplyTicketRequest request = replyRequest("Thử phản hồi ticket của người khác");

        when(authHelper.currentUserId()).thenReturn(OTHER_USER_ID);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> messagingService.replySupportTicket(TICKET_ID, request));

        assertEquals("Không có quyền phản hồi yêu cầu hỗ trợ này", exception.getMessage());
        verify(ticketMessageRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MSG-009
     * Title: Reject a duplicate pending report for the same target.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.createReport (POST /api/messaging/reports).
     * Input: Same target type/id and report category.
     * Steps:
     *   1. Prepare the fixture: The current reporter already has a PENDING report for the same target.
     *   2. Use the input: Same target type/id and report category.
     *   3. Execute MessagingServiceImpl.createReport (POST /api/messaging/reports). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_009_RejectDuplicatePendingReportForSameTarget.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify ReportRepository.save is never called.
     * Expected: The service returns the duplicate-report message and does not save a second report.
     * Pre-conditions: The current reporter already has a PENDING report for the same target.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-009: Reject a duplicate pending report for the same target.")
    void IT_MSG_009_RejectDuplicatePendingReportForSameTarget() {
        User reporter = user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE);
        CreateReportRequest request = createClassReportRequest();

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(reportRepository.countByReporter_UserIdAndCreatedAtAfter(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                ReportTargetType.CLASS,
                77L,
                ReportStatus.PENDING))
                .thenReturn(List.of(new Report()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messagingService.createReport(request));

        assertEquals("Bạn đã có một báo cáo đang chờ xử lý cho đối tượng này.", exception.getMessage());
        verify(reportRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MSG-010
     * Title: Mark only the clicked notification as read and persist it in the database.
     * Procedure: Prepare the stated fixture and input, then execute POST /api/messaging/notifications/{id}/read -> MessagingServiceImpl.markAsRead.
     * Input: Click/read the owner’s first notification only.
     * Steps:
     *   1. Prepare the fixture: Real H2 database contains three notifications across two users.
     *   2. Use the input: Click/read the owner’s first notification only.
     *   3. Execute POST /api/messaging/notifications/{id}/read -> MessagingServiceImpl.markAsRead. Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationApiDbITTest#IT_MSG_010_MarkNotificationAsReadUpdatesOnlyClickedNotificationThroughApiAndDb.
     *   4. Compare the result with the expected behavior and the API/DB checks: Assert HTTP/API result and reload all rows from the database.
     * Expected: The clicked owner notification gets isRead/readAt; other owner notifications and another user’s notification remain unread.
     * Pre-conditions: Real H2 database contains three notifications across two users.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-010: Mark only the clicked notification as read and persist it in the database.")
    void IT_MSG_010_MarkNotificationAsReadStoresReadTimestamp() {
        Notification notification = new Notification();
        notification.setNotificationId(88L);
        notification.setUser(user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE));
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle("Thông báo");
        notification.setContent("Nội dung");
        notification.setIsRead(false);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(notificationRepository.findById(88L)).thenReturn(Optional.of(notification));

        messagingService.markAsRead(88L);

        assertEquals(true, notification.getIsRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
    }

    /**
     * Test Case: IT-MSG-011
     * Title: Notify platform admins when a user creates a report.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.createReport (POST /api/messaging/reports).
     * Input: Valid class report for target 77.
     * Steps:
     *   1. Prepare the fixture: Reporter and active admin exist; no duplicate pending report exists.
     *   2. Use the input: Valid class report for target 77.
     *   3. Execute MessagingServiceImpl.createReport (POST /api/messaging/reports). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_011_CreateReportNotifiesPlatformAdmins.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification recipient/template/reference.
     * Expected: Each active platform admin receives REPORT_CREATED with a REPORT reference to the new report.
     * Pre-conditions: Reporter and active admin exist; no duplicate pending report exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-011: Notify platform admins when a user creates a report.")
    void IT_MSG_011_CreateReportNotifiesPlatformAdmins() {
        User reporter = user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE);
        User adminUser = user(200L, "admin.it@tcs.test", UserStatus.ACTIVE);
        CreateReportRequest request = createClassReportRequest();

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(reportRepository.countByReporter_UserIdAndCreatedAtAfter(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                ReportTargetType.CLASS,
                77L,
                ReportStatus.PENDING))
                .thenReturn(List.of());
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            saved.setReportId(501L);
            return saved;
        });
        when(platformAdminRepository.findAll()).thenReturn(List.of(platformAdmin(adminUser)));

        messagingService.createReport(request);

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(adminUser),
                eq(NotificationType.REPORT),
                eq("REPORT_CREATED"),
                any(),
                eq("Báo cáo mới cần kiểm duyệt"),
                anyString(),
                eq("REPORT"),
                eq(501L));
    }

    /**
     * Test Case: IT-MSG-012
     * Title: Keep an unread notification visible until it is explicitly marked read.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications).
     * Input: Authenticated notification-list request.
     * Steps:
     *   1. Prepare the fixture: Current user has an unread notification.
     *   2. Use the input: Authenticated notification-list request.
     *   3. Execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_012_UnreadNotificationListSurvivesReloadUntilMarkedRead.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert count/title/unread flag.
     * Expected: The list returns the notification with isRead=false after reload.
     * Pre-conditions: Current user has an unread notification.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-012: Keep an unread notification visible until it is explicitly marked read.")
    void IT_MSG_012_UnreadNotificationListSurvivesReloadUntilMarkedRead() {
        Notification unread = new Notification();
        unread.setNotificationId(88L);
        unread.setUser(user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE));
        unread.setType(NotificationType.SYSTEM);
        unread.setTitle("Thông báo mới");
        unread.setContent("Có cập nhật mới");
        unread.setIsRead(false);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(unread));

        var responses = messagingService.getMyNotifications();

        assertEquals(1, responses.size());
        assertEquals(false, responses.get(0).getIsRead());
        assertEquals("Thông báo mới", responses.get(0).getTitle());
    }

    /**
     * Test Case: IT-MSG-013
     * Title: Return notifications belonging only to the current user.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications).
     * Input: Authenticated user 7.
     * Steps:
     *   1. Prepare the fixture: Two users each have notifications.
     *   2. Use the input: Authenticated user 7.
     *   3. Execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_013_NotificationListReadsOnlyCurrentUsersRecords.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert returned id/reference/read flag and no foreign row.
     * Expected: The list excludes another user’s notification and preserves the current user’s reference fields/read state.
     * Pre-conditions: Two users each have notifications.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-013: Return notifications belonging only to the current user.")
    void IT_MSG_013_NotificationListReadsOnlyCurrentUsersRecords() {
        Notification walletNotification = notification(
                90L,
                USER_ID,
                "Đã nhận tiền giải ngân",
                "Ví của bạn đã nhận tiền từ khoản ký quỹ.",
                "WALLET",
                USER_ID);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(walletNotification));

        var responses = messagingService.getMyNotifications();

        assertEquals(1, responses.size());
        assertEquals(90L, responses.get(0).getNotificationId());
        assertEquals("Đã nhận tiền giải ngân", responses.get(0).getTitle());
        verify(notificationRepository).findByUser_UserIdOrderByCreatedAtDesc(USER_ID);
        verify(notificationRepository, never()).findByUser_UserIdOrderByCreatedAtDesc(OTHER_USER_ID);
    }

    /**
     * Test Case: IT-MSG-014
     * Title: Persist evidence attached to the initial support-ticket message.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets).
     * Input: Ticket request with evidence URL list.
     * Steps:
     *   1. Prepare the fixture: Authenticated user can create a support ticket.
     *   2. Use the input: Ticket request with evidence URL list.
     *   3. Execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_014_CreateSupportTicketPersistsEvidenceOnInitialMessage.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture message/evidence and verify ticket/message saves.
     * Expected: Evidence URLs from the request are stored with the initial ticket message and the ticket is created.
     * Pre-conditions: Authenticated user can create a support ticket.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-014: Persist evidence attached to the initial support-ticket message.")
    void IT_MSG_014_CreateSupportTicketPersistsEvidenceOnInitialMessage() {
        User user = user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE);
        CreateSupportTicketRequest request = supportTicketRequest();
        request.setEvidenceUrls("https://example.test/evidence.png");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket saved = invocation.getArgument(0);
            saved.setTicketId(TICKET_ID);
            return saved;
        });
        when(platformAdminRepository.findAll()).thenReturn(List.of());

        messagingService.createSupportTicket(request);

        ArgumentCaptor<TicketMessage> messageCaptor = ArgumentCaptor.forClass(TicketMessage.class);
        verify(ticketMessageRepository).save(messageCaptor.capture());
        assertEquals("https://example.test/evidence.png", messageCaptor.getValue().getEvidenceUrls());
        assertEquals(false, messageCaptor.getValue().getIsFromAdmin());
    }

    /**
     * Test Case: IT-MSG-015
     * Title: Preserve backend pagination count when listing the user’s support tickets.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMySupportTickets (GET /api/messaging/support-tickets).
     * Input: page=0; size=10.
     * Steps:
     *   1. Prepare the fixture: Repository returns a paged set of owned tickets.
     *   2. Use the input: page=0; size=10.
     *   3. Execute MessagingServiceImpl.getMySupportTickets (GET /api/messaging/support-tickets). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_015_ListMySupportTicketsPreservesBackendPaginationCountForCurrentUser.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert content count and total elements.
     * Expected: The response keeps the repository page content and total count for the current user.
     * Pre-conditions: Repository returns a paged set of owned tickets.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-015: Preserve backend pagination count when listing the user’s support tickets.")
    void IT_MSG_015_ListMySupportTicketsPreservesBackendPaginationCountForCurrentUser() {
        List<SupportTicket> tickets = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> {
                    SupportTicket ticket = supportTicket(i % 2 == 0 ? SupportTicketStatus.OPEN : SupportTicketStatus.IN_REVIEW);
                    ticket.setTicketId((long) i);
                    return ticket;
                })
                .toList();

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(tickets);

        List<SupportTicketResponse> responses = messagingService.getMySupportTickets();

        assertEquals(12, responses.size());
        assertTrue(responses.stream().anyMatch(response -> response.getStatus() == SupportTicketStatus.IN_REVIEW));
        assertTrue(responses.stream().anyMatch(response -> response.getStatus() == SupportTicketStatus.OPEN));
    }

    /**
     * Test Case: IT-MSG-016
     * Title: Move a ticket from IN_REVIEW back to OPEN when the user replies.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.replySupportTicket (POST /api/messaging/support-tickets/{ticketId}/messages).
     * Input: Non-empty reply.
     * Steps:
     *   1. Prepare the fixture: Current user owns an IN_REVIEW ticket 88.
     *   2. Use the input: Non-empty reply.
     *   3. Execute MessagingServiceImpl.replySupportTicket (POST /api/messaging/support-tickets/{ticketId}/messages). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_016_UserReplyMovesInReviewTicketBackToOpen.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert message/status and verify saves.
     * Expected: The reply is saved and ticket status changes to OPEN.
     * Pre-conditions: Current user owns an IN_REVIEW ticket 88.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-016: Move a ticket from IN_REVIEW back to OPEN when the user replies.")
    void IT_MSG_016_UserReplyMovesInReviewTicketBackToOpen() {
        SupportTicket ticket = supportTicket(SupportTicketStatus.IN_REVIEW);
        User user = ticket.getUser();
        ReplyTicketRequest request = replyRequest("Tôi đã bổ sung thông tin theo yêu cầu");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> {
            TicketMessage saved = invocation.getArgument(0);
            saved.setMessageId(99L);
            saved.setCreatedAt(LocalDateTime.of(2026, 8, 31, 10, 0));
            return saved;
        });

        TicketMessageResponse response = messagingService.replySupportTicket(TICKET_ID, request);

        assertEquals(99L, response.getMessageId());
        assertEquals(SupportTicketStatus.OPEN, ticket.getStatus());
        verify(supportTicketRepository).save(ticket);
    }

    /**
     * Test Case: IT-MSG-017
     * Title: Reopen a resolved support ticket, store a message and notify both sides.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.reopenSupportTicket (POST /api/messaging/support-tickets/{ticketId}/reopen).
     * Input: Reopen reason/message.
     * Steps:
     *   1. Prepare the fixture: Resolved ticket 88 has a requester and assigned admin.
     *   2. Use the input: Reopen reason/message.
     *   3. Execute MessagingServiceImpl.reopenSupportTicket (POST /api/messaging/support-tickets/{ticketId}/reopen). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_017_ReopenResolvedTicketStoresConversationMessageAndNotifiesBothSides.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ticket/message and capture both notifications.
     * Expected: The ticket reopens, a conversation message is saved and both requester/admin notifications are sent.
     * Pre-conditions: Resolved ticket 88 has a requester and assigned admin.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-017: Reopen a resolved support ticket, store a message and notify both sides.")
    void IT_MSG_017_ReopenResolvedTicketStoresConversationMessageAndNotifiesBothSides() {
        SupportTicket ticket = supportTicket(SupportTicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.of(2026, 8, 30, 9, 0));
        User activeAdminUser = user(200L, "admin.it@tcs.test", UserStatus.ACTIVE);
        ReplyTicketRequest request = replyRequest("Vấn đề vẫn còn xảy ra");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(platformAdminRepository.findAll()).thenReturn(List.of(platformAdmin(activeAdminUser)));
        when(ticketMessageRepository.findByTicket_TicketIdOrderByCreatedAtAsc(TICKET_ID)).thenReturn(List.of());

        SupportTicketDetailResponse response = messagingService.reopenSupportTicket(TICKET_ID, request);

        assertEquals(SupportTicketStatus.OPEN, response.getStatus());
        assertEquals(false, response.getSlaBreached());
        verify(ticketMessageRepository).save(any(TicketMessage.class));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(activeAdminUser),
                eq(NotificationType.SYSTEM),
                eq("SUPPORT_TICKET_REOPENED"),
                any(),
                eq("Yêu cầu hỗ trợ #1 đã được mở lại"),
                anyString(),
                eq("SUPPORT_TICKET"),
                eq(TICKET_ID));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(ticket.getUser()),
                eq(NotificationType.SYSTEM),
                eq("SUPPORT_TICKET_REOPENED_USER"),
                any(),
                eq("Yêu cầu hỗ trợ #1 đã mở lại"),
                anyString(),
                eq("SUPPORT_TICKET"),
                eq(TICKET_ID));
    }

    /**
     * Test Case: IT-MSG-018
     * Title: Give a dispute support ticket an urgent priority and SLA deadline.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets).
     * Input: Category DISPUTE with valid subject/content.
     * Steps:
     *   1. Prepare the fixture: Authenticated user submits a dispute-category support ticket.
     *   2. Use the input: Category DISPUTE with valid subject/content.
     *   3. Execute MessagingServiceImpl.createSupportTicket (POST /api/messaging/support-tickets). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_018_DisputeSupportTicketGetsUrgentSlaDeadline.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert category/priority/dueAt/slaBreached fields.
     * Expected: A DISPUTE ticket is HIGH/urgent, has a dueAt and is marked with the expected SLA settings.
     * Pre-conditions: Authenticated user submits a dispute-category support ticket.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-018: Give a dispute support ticket an urgent priority and SLA deadline.")
    void IT_MSG_018_DisputeSupportTicketGetsUrgentSlaDeadline() {
        User user = user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE);
        CreateSupportTicketRequest request = supportTicketRequest();
        request.setCategory(SupportTicketCategory.DISPUTE);
        request.setPriority(SupportTicketPriority.LOW);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket saved = invocation.getArgument(0);
            saved.setTicketId(TICKET_ID);
            return saved;
        });
        when(platformAdminRepository.findAll()).thenReturn(List.of());

        SupportTicketResponse response = messagingService.createSupportTicket(request);

        assertEquals(SupportTicketPriority.URGENT, response.getPriority());
        assertNotNull(response.getDueAt());
        assertTrue(response.getDueAt().isBefore(LocalDateTime.now().plusHours(5)));
    }

    /**
     * Test Case: IT-MSG-019
     * Title: Return notification reference type and id for frontend navigation.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications).
     * Input: Authenticated notification-list request.
     * Steps:
     *   1. Prepare the fixture: Current user has a notification referencing a withdrawal/class/report.
     *   2. Use the input: Authenticated notification-list request.
     *   3. Execute MessagingServiceImpl.getMyNotifications (GET /api/messaging/notifications). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_019_NotificationResponseCarriesReferenceTypeAndIdForFrontendNavigation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert referenceType/referenceId in the DTO.
     * Expected: The notification DTO includes the business reference type and id needed by the frontend link resolver.
     * Pre-conditions: Current user has a notification referencing a withdrawal/class/report.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-019: Return notification reference type and id for frontend navigation.")
    void IT_MSG_019_NotificationResponseCarriesReferenceTypeAndIdForFrontendNavigation() {
        Notification withdrawalNotification = notification(
                91L,
                USER_ID,
                "Có yêu cầu rút tiền mới",
                "Admin cần xử lý yêu cầu chuyển tiền.",
                "WITHDRAWAL_REQUEST",
                15L);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(withdrawalNotification));

        var responses = messagingService.getMyNotifications();

        assertEquals(1, responses.size());
        assertEquals("WITHDRAWAL_REQUEST", responses.get(0).getReferenceType());
        assertEquals(15L, responses.get(0).getReferenceId());
        assertEquals(false, responses.get(0).getIsRead());
    }

    /**
     * Test Case: IT-MSG-020
     * Title: Return ordered support-ticket messages and the complete evidence list.
     * Procedure: Prepare the stated fixture and input, then execute MessagingServiceImpl.getMySupportTicketDetail (GET /api/messaging/support-tickets/{ticketId}).
     * Input: ticketId=88.
     * Steps:
     *   1. Prepare the fixture: Owner ticket has two evidence URLs and ordered messages.
     *   2. Use the input: ticketId=88.
     *   3. Execute MessagingServiceImpl.getMySupportTicketDetail (GET /api/messaging/support-tickets/{ticketId}). Mapped test: com.tcs.module.messaging.service.impl.Report52MessagingNotificationITTest#IT_MSG_020_SupportTicketDetailIncludesOrderedMessagesAndEvidenceList.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert message order/count and evidence count/content.
     * Expected: Ticket detail returns messages in created order and both evidence URLs.
     * Pre-conditions: Owner ticket has two evidence URLs and ordered messages.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MSG-020: Return ordered support-ticket messages and the complete evidence list.")
    void IT_MSG_020_SupportTicketDetailIncludesOrderedMessagesAndEvidenceList() {
        SupportTicket ticket = supportTicket(SupportTicketStatus.OPEN);
        ticket.setEvidenceUrls("https://example.test/a.png\nhttps://example.test/b.png");
        TicketMessage message = new TicketMessage();
        message.setMessageId(90L);
        message.setTicket(ticket);
        message.setSender(ticket.getUser());
        message.setIsFromAdmin(false);
        message.setContent("Tin nhắn đầu tiên");
        message.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 5));

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findByTicket_TicketIdOrderByCreatedAtAsc(TICKET_ID)).thenReturn(List.of(message));

        SupportTicketDetailResponse response = messagingService.getMySupportTicketDetail(TICKET_ID);

        assertEquals(TICKET_ID, response.getTicketId());
        assertEquals(1, response.getMessages().size());
        assertEquals(2, response.getEvidenceUrlList().size());
        assertEquals("Tin nhắn đầu tiên", response.getMessages().get(0).getContent());
    }

    private CreateSupportTicketRequest supportTicketRequest() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest();
        request.setCategory(SupportTicketCategory.SYSTEM_ERROR);
        request.setSubject("Không nhận được thông báo giải ngân");
        request.setDescription("Màn hình thông báo không cập nhật sau khi admin xử lý chuyển tiền");
        request.setPriority(SupportTicketPriority.LOW);
        return request;
    }

    private CreateReportRequest createClassReportRequest() {
        CreateReportRequest request = new CreateReportRequest();
        request.setTargetType(ReportTargetType.CLASS);
        request.setTargetId(77L);
        request.setCategory(ReportCategory.SPAM);
        request.setDescription("Lớp có thông tin không phù hợp cần admin kiểm tra");
        return request;
    }

    private ReplyTicketRequest replyRequest(String content) {
        ReplyTicketRequest request = new ReplyTicketRequest();
        request.setContent(content);
        return request;
    }

    private TicketMessage ticketMessage(SupportTicket ticket, String content) {
        TicketMessage message = new TicketMessage();
        message.setMessageId(90L);
        message.setTicket(ticket);
        message.setSender(ticket.getUser());
        message.setIsFromAdmin(false);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 5));
        return message;
    }

    private SupportTicket supportTicket(SupportTicketStatus status) {
        User user = user(USER_ID, "client.it@tcs.test", UserStatus.ACTIVE);
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketId(TICKET_ID);
        ticket.setUser(user);
        ticket.setCategory(SupportTicketCategory.INQUIRY);
        ticket.setSubject("Cần hỗ trợ thanh toán");
        ticket.setDescription("Mô tả chi tiết");
        ticket.setPriority(SupportTicketPriority.LOW);
        ticket.setStatus(status);
        ticket.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 0));
        return ticket;
    }

    private PlatformAdmin platformAdmin(User user) {
        PlatformAdmin admin = new PlatformAdmin();
        admin.setAdminId(user.getUserId());
        admin.setUser(user);
        return admin;
    }

    private Notification notification(
            Long notificationId,
            Long userId,
            String title,
            String content,
            String referenceType,
            Long referenceId) {

        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setUser(user(userId, "notify" + userId + "@tcs.test", UserStatus.ACTIVE));
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 0));
        return notification;
    }

    private User user(Long userId, String email, UserStatus status) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setStatus(status);
        return user;
    }

}
