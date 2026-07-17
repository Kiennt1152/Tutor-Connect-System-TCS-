package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import java.util.List;

public interface MessagingService {

    List<NotificationResponse> getMyNotifications();

    void markAsRead(Long notificationId);

    SupportTicketResponse createSupportTicket(CreateSupportTicketRequest request);

    /** Danh sách yêu cầu hỗ trợ do người dùng hiện tại tạo, mới nhất trước. */
    List<SupportTicketResponse> getMySupportTickets();

    /** Chi tiết yêu cầu hỗ trợ kèm toàn bộ hội thoại (bao gồm phản hồi của admin). Chỉ chủ ticket được xem. */
    SupportTicketDetailResponse getMySupportTicketDetail(Long ticketId);

    ReportResponse createReport(CreateReportRequest request);
}
