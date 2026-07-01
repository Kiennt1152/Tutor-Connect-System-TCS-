package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import java.util.List;

public interface MessagingService {

    List<NotificationResponse> getMyNotifications();

    void markAsRead(Long notificationId);

    SupportTicketResponse createSupportTicket(CreateSupportTicketRequest request);

    ReportResponse createReport(CreateReportRequest request);
}
