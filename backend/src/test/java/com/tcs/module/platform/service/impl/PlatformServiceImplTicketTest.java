package com.tcs.module.platform.service.impl;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.CloseTicketRequest;
import com.tcs.module.platform.dto.request.RespondTicketRequest;
import com.tcs.module.platform.dto.request.UpdateTicketRequest;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
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
    private NotificationDispatchService notificationDispatchService;
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

    @ParameterizedTest
    @CsvSource({"LOW,48", "MEDIUM,24", "HIGH,12", "URGENT,4"})
    @DisplayName("updateTicket: priority mới tính lại dueAt từ createdAt")
    void updateTicket_RecalculatesDueAtFromCreatedAt(SupportTicketPriority priority, int expectedHours) {
        ticket.setPriority(priority == SupportTicketPriority.LOW
                ? SupportTicketPriority.HIGH
                : SupportTicketPriority.LOW);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setPriority(priority);

        platformService.updateTicket(TICKET_ID, request);

        assertEquals(priority, ticket.getPriority());
        assertEquals(ticket.getCreatedAt().plusHours(expectedHours), ticket.getDueAt());
        assertFalse(ticket.getSlaBreached());
        verify(auditLogService).record(
                eq("UPDATE_TICKET"), eq("SupportTicket"), eq(TICKET_ID), any(), any());
    }

    @Test
    @DisplayName("updateTicket: priority khẩn cấp quá hạn cập nhật slaBreached")
    void updateTicket_SetsSlaBreached() {
        ticket.setPriority(SupportTicketPriority.LOW);
        ticket.setCreatedAt(LocalDateTime.now().minusHours(10));
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setPriority(SupportTicketPriority.URGENT);
        platformService.updateTicket(TICKET_ID, request);

        assertTrue(ticket.getSlaBreached());
        assertEquals(ticket.getCreatedAt().plusHours(4), ticket.getDueAt());
    }

    @Test
    @DisplayName("updateTicket: đổi category không thay dueAt")
    void updateTicket_CategoryOnlyKeepsDueAt() {
        LocalDateTime originalDueAt = ticket.getDueAt();
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setCategory(SupportTicketCategory.INQUIRY);
        platformService.updateTicket(TICKET_ID, request);

        assertEquals(SupportTicketCategory.INQUIRY, ticket.getCategory());
        assertEquals(originalDueAt, ticket.getDueAt());
    }

    @Test
    @DisplayName("updateTicket: audit lưu category, priority và dueAt trước/sau")
    void updateTicket_AuditsBeforeAndAfterValues() {
        LocalDateTime originalDueAt = ticket.getDueAt();
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setCategory(SupportTicketCategory.INQUIRY);
        request.setPriority(SupportTicketPriority.URGENT);

        platformService.updateTicket(TICKET_ID, request);

        ArgumentCaptor<Object> oldValue = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> newValue = ArgumentCaptor.forClass(Object.class);
        verify(auditLogService).record(
                eq("UPDATE_TICKET"), eq("SupportTicket"), eq(TICKET_ID),
                oldValue.capture(), newValue.capture());
        Map<?, ?> before = (Map<?, ?>) oldValue.getValue();
        Map<?, ?> after = (Map<?, ?>) newValue.getValue();
        assertEquals(SupportTicketCategory.BUG_REPORT, before.get("category"));
        assertEquals(SupportTicketPriority.HIGH, before.get("priority"));
        assertEquals(originalDueAt, before.get("dueAt"));
        assertEquals(SupportTicketCategory.INQUIRY, after.get("category"));
        assertEquals(SupportTicketPriority.URGENT, after.get("priority"));
        assertEquals(ticket.getCreatedAt().plusHours(4), after.get("dueAt"));
    }

    @Test
    @DisplayName("updateTicket: từ chối request rỗng hoặc không thay đổi")
    void updateTicket_RejectsEmptyAndNoOpRequests() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalArgumentException.class,
                () -> platformService.updateTicket(TICKET_ID, new UpdateTicketRequest()));

        UpdateTicketRequest noOp = new UpdateTicketRequest();
        noOp.setCategory(ticket.getCategory());
        noOp.setPriority(ticket.getPriority());
        assertThrows(IllegalArgumentException.class,
                () -> platformService.updateTicket(TICKET_ID, noOp));
        verify(supportTicketRepository, never()).save(any());
        verify(auditLogService, never()).record(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateTicket: từ chối ticket đã kết thúc")
    void updateTicket_RejectsTerminatedTicket() {
        ticket.setStatus(SupportTicketStatus.CLOSED);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setPriority(SupportTicketPriority.URGENT);

        assertThrows(IllegalArgumentException.class,
                () -> platformService.updateTicket(TICKET_ID, request));
        verify(supportTicketRepository, never()).save(any());
    }

    @Test
    @DisplayName("getTicketDetail: trả về đầy đủ thông tin ticket bao gồm dueAt, slaBreached, responseSlaMs")
    void getTicketDetail_Success() {
        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        ticket.setSlaBreached(true);
        ticket.setResponseSlaMs(45000L);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));
        when(ticketMessageRepository.findByTicket_TicketIdOrderByCreatedAtAsc(TICKET_ID)).thenReturn(java.util.List.of());

        SupportTicketDetailResponse response = platformService.getTicketDetail(TICKET_ID);

        assertNotNull(response);
        assertEquals(TICKET_ID, response.getTicketId());
        assertEquals(ticket.getDueAt(), response.getDueAt());
        assertEquals(Boolean.TRUE, response.getSlaBreached());
        assertEquals(45000L, response.getResponseSlaMs());
        assertEquals(SupportTicketStatus.IN_PROGRESS, response.getStatus());
        assertEquals(platformAdmin.getAdminId(), response.getAssignedAdminId());
    }
}
