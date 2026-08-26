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
import java.util.List;
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

/**
 * ============================================================================
 * KIỂM THỬ TỰ ĐỘNG QUẢN LÝ SUPPORT TICKET & SLA (UNIT TEST TICKET SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các ca kiểm thử:
 *   - Kiểm tra lấy chi tiết Ticket và tự động phân công PIC Admin (Auto-Assignment).
 *   - Kiểm tra gửi phản hồi Ticket và đo lường First Response SLA.
 *   - Kiểm tra cập nhật Category/Priority và tính toán lại Due Date.
 *   - Kiểm tra đóng Ticket (RESOLVED / CLOSED) và gửi thông báo.
 *   - Kiểm tra quét định kỳ và thăng cấp tự động khi vi phạm SLA (SLA Escalation Scanner).
 *   - Kiểm tra gộp Ticket (Merge Ticket) và chuyển tiếp tranh chấp (Dispute Redirect).
 */
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
    @Mock
    private com.tcs.module.marketplace.repository.TutoringClassRepository tutoringClassRepository;
    @Mock
    private com.tcs.module.platform.repository.ReportRepository reportRepository;

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

    @Test
    @DisplayName("mergeTicket: gộp ticket nguồn vào ticket đích thành công (BF09-TC03)")
    void mergeTicket_Success() {
        Long targetTicketId = 1L;
        SupportTicket targetTicket = new SupportTicket();
        targetTicket.setTicketId(targetTicketId);
        targetTicket.setUser(ticketUser);
        targetTicket.setSubject("Yêu cầu gốc");
        targetTicket.setStatus(SupportTicketStatus.IN_PROGRESS);

        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.findById(targetTicketId)).thenReturn(Optional.of(targetTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        com.tcs.module.platform.dto.request.MergeTicketRequest request = new com.tcs.module.platform.dto.request.MergeTicketRequest();
        request.setTargetTicketId(targetTicketId);
        request.setReason("Trùng vấn đề nạp tiền");

        SupportTicketDetailResponse response = platformService.mergeTicket(TICKET_ID, request);

        assertNotNull(response);
        assertEquals(SupportTicketStatus.CLOSED, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());

        // Verify message was created in target ticket
        ArgumentCaptor<TicketMessage> msgCaptor = ArgumentCaptor.forClass(TicketMessage.class);
        verify(ticketMessageRepository).save(msgCaptor.capture());
        TicketMessage savedMsg = msgCaptor.getValue();
        assertEquals(targetTicket, savedMsg.getTicket());
        assertTrue(savedMsg.getContent().contains("GỘP TICKET"));
        assertTrue(savedMsg.getContent().contains(ticket.getSubject()));
        assertTrue(savedMsg.getContent().contains("Trùng vấn đề nạp tiền"));

        // Verify audit log
        verify(auditLogService).record(eq("MERGE_TICKET"), eq("SupportTicket"), eq(TICKET_ID), any(), any());

        // Verify notification
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(ticketUser), any(), eq("SUPPORT_TICKET_RESPONSE"), any(), any(), any(), eq("SUPPORT_TICKET"), eq(TICKET_ID));
    }

    @Test
    @DisplayName("mergeTicket: từ chối khi gộp ticket vào chính nó")
    void mergeTicket_RejectsSameTicket() {
        com.tcs.module.platform.dto.request.MergeTicketRequest request = new com.tcs.module.platform.dto.request.MergeTicketRequest();
        request.setTargetTicketId(TICKET_ID);

        assertThrows(IllegalArgumentException.class, () -> platformService.mergeTicket(TICKET_ID, request));
        verify(supportTicketRepository, never()).save(any());
    }

    @Test
    @DisplayName("mergeTicket: từ chối khi gộp ticket của 2 người dùng khác nhau")
    void mergeTicket_RejectsDifferentUsers() {
        Long targetTicketId = 1L;
        User otherUser = new User();
        otherUser.setUserId(999L);
        otherUser.setEmail("other@example.com");

        SupportTicket targetTicket = new SupportTicket();
        targetTicket.setTicketId(targetTicketId);
        targetTicket.setUser(otherUser);
        targetTicket.setStatus(SupportTicketStatus.OPEN);

        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.findById(targetTicketId)).thenReturn(Optional.of(targetTicket));

        com.tcs.module.platform.dto.request.MergeTicketRequest request = new com.tcs.module.platform.dto.request.MergeTicketRequest();
        request.setTargetTicketId(targetTicketId);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> platformService.mergeTicket(TICKET_ID, request));
        assertEquals("Chỉ có thể gộp các ticket của cùng một người dùng", ex.getMessage());
        verify(ticketMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("mergeTicket: từ chối khi ticket đích đã bị đóng")
    void mergeTicket_RejectsAlreadyClosedTargetTicket() {
        Long targetTicketId = 1L;
        SupportTicket targetTicket = new SupportTicket();
        targetTicket.setTicketId(targetTicketId);
        targetTicket.setUser(ticketUser);
        targetTicket.setStatus(SupportTicketStatus.CLOSED);

        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.findById(targetTicketId)).thenReturn(Optional.of(targetTicket));

        com.tcs.module.platform.dto.request.MergeTicketRequest request = new com.tcs.module.platform.dto.request.MergeTicketRequest();
        request.setTargetTicketId(targetTicketId);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> platformService.mergeTicket(TICKET_ID, request));
        assertEquals("Không thể gộp vào ticket đích đã bị đóng", ex.getMessage());
        verify(ticketMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("scanAndEscalateSlaBreaches: tự động nâng độ ưu tiên và gửi nhắc nhở khi quá hạn SLA (BF09-TC02)")
    void scanAndEscalateSlaBreaches_Success() {
        ticket.setPriority(SupportTicketPriority.LOW);
        ticket.setSlaBreached(false);
        ticket.setAssignedAdmin(platformAdmin);

        when(supportTicketRepository.findBreachedCandidateTickets(any(), any()))
                .thenReturn(List.of(ticket));
        when(platformAdminRepository.findAll()).thenReturn(List.of(platformAdmin));

        int count = platformService.scanAndEscalateSlaBreaches();

        assertEquals(1, count);
        assertTrue(ticket.getSlaBreached());
        assertEquals(SupportTicketPriority.MEDIUM, ticket.getPriority());

        // Verify save
        verify(supportTicketRepository).save(ticket);

        // Verify audit log
        verify(auditLogService).record(
                eq("SLA_BREACH_ESCALATION"), eq("SupportTicket"), eq(TICKET_ID), any(), any());

        // Verify admin reminder notification
        verify(notificationDispatchService).notifyUser(
                eq(adminUser), any(), contains("Cảnh báo quá hạn SLA"), any(), eq("SUPPORT_TICKET"), eq(TICKET_ID));

        // Verify user progress notification
        verify(notificationDispatchService).notifyUser(
                eq(ticketUser), any(), contains("Cập nhật tiến độ"), any(), eq("SUPPORT_TICKET"), eq(TICKET_ID));
    }

    @Test
    @DisplayName("scanAndEscalateSlaBreaches: nâng HIGH lên URGENT và gửi broadcast khi chưa gán admin")
    void scanAndEscalateSlaBreaches_HighToUrgentBroadcast() {
        ticket.setPriority(SupportTicketPriority.HIGH);
        ticket.setSlaBreached(false);
        ticket.setAssignedAdmin(null);

        when(supportTicketRepository.findBreachedCandidateTickets(any(), any()))
                .thenReturn(List.of(ticket));
        when(platformAdminRepository.findAll()).thenReturn(List.of(platformAdmin));

        int count = platformService.scanAndEscalateSlaBreaches();

        assertEquals(1, count);
        assertTrue(ticket.getSlaBreached());
        assertEquals(SupportTicketPriority.URGENT, ticket.getPriority());

        // Verify admin broadcast notification
        verify(notificationDispatchService).notifyUser(
                eq(adminUser), any(), contains("Cảnh báo quá hạn SLA"), any(), eq("SUPPORT_TICKET"), eq(TICKET_ID));
    }

    @Test
    @DisplayName("scanAndEscalateSlaBreaches: trả về 0 khi không có ticket nào quá hạn")
    void scanAndEscalateSlaBreaches_NoBreaches() {
        when(supportTicketRepository.findBreachedCandidateTickets(any(), any()))
                .thenReturn(List.of());

        int count = platformService.scanAndEscalateSlaBreaches();

        assertEquals(0, count);
        verify(supportTicketRepository, never()).save(any());
        verify(notificationDispatchService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("redirectTicketToDispute: chuyển đổi category thành DISPUTE, nâng độ ưu tiên lên HIGH và gửi thông báo (BF09-TC07)")
    void redirectTicketToDispute_Success() {
        ticket.setCategory(SupportTicketCategory.INQUIRY);
        ticket.setPriority(SupportTicketPriority.LOW);
        ticket.setStatus(SupportTicketStatus.OPEN);

        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        com.tcs.module.platform.dto.request.RedirectDisputeRequest req = new com.tcs.module.platform.dto.request.RedirectDisputeRequest();
        req.setNotes("Chuyển sang hòa giải lớp học");

        SupportTicketDetailResponse res = platformService.redirectTicketToDispute(TICKET_ID, req);

        assertNotNull(res);
        assertEquals(SupportTicketCategory.DISPUTE, ticket.getCategory());
        assertEquals(SupportTicketPriority.HIGH, ticket.getPriority());

        // Verify message created
        verify(ticketMessageRepository).save(any());

        // Verify audit log
        verify(auditLogService).record(eq("REDIRECT_TICKET_TO_DISPUTE"), eq("SupportTicket"), eq(TICKET_ID), any(), any());

        // Verify user notified
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(ticketUser), any(), eq("SUPPORT_TICKET_RESPONSE"), any(), any(), any(), eq("SUPPORT_TICKET"), eq(TICKET_ID));
    }

    @Test
    @DisplayName("redirectTicketToDispute: tạo bản ghi Report khi có targetClassId")
    void redirectTicketToDispute_WithClassCreatesReport() {
        Long classId = 99L;
        com.tcs.module.marketplace.entity.TutoringClass tutoringClass = new com.tcs.module.marketplace.entity.TutoringClass();
        tutoringClass.setClassId(classId);
        tutoringClass.setTitle("Toán 12");

        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(tutoringClassRepository.findById(classId)).thenReturn(Optional.of(tutoringClass));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        com.tcs.module.platform.dto.request.RedirectDisputeRequest req = new com.tcs.module.platform.dto.request.RedirectDisputeRequest();
        req.setTargetClassId(classId);
        req.setNotes("Khiếu nại không hoàn tiền");

        platformService.redirectTicketToDispute(TICKET_ID, req);

        assertEquals(tutoringClass, ticket.getTargetClass());
        org.mockito.ArgumentCaptor<com.tcs.module.platform.entity.Report> reportCaptor = org.mockito.ArgumentCaptor.forClass(com.tcs.module.platform.entity.Report.class);
        verify(reportRepository).save(reportCaptor.capture());
        com.tcs.module.platform.entity.Report savedReport = reportCaptor.getValue();
        assertNotNull(savedReport);
        assertEquals(com.tcs.module.platform.enums.ReportCategory.OTHER, savedReport.getCategory());
        assertEquals(com.tcs.module.platform.enums.ReportTargetType.CLASS, savedReport.getTargetType());
        assertEquals(classId, savedReport.getTargetId());
        assertEquals(com.tcs.module.platform.enums.ReportStatus.PENDING, savedReport.getStatus());
    }

    @Test
    @DisplayName("redirectTicketToDispute: từ chối khi ticket đã bị đóng")
    void redirectTicketToDispute_RejectsClosedTicket() {
        ticket.setStatus(SupportTicketStatus.CLOSED);

        com.tcs.security.UserPrincipal principal = new com.tcs.security.UserPrincipal(adminUser, UserRole.PLATFORM_ADMIN);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN)).thenReturn(principal);
        when(platformAdminRepository.findByUser_UserId(ADMIN_USER_ID)).thenReturn(Optional.of(platformAdmin));
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> platformService.redirectTicketToDispute(TICKET_ID, new com.tcs.module.platform.dto.request.RedirectDisputeRequest()));
        assertEquals("Không thể chuyển tiếp ticket đã bị đóng", ex.getMessage());
        verify(ticketMessageRepository, never()).save(any());
    }
}
