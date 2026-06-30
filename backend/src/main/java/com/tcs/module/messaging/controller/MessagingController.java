package com.tcs.module.messaging.controller;

import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.service.MessagingService;
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

    @PostMapping("/support-tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketResponse createSupportTicket(@RequestBody CreateSupportTicketRequest request) {
        return messagingService.createSupportTicket(request);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@RequestBody CreateReportRequest request) {
        return messagingService.createReport(request);
    }
}
