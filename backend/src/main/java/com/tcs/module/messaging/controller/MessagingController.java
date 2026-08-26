package com.tcs.module.messaging.controller;

import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.service.MessagingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService messagingService;

    @GetMapping("/notifications")
    public List<NotificationResponse> getMyNotifications() {
        return messagingService.getMyNotifications();
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public Map<String, String> markAsRead(@PathVariable Long notificationId) {
        messagingService.markAsRead(notificationId);
        return Map.of("message", "Đã đánh dấu đã đọc");
    }

    // =========================================================================
    // LUỒNG 3: NGƯỜI DÙNG TẠO TICKET HỖ TRỢ & TỰ ĐỘNG TÍNH HẠN SLA (UC-65, UC-66)
    // =========================================================================

    // Danh sách ticket hỗ trợ của cá nhân người dùng
    @GetMapping("/support-tickets")
    public List<SupportTicketResponse> getMySupportTickets() {
        return messagingService.getMySupportTickets();
    }

    // Xem chi tiết ticket và tiến trình xử lý của Admin
    @GetMapping("/support-tickets/{ticketId}")
    public SupportTicketDetailResponse getMySupportTicketDetail(@PathVariable Long ticketId) {
        return messagingService.getMySupportTicketDetail(ticketId);
    }

    // Luồng 3 - Bước 2: Tiếp nhận yêu cầu tạo mới Ticket hỗ trợ
    @PostMapping("/support-tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketResponse createSupportTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
        return messagingService.createSupportTicket(request);
    }

    // Người dùng gửi thêm tin nhắn phản hồi / bổ sung bằng chứng vào Ticket
    @PostMapping("/support-tickets/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketMessageResponse replySupportTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody com.tcs.module.messaging.dto.request.ReplyTicketRequest request) {
        return messagingService.replySupportTicket(ticketId, request);
    }

    // Người dùng mở lại Ticket nếu sự cố chưa được giải quyết triệt để
    @PostMapping("/support-tickets/{ticketId}/reopen")
    public SupportTicketDetailResponse reopenSupportTicket(
            @PathVariable Long ticketId,
            @RequestBody(required = false) com.tcs.module.messaging.dto.request.ReplyTicketRequest request) {
        return messagingService.reopenSupportTicket(ticketId, request);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@RequestBody CreateReportRequest request) {
        return messagingService.createReport(request);
    }
}
