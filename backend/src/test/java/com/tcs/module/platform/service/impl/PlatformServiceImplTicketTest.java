package com.tcs.module.platform.service.impl;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.platform.dto.request.CloseTicketRequest;
import com.tcs.module.platform.dto.request.RespondTicketRequest;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.TicketMessage;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.Optional;
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
class PlatformServiceImplTicketTest {

    private static final Long ADMIN_USER_ID = 10L;
    private static final Long ADMIN_PROFILE_ID = 2L;
    private static final Long TICKET_ID = 5L;

    @Mock
    private SupportTicketRepository supportTicketRepository;
    @Mock
    private PlatformAdminRepository platformAdminRepository;
    @Mock
    private TicketMessageRepository ticketMessageRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AuthHelper authHelper;

    @InjectMocks
    private PlatformServiceImpl platformService;

    private User adminUser;
    private PlatformAdmin platformAdmin;
    private SupportTicket ticket;
    private User ticketUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(ADMIN_USER_ID);
        adminUser.setEmail("admin@example.com");

        platformAdmin = new PlatformAdmin();
        platformAdmin.setAdminId(ADMIN_PROFILE_ID);
        platformAdmin.setUser(adminUser);

        ticketUser = new User();
        ticketUser.setUserId(100L);
        ticketUser.setEmail("user@example.com");

        ticket = new SupportTicket();
        ticket.setTicketId(TICKET_ID);
        ticket.setUser(ticketUser);
        ticket.setCategory(SupportTicketCategory.BUG_REPORT);
        ticket.setSubject("Lỗi hiển thị");
        ticket.setDescription("Mô tả lỗi");
        ticket.setPriority(SupportTicketPriority.HIGH);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now().minusHours(1));
        ticket.setDueAt(LocalDateTime.now().plusHours(11));
    }

    @Test
    @DisplayName("respondToTicket: admin phản hồi ticket, gán admin, lưu TicketMessage và chuyển status thành IN_REVIEW")
    void respondToTicket_Success() {
        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        RespondTicketRequest req = new RespondTicketRequest();
        req.setContent("Chúng tôi đã tiếp nhận và đang sửa lỗi");

        SupportTicketDetailResponse response = platformService.respondToTicket(TICKET_ID, req);

        assertNotNull(response);
        assertEquals(SupportTicketStatus.IN_REVIEW, ticket.getStatus());
        assertEquals(platformAdmin, ticket.getAssignedAdmin());
        assertNotNull(ticket.getResponseSlaMs());
        verify(ticketMessageRepository, times(1)).save(any(TicketMessage.class));
        verify(auditLogService, times(1)).record(eq("RESPOND_TICKET"), eq("SupportTicket"), eq(TICKET_ID), any(), eq(req));
    }

    @Test
    @DisplayName("closeTicket: đóng ticket thành công với trạng thái RESOLVED")
    void closeTicket_Resolved_Success() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        CloseTicketRequest req = new CloseTicketRequest();
        req.setStatus(SupportTicketStatus.RESOLVED);
        req.setAdminNotes("Đã xử lý xong");

        SupportTicketDetailResponse response = platformService.closeTicket(TICKET_ID, req);

        assertNotNull(response);
        assertEquals(SupportTicketStatus.RESOLVED, ticket.getStatus());
        assertNotNull(ticket.getResolvedAt());
        verify(supportTicketRepository, times(1)).save(ticket);
    }
}
