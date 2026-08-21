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
}
