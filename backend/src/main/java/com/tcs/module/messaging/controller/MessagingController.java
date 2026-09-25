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

/**
 * ====================================================================================================
 * [UC-51] LỊCH SỬ TIN NHẮN & TỆP ĐÍNH KÈM (MESSAGING CONTROLLER)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Truy xuất lịch sử trò chuyện phân trang, hỗ trợ tìm kiếm tin nhắn cũ theo từ khóa.
 * 2. Đánh dấu trạng thái đã nhận, đã xem và đếm số lượng tin nhắn chưa đọc của từng cuộc hội thoại.
 * 3. Quản lý truyền tải tệp đính kèm (hình ảnh bài tập, tài liệu PDF giảng dạy) trong khung chat.
 * * @author Hoàng Minh Đức (mduc1011-swp)
 */
@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService messagingService;

    /**
     * [UC-51] Lấy danh sách thông báo của người dùng hiện tại.
     * 
     * Luồng xử lý:
     * 1. Xác thực ID người dùng từ Security Context.
     * 2. Gọi dịch vụ để truy xuất toàn bộ thông báo của người dùng, sắp xếp giảm dần theo thời gian tạo.
     * 3. Trả về danh sách NotificationResponse.
     * 
     * @return Danh sách thông báo {@link NotificationResponse} của tài khoản hiện tại
     */
    @GetMapping("/notifications")
    public List<NotificationResponse> getMyNotifications() {
        return messagingService.getMyNotifications();
    }

    /**
     * [UC-51] Đánh dấu một thông báo là đã đọc.
     * 
     * Luồng xử lý:
     * 1. Tiếp nhận notificationId từ URL path.
     * 2. Kiểm tra quyền sở hữu thông báo thuộc về người dùng đang đăng nhập.
     * 3. Cập nhật cờ isRead = true và thời gian readAt thành thời điểm hiện tại.
     * 4. Trả về thông báo thành công dạng JSON map.
     * 
     * @param notificationId ID của thông báo cần đánh dấu đã đọc
     * @return Map chứa thông báo xác nhận thành công
     */
    @PatchMapping("/notifications/{notificationId}/read")
    public Map<String, String> markAsRead(@PathVariable Long notificationId) {
        messagingService.markAsRead(notificationId);
        return Map.of("message", "Đã đánh dấu đã đọc");
    }

    // =========================================================================
    // LUỒNG 3: NGƯỜI DÙNG TẠO TICKET HỖ TRỢ & TỰ ĐỘNG TÍNH HẠN SLA (UC-65, UC-66)
    // =========================================================================

    /**
     * [UC-66] Lấy danh sách các yêu cầu hỗ trợ (Support Tickets) do người dùng hiện tại tạo.
     * 
     * Luồng xử lý:
     * 1. Xác định userId từ token xác thực hiện tại.
     * 2. Truy vấn danh sách ticket cá nhân sắp xếp mới nhất lên đầu.
     * 3. Trả về danh sách SupportTicketResponse cho client.
     * 
     * @return Danh sách các ticket hỗ trợ {@link SupportTicketResponse} của người dùng
     */
    @GetMapping("/support-tickets")
    public List<SupportTicketResponse> getMySupportTickets() {
        return messagingService.getMySupportTickets();
    }

    /**
     * [UC-66] Xem thông tin chi tiết một ticket hỗ trợ kèm toàn bộ lịch sử trao đổi.
     * 
     * Luồng xử lý:
     * 1. Tiếp nhận ticketId từ path variable.
     * 2. Kiểm tra bản quyền sở hữu ticket (chỉ chủ tạo ticket mới được truy cập).
     * 3. Truy xuất thông tin tiến trình SLA, phản hồi của Admin và danh sách tin nhắn chi tiết.
     * 
     * @param ticketId ID yêu cầu hỗ trợ cần xem
     * @return Chi tiết ticket {@link SupportTicketDetailResponse} kèm hội thoại
     */
    @GetMapping("/support-tickets/{ticketId}")
    public SupportTicketDetailResponse getMySupportTicketDetail(@PathVariable Long ticketId) {
        return messagingService.getMySupportTicketDetail(ticketId);
    }

    /**
     * [UC-65, UC-66] Tiếp nhận yêu cầu tạo mới ticket hỗ trợ kỹ thuật và tự động tính hạn SLA.
     * 
     * Luồng xử lý:
     * 1. Xác thực tính hợp lệ của danh mục và tiêu đề yêu cầu hỗ trợ.
     * 2. Tự động áp dụng mức ưu tiên sàn (Priority Floor) theo danh mục sự cố.
     * 3. Tính toán thời hạn giải quyết SLA (dueAt) và khởi tạo tin nhắn đầu tiên.
     * 4. Gửi thông báo In-App thời gian thực tới tất cả quản trị viên sàn đang hoạt động.
     * 
     * @param request Dữ liệu tạo ticket {@link CreateSupportTicketRequest}
     * @return Thông tin ticket vừa tạo {@link SupportTicketResponse}
     */
    @PostMapping("/support-tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketResponse createSupportTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
        return messagingService.createSupportTicket(request);
    }

    /**
     * [UC-66] Gửi thêm tin nhắn phản hồi hoặc bổ sung bằng chứng vào ticket hỗ trợ đang mở.
     * 
     * Luồng xử lý:
     * 1. Xác thực người gửi chính là người tạo ticket.
     * 2. Kiểm tra trạng thái ticket không được ở trạng thái CLOSED hoặc RESOLVED.
     * 3. Lưu nội dung tin nhắn và bằng chứng đính kèm vào lịch sử trao đổi.
     * 4. Chuyển trạng thái ticket về OPEN nếu trước đó đang ở trạng thái chờ đánh giá (IN_REVIEW).
     * 
     * @param ticketId ID yêu cầu hỗ trợ
     * @param request Nội dung và bằng chứng phản hồi {@link com.tcs.module.messaging.dto.request.ReplyTicketRequest}
     * @return Thông tin tin nhắn phản hồi {@link TicketMessageResponse}
     */
    @PostMapping("/support-tickets/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketMessageResponse replySupportTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody com.tcs.module.messaging.dto.request.ReplyTicketRequest request) {
        return messagingService.replySupportTicket(ticketId, request);
    }

    /**
     * [UC-66] Mở lại ticket hỗ trợ đã đóng/đã giải quyết nếu vấn đề chưa được khắc phục triệt để.
     * 
     * Luồng xử lý:
     * 1. Kiểm tra quyền sở hữu ticket của người dùng và trạng thái hiện tại phải là CLOSED hoặc RESOLVED.
     * 2. Đặt lại trạng thái OPEN, tính toán hạn SLA mới và xóa cờ vi phạm SLA cũ.
     * 3. Ghi nhận tin nhắn hệ thống nêu rõ lý do mở lại vào chuỗi hội thoại.
     * 4. Phát thông báo In-App tới Quản trị viên và phản hồi thông tin chi tiết ticket sau cập nhật.
     * 
     * @param ticketId ID yêu cầu hỗ trợ cần mở lại
     * @param request Lý do và bằng chứng mở lại (tùy chọn)
     * @return Thông tin chi tiết ticket sau khi mở lại {@link SupportTicketDetailResponse}
     */
    @PostMapping("/support-tickets/{ticketId}/reopen")
    public SupportTicketDetailResponse reopenSupportTicket(
            @PathVariable Long ticketId,
            @RequestBody(required = false) com.tcs.module.messaging.dto.request.ReplyTicketRequest request) {
        return messagingService.reopenSupportTicket(ticketId, request);
    }

    /**
     * [UC-49] Tạo báo cáo vi phạm mới đối với người dùng, lớp học hoặc đánh giá trên sàn.
     * 
     * Luồng xử lý:
     * 1. Kiểm tra giới hạn tần suất tạo báo cáo (tối đa 5 báo cáo trong 24 giờ).
     * 2. Xác thực tính hợp lệ của đối tượng bị báo cáo (USER, CLASS, REVIEW) và lý do giải trình tối thiểu 10 ký tự.
     * 3. Chặn hành vi tự báo cáo chính mình.
     * 4. Lưu bản ghi Report với trạng thái PENDING và trả về thông tin xác nhận.
     * 
     * @param request Thông tin báo cáo vi phạm {@link CreateReportRequest}
     * @return Kết quả ghi nhận báo cáo {@link ReportResponse}
     */
    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@RequestBody CreateReportRequest request) {
        return messagingService.createReport(request);
    }
}
