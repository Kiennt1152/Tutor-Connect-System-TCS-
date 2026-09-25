package com.tcs.module.platform.service;

import com.tcs.module.messaging.entity.Message;
import com.tcs.module.platform.dto.request.ReviewCircumventionRequest;
import com.tcs.module.platform.dto.response.CircumventionEventResponse;
import com.tcs.module.platform.dto.response.CircumventionConversationResponse;
import com.tcs.module.platform.dto.response.PageCircumventionEventResponse;

/**
 * ============================================================================
 * [UC-59] QUÉT & PHÁT HIỆN HÀNH VI LÁCH NỀN TẢNG (CIRCUMVENTION SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Dịch vụ phát hiện và xử lý các hành vi chia sẻ thông tin liên lạc ngoài sàn trong tin nhắn chat.
 *   - Ngăn chặn rủi ro giao dịch ngoài nền tảng và bảo vệ quyền lợi an toàn qua Escrow.
 * 
 * Chức năng chính:
 *   1. Quét nội dung tin nhắn: Phân tích tin nhắn thời gian thực để phát hiện số điện thoại, email, link ngoài.
 *   2. Danh sách nghi vấn: Cung cấp danh sách các sự kiện gắn cờ rủi ro chờ Quản trị viên thẩm định.
 *   3. Bằng chứng đối soát: Truy xuất ngữ cảnh các tin nhắn trước và sau vi phạm.
 *   4. Thẩm định vi phạm: Cập nhật kết luận kiểm duyệt (Xác nhận vi phạm hoặc Hủy bỏ cảnh báo).
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Khi tin nhắn được gửi, hệ thống gọi hàm `inspect` để quét nội dung.
 *   - Bước 2: Nếu có dấu hiệu lách sàn, tạo bản ghi sự kiện vi phạm kèm điểm rủi ro.
 *   - Bước 3: Quản trị viên tra cứu bằng chứng ngữ cảnh hội thoại (`getConversationEvidence`).
 *   - Bước 4: Đưa ra quyết định phê duyệt xử lý (`review`) và liên thông ban hành chế tài.
 * ============================================================================
 */
public interface CircumventionService {
    /**
     * [UC-59] Quét và phân tích nội dung tin nhắn thời gian thực để phát hiện thông tin liên lạc ngoài sàn.
     * 
     * Luồng xử lý:
     * 1. Tiếp nhận tin nhắn vừa được gửi trong hệ thống trò chuyện.
     * 2. Áp dụng 4 bộ lọc Regex nhận diện: Số điện thoại Việt Nam, Email, Đường dẫn URL ngoài, Tài khoản mạng xã hội.
     * 3. Nếu phát hiện vi phạm, tạo sự kiện CircumventionEvent lưu lại bằng chứng chuỗi ký tự khớp và điểm rủi ro.
     * 
     * @param message Thực thể tin nhắn cần kiểm duyệt
     */
    void inspect(Message message);

    /**
     * [UC-59] Truy vấn danh sách các sự kiện nghi vấn lách nền tảng được phân trang theo trạng thái.
     * 
     * @param status Trạng thái sự kiện (PENDING, CONFIRMED, DISMISSED hoặc null/rỗng cho tất cả)
     * @param page Số trang truy vấn (bắt đầu từ 0)
     * @param size Số lượng bản ghi trên một trang
     * @return PageCircumventionEventResponse danh sách sự kiện kèm điểm rủi ro và thông tin vi phạm
     */
    PageCircumventionEventResponse list(String status, int page, int size);

    /**
     * [UC-59] Truy xuất ngữ cảnh đối soát bằng chứng cuộc trò chuyện xung quanh tin nhắn vi phạm.
     * 
     * Luồng xử lý:
     * 1. Tìm bản ghi sự kiện CircumventionEvent theo eventId.
     * 2. Tải tối đa 100 tin nhắn gần nhất trong cuộc trò chuyện chứa tin nhắn bị gắn cờ.
     * 3. Lấy thông tin các bên tham gia cuộc trò chuyện và đánh dấu cờ (flagged) cho tin nhắn vi phạm.
     * 4. Ghi nhận nhật ký kiểm toán hành động xem bằng chứng của Quản trị viên.
     * 
     * @param eventId Định danh sự kiện nghi vấn lách sàn
     * @return CircumventionConversationResponse ngữ cảnh hội thoại, danh sách thành viên và các tin nhắn bằng chứng
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy sự kiện
     */
    CircumventionConversationResponse getConversationEvidence(Long eventId);

    /**
     * [UC-59] Thẩm định và ban hành quyết định phê duyệt xử lý sự kiện vi phạm lách sàn.
     * 
     * Luồng xử lý:
     * 1. Xác thực sự kiện đang ở trạng thái PENDING.
     * 2. Cập nhật kết luận của Quản trị viên (CONFIRMED hoặc DISMISSED) cùng ghi chú giải trình.
     * 3. Lưu vết kiểm toán hành động thẩm định vào AuditLogService.
     * 
     * @param eventId Định danh sự kiện vi phạm
     * @param request Yêu cầu phê duyệt chứa trạng thái mới và ghi chú
     * @return CircumventionEventResponse thông tin sự kiện sau khi cập nhật quyết định
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy sự kiện hoặc Quản trị viên
     * @throws IllegalStateException nếu sự kiện đã được duyệt trước đó
     */
    CircumventionEventResponse review(Long eventId, ReviewCircumventionRequest request);
}
