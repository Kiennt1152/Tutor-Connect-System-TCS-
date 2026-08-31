package com.tcs.module.messaging.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.request.ReplyTicketRequest;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.TicketMessage;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long TICKET_ID = 1L;

    @Mock
    private AuthHelper authHelper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SupportTicketRepository supportTicketRepository;
    @Mock
    private TicketMessageRepository ticketMessageRepository;
    @Mock
    private TutoringClassRepository tutoringClassRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDispatchService notificationDispatchService;
    @Mock
    private PlatformAdminRepository platformAdminRepository;
    @Mock
    private com.tcs.module.platform.repository.ReportRepository reportRepository;

    @InjectMocks
    private MessagingServiceImpl messagingService;

    private User user;
    private SupportTicket ticket;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);

        ticket = new SupportTicket();
        ticket.setTicketId(TICKET_ID);
        ticket.setUser(user);
        ticket.setCategory(SupportTicketCategory.INQUIRY);
        ticket.setSubject("Cần hỗ trợ thanh toán");
        ticket.setDescription("Mô tả chi tiết");
        ticket.setPriority(SupportTicketPriority.LOW);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("createSupportTicket: tính toán dueAt và đặt slaBreached = false thành công")
    void createSupportTicket_Success() {
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> {
            SupportTicket t = i.getArgument(0);
            t.setTicketId(TICKET_ID);
            return t;
        });
        User activeAdminUser = new User();
        activeAdminUser.setUserId(200L); activeAdminUser.setEmail("admin@example.com"); activeAdminUser.setStatus(UserStatus.ACTIVE);
        User suspendedAdminUser = new User();
        suspendedAdminUser.setUserId(201L); suspendedAdminUser.setEmail("disabled@example.com"); suspendedAdminUser.setStatus(UserStatus.SUSPENDED);
        PlatformAdmin activeAdmin = new PlatformAdmin(); activeAdmin.setUser(activeAdminUser);
        PlatformAdmin suspendedAdmin = new PlatformAdmin(); suspendedAdmin.setUser(suspendedAdminUser);
        when(platformAdminRepository.findAll()).thenReturn(List.of(activeAdmin, suspendedAdmin));

        CreateSupportTicketRequest req = new CreateSupportTicketRequest();
        req.setCategory(SupportTicketCategory.INQUIRY);
        req.setSubject("Lỗi hệ thống");
        req.setPriority(SupportTicketPriority.HIGH);

        SupportTicketResponse response = messagingService.createSupportTicket(req);

        assertNotNull(response);
        assertEquals(TICKET_ID, response.getTicketId());
        assertNotNull(response.getDueAt());
        assertEquals(false, response.getSlaBreached());
        verify(supportTicketRepository, times(1)).save(any(SupportTicket.class));
        verify(ticketMessageRepository, times(1)).save(any(TicketMessage.class));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(activeAdminUser), eq(NotificationType.SYSTEM), eq("SUPPORT_TICKET_CREATED"),
                any(Map.class), eq("Yêu cầu hỗ trợ mới #1"), anyString(), eq("SUPPORT_TICKET"), eq(TICKET_ID));
        verify(notificationDispatchService, times(1)).notifyUserFromTemplate(
                any(), any(), anyString(), any(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("replySupportTicket: chuyển status từ IN_REVIEW sang OPEN và lưu tin nhắn")
    void replySupportTicket_InReviewToOpen() {
        ticket.setStatus(SupportTicketStatus.IN_REVIEW);

        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(i -> {
            TicketMessage m = i.getArgument(0);
            m.setMessageId(50L);
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });

        ReplyTicketRequest req = new ReplyTicketRequest();
        req.setContent("Tôi đã cung cấp thêm thông tin");

        TicketMessageResponse response = messagingService.replySupportTicket(TICKET_ID, req);

        assertNotNull(response);
        assertEquals("Tôi đã cung cấp thêm thông tin", response.getContent());
        assertEquals(SupportTicketStatus.OPEN, ticket.getStatus());
        verify(supportTicketRepository, times(1)).save(ticket);
    }

    @Test
    @DisplayName("replySupportTicket: từ chối khi khác người sở hữu ticket (IDOR guard)")
    void replySupportTicket_ForbiddenForOtherUser() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(authHelper.currentUserId()).thenReturn(OTHER_USER_ID);

        ReplyTicketRequest req = new ReplyTicketRequest();
        req.setContent("Thử trả lời ticket người khác");

        assertThrows(ForbiddenException.class, () -> messagingService.replySupportTicket(TICKET_ID, req));
    }

    @Test
    @DisplayName("replySupportTicket: từ chối khi ticket đã CLOSED")
    void replySupportTicket_ClosedTicket_ThrowsException() {
        ticket.setStatus(SupportTicketStatus.CLOSED);

        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(authHelper.currentUserId()).thenReturn(USER_ID);

        ReplyTicketRequest req = new ReplyTicketRequest();
        req.setContent("Trả lời ticket đã đóng");

        assertThrows(IllegalArgumentException.class, () -> messagingService.replySupportTicket(TICKET_ID, req));
    }

    @Test
    @DisplayName("reopenSupportTicket: chỉ thành công khi ticket là RESOLVED hoặc CLOSED, lưu message và gửi thông báo cho admin & user")
    void reopenSupportTicket_Success() {
        ticket.setStatus(SupportTicketStatus.RESOLVED);

        User activeAdminUser = new User();
        activeAdminUser.setUserId(200L); activeAdminUser.setEmail("admin@example.com"); activeAdminUser.setStatus(UserStatus.ACTIVE);
        PlatformAdmin activeAdmin = new PlatformAdmin(); activeAdmin.setUser(activeAdminUser);

        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));
        when(platformAdminRepository.findAll()).thenReturn(List.of(activeAdmin));

        ReplyTicketRequest req = new ReplyTicketRequest();
        req.setContent("Vấn đề vẫn chưa được khắc phục triệt để");

        SupportTicketDetailResponse response = messagingService.reopenSupportTicket(TICKET_ID, req);

        assertNotNull(response);
        assertEquals(SupportTicketStatus.OPEN, ticket.getStatus());
        assertNotNull(ticket.getDueAt());
        assertEquals(false, ticket.getSlaBreached());
        assertNull(ticket.getResolvedAt());

        // Verify ticket message saved
        verify(ticketMessageRepository, times(1)).save(any(TicketMessage.class));

        // Verify admin notification
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(activeAdminUser), eq(NotificationType.SYSTEM), eq("SUPPORT_TICKET_REOPENED"),
                any(Map.class), eq("Yêu cầu hỗ trợ #1 đã được mở lại"), anyString(), eq("SUPPORT_TICKET"), eq(TICKET_ID));

        // Verify user notification
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(user), eq(NotificationType.SYSTEM), eq("SUPPORT_TICKET_REOPENED_USER"),
                any(Map.class), eq("Yêu cầu hỗ trợ #1 đã mở lại"), anyString(), eq("SUPPORT_TICKET"), eq(TICKET_ID));
    }

    @Test
    @DisplayName("reopenSupportTicket: thất bại khi ticket đang OPEN")
    void reopenSupportTicket_AlreadyOpen_ThrowsException() {
        ticket.setStatus(SupportTicketStatus.OPEN);

        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(authHelper.currentUserId()).thenReturn(USER_ID);

        assertThrows(IllegalArgumentException.class, () -> messagingService.reopenSupportTicket(TICKET_ID));
    }

    // ===================================================================
    //  Sheet: markNotificationAsRead
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("markNotificationAsRead")
    class MarkNotificationAsRead {

        private static final Long NOTI_ID = 55L;

        private com.tcs.module.messaging.entity.Notification notification(User owner) {
            var n = new com.tcs.module.messaging.entity.Notification();
            n.setUser(owner);
            n.setIsRead(false);
            return n;
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - thong bao cua chinh minh -> danh dau da doc va luu")
        void utcid01_markOwnNotification() {
            var n = notification(user);
            when(notificationRepository.findById(NOTI_ID)).thenReturn(java.util.Optional.of(n));
            when(authHelper.currentUserId()).thenReturn(USER_ID);

            messagingService.markAsRead(NOTI_ID);

            org.junit.jupiter.api.Assertions.assertTrue(n.getIsRead());
            org.junit.jupiter.api.Assertions.assertNotNull(n.getReadAt());
            verify(notificationRepository).save(n);
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (A) - thong bao cua nguoi khac -> 'Không có quyền cập nhật thông báo này'")
        void utcid02_notOwner() {
            User other = new User();
            other.setUserId(OTHER_USER_ID);
            when(notificationRepository.findById(NOTI_ID)).thenReturn(java.util.Optional.of(notification(other)));
            when(authHelper.currentUserId()).thenReturn(USER_ID);

            var ex = assertThrows(com.tcs.exception.ForbiddenException.class,
                    () -> messagingService.markAsRead(NOTI_ID));
            assertEquals("Không có quyền cập nhật thông báo này", ex.getMessage());
            verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - notificationId khong ton tai -> 'Không tìm thấy thông báo'")
        void utcid03_notFound() {
            when(notificationRepository.findById(NOTI_ID)).thenReturn(java.util.Optional.empty());

            var ex = assertThrows(com.tcs.exception.ResourceNotFoundException.class,
                    () -> messagingService.markAsRead(NOTI_ID));
            assertEquals("Không tìm thấy thông báo", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: createReport
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("createReport")
    class CreateReport {

        private static final Long TARGET_ID = 777L;

        private com.tcs.module.messaging.dto.request.CreateReportRequest req(String description, Long targetId) {
            var r = new com.tcs.module.messaging.dto.request.CreateReportRequest();
            r.setTargetType(com.tcs.module.platform.enums.ReportTargetType.USER);
            r.setTargetId(targetId);
            r.setCategory(com.tcs.module.platform.enums.ReportCategory.OTHER);
            r.setDescription(description);
            return r;
        }

        private void givenReporter() {
            when(authHelper.currentUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        }

        private void givenUnderDailyLimit(long count) {
            when(reportRepository.countByReporter_UserIdAndCreatedAtAfter(
                    org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(count);
        }

        private void givenTargetUserExists() {
            when(userRepository.findById(TARGET_ID)).thenReturn(java.util.Optional.of(new User()));
        }

        private void givenNoPendingReport() {
            when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                    USER_ID, com.tcs.module.platform.enums.ReportTargetType.USER, TARGET_ID,
                    com.tcs.module.platform.enums.ReportStatus.PENDING)).thenReturn(java.util.List.of());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - du thong tin, mo ta du dai, chua vuot han muc -> tao bao cao")
        void utcid01_createSuccessfully() {
            givenReporter();
            givenUnderDailyLimit(0);
            givenTargetUserExists();
            givenNoPendingReport();
            when(reportRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

            messagingService.createReport(req("Nguoi nay co hanh vi khong dung muc", TARGET_ID));

            verify(reportRepository).save(org.mockito.ArgumentMatchers.any());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (A) - thieu targetType/targetId/category -> 'targetType, targetId và category là bắt buộc'")
        void utcid02_missingFields() {
            var r = req("Mo ta du dai cho hop le", TARGET_ID);
            r.setTargetId(null);

            var ex = assertThrows(IllegalArgumentException.class, () -> messagingService.createReport(r));
            assertEquals("targetType, targetId và category là bắt buộc", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (B) - mo ta ngan hon 10 ky tu -> 'Mô tả báo cáo phải có ít nhất 10 ký tự.'")
        void utcid03_descriptionTooShort() {
            givenReporter();

            var ex = assertThrows(IllegalArgumentException.class,
                    () -> messagingService.createReport(req("ngan", TARGET_ID)));
            assertEquals("Mô tả báo cáo phải có ít nhất 10 ký tự.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (B) - da gui 5 bao cao trong 24 gio -> 'Bạn đã đạt giới hạn 5 báo cáo trong 24 giờ.'")
        void utcid04_dailyLimitReached() {
            givenReporter();
            givenUnderDailyLimit(5);

            var ex = assertThrows(IllegalArgumentException.class,
                    () -> messagingService.createReport(req("Mo ta du dai cho hop le", TARGET_ID)));
            assertEquals("Bạn đã đạt giới hạn 5 báo cáo trong 24 giờ.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (A) - tu bao cao chinh minh -> 'Không thể báo cáo chính mình.'")
        void utcid05_reportSelf() {
            givenReporter();
            givenUnderDailyLimit(0);

            var ex = assertThrows(IllegalArgumentException.class,
                    () -> messagingService.createReport(req("Mo ta du dai cho hop le", USER_ID)));
            assertEquals("Không thể báo cáo chính mình.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (A) - da co bao cao dang cho cho cung doi tuong -> chan tao trung")
        void utcid06_duplicatePendingReport() {
            givenReporter();
            givenUnderDailyLimit(0);
            givenTargetUserExists();
            when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                    USER_ID, com.tcs.module.platform.enums.ReportTargetType.USER, TARGET_ID,
                    com.tcs.module.platform.enums.ReportStatus.PENDING))
                    .thenReturn(java.util.List.of(new com.tcs.module.platform.entity.Report()));

            var ex = assertThrows(IllegalArgumentException.class,
                    () -> messagingService.createReport(req("Mo ta du dai cho hop le", TARGET_ID)));
            assertEquals("Bạn đã có một báo cáo đang chờ xử lý cho đối tượng này.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID07 (A) - nguoi bi bao cao khong ton tai -> 'Không tìm thấy người dùng bị báo cáo.'")
        void utcid07_targetUserNotFound() {
            givenReporter();
            givenUnderDailyLimit(0);
            when(userRepository.findById(TARGET_ID)).thenReturn(java.util.Optional.empty());

            var ex = assertThrows(com.tcs.exception.ResourceNotFoundException.class,
                    () -> messagingService.createReport(req("Mo ta du dai cho hop le", TARGET_ID)));
            assertEquals("Không tìm thấy người dùng bị báo cáo.", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: replySupportTicket
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("replySupportTicket")
    class ReplySupportTicket {

        private com.tcs.module.messaging.dto.request.ReplyTicketRequest reply() {
            var r = new com.tcs.module.messaging.dto.request.ReplyTicketRequest();
            r.setContent("Em van chua nhan duoc phan hoi ạ");
            return r;
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - chu ticket, ticket con mo -> tao TicketMessage cua nguoi dung")
        void utcid01_replySuccessfully() {
            when(supportTicketRepository.findById(TICKET_ID)).thenReturn(java.util.Optional.of(ticket));
            when(authHelper.currentUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
            when(ticketMessageRepository.save(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(i -> i.getArgument(0));

            messagingService.replySupportTicket(TICKET_ID, reply());

            verify(ticketMessageRepository).save(org.mockito.ArgumentMatchers.any());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (A) - ticket cua nguoi khac -> 'Không có quyền phản hồi yêu cầu hỗ trợ này'")
        void utcid02_notOwner() {
            when(supportTicketRepository.findById(TICKET_ID)).thenReturn(java.util.Optional.of(ticket));
            when(authHelper.currentUserId()).thenReturn(OTHER_USER_ID);

            var ex = assertThrows(com.tcs.exception.ForbiddenException.class,
                    () -> messagingService.replySupportTicket(TICKET_ID, reply()));
            assertEquals("Không có quyền phản hồi yêu cầu hỗ trợ này", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - ticket da dong/da giai quyet -> chan phan hoi")
        void utcid03_ticketClosed() {
            ticket.setStatus(SupportTicketStatus.CLOSED);
            when(supportTicketRepository.findById(TICKET_ID)).thenReturn(java.util.Optional.of(ticket));
            when(authHelper.currentUserId()).thenReturn(USER_ID);

            var ex = assertThrows(IllegalArgumentException.class,
                    () -> messagingService.replySupportTicket(TICKET_ID, reply()));
            assertEquals("Không thể phản hồi yêu cầu hỗ trợ đã đóng hoặc đã giải quyết", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - ticketId khong ton tai -> 'Không tìm thấy yêu cầu hỗ trợ'")
        void utcid04_ticketNotFound() {
            when(supportTicketRepository.findById(TICKET_ID)).thenReturn(java.util.Optional.empty());

            var ex = assertThrows(com.tcs.exception.ResourceNotFoundException.class,
                    () -> messagingService.replySupportTicket(TICKET_ID, reply()));
            assertEquals("Không tìm thấy yêu cầu hỗ trợ", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (A) - khong load duoc nguoi gui -> 'Không tìm thấy người dùng'")
        void utcid05_senderNotFound() {
            when(supportTicketRepository.findById(TICKET_ID)).thenReturn(java.util.Optional.of(ticket));
            when(authHelper.currentUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.empty());

            var ex = assertThrows(com.tcs.exception.ResourceNotFoundException.class,
                    () -> messagingService.replySupportTicket(TICKET_ID, reply()));
            assertEquals("Không tìm thấy người dùng", ex.getMessage());
        }
    }
}
