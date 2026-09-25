package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.request.ReplyTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import java.util.List;

public interface MessagingService {

    /**
     * [UC-51] Lấy danh sách thông báo của người dùng đang đăng nhập.
     * 
     * @return Danh sách thông báo {@link NotificationResponse}
     */
    List<NotificationResponse> getMyNotifications();

    /**
     * [UC-51] Đánh dấu thông báo là đã đọc.
     * 
     * @param notificationId ID thông báo cần cập nhật
     */
    void markAsRead(Long notificationId);

    /**
     * [UC-65, UC-66] Tiếp nhận và tạo phiếu yêu cầu hỗ trợ kỹ thuật mới.
     * 
     * @param request Dữ liệu tạo ticket {@link CreateSupportTicketRequest}
     * @return Thông tin ticket vừa tạo {@link SupportTicketResponse}
     */
    SupportTicketResponse createSupportTicket(CreateSupportTicketRequest request);

    /**
     * [UC-66] Lấy danh sách yêu cầu hỗ trợ do người dùng hiện tại tạo, mới nhất trước.
     * 
     * @return Danh sách ticket hỗ trợ {@link SupportTicketResponse}
     */
    List<SupportTicketResponse> getMySupportTickets();

    /**
     * [UC-66] Chi tiết yêu cầu hỗ trợ kèm toàn bộ hội thoại (chỉ chủ ticket được xem).
     * 
     * @param ticketId ID ticket hỗ trợ
     * @return Chi tiết ticket {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse getMySupportTicketDetail(Long ticketId);

    /**
     * [UC-66] Gửi thêm tin nhắn phản hồi / bổ sung bằng chứng vào ticket hỗ trợ đang mở.
     * 
     * @param ticketId ID ticket hỗ trợ
     * @param request Nội dung và bằng chứng phản hồi {@link ReplyTicketRequest}
     * @return Tin nhắn đã lưu {@link TicketMessageResponse}
     */
    TicketMessageResponse replySupportTicket(Long ticketId, ReplyTicketRequest request);

    /**
     * [UC-66] Mở lại ticket hỗ trợ đã đóng/đã giải quyết (không kèm lý do bổ sung).
     * 
     * @param ticketId ID ticket cần mở lại
     * @return Chi tiết ticket sau khi mở lại {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse reopenSupportTicket(Long ticketId);

    /**
     * [UC-66] Mở lại ticket hỗ trợ đã đóng kèm lý do và tài liệu bổ sung.
     * 
     * @param ticketId ID ticket cần mở lại
     * @param request Lý do và bằng chứng mở lại {@link ReplyTicketRequest}
     * @return Chi tiết ticket sau khi mở lại {@link SupportTicketDetailResponse}
     */
    SupportTicketDetailResponse reopenSupportTicket(Long ticketId, ReplyTicketRequest request);

    /**
     * [UC-49] Tạo mới báo cáo vi phạm đối với đối tượng cụ thể (USER, CLASS, REVIEW).
     * 
     * @param request Thông tin báo cáo vi phạm {@link CreateReportRequest}
     * @return Bản ghi báo cáo vi phạm {@link ReportResponse}
     */
    ReportResponse createReport(CreateReportRequest request);
}
