package com.tcs.module.messaging.service.impl;

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
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.TicketMessage;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52MessagingNotificationITTest {

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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_MSG_006_BlockAnonymousNotificationListBeforeLoadingRecords() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> messagingService.getMyNotifications());

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(notificationRepository, never()).findByUser_UserIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    void SUPPORT_MSG_MarkNotificationAsReadStoresReadTimestampAtServiceLevel() {
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
