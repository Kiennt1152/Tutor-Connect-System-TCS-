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
    void inspect(Message message);
    PageCircumventionEventResponse list(String status, int page, int size);
    CircumventionConversationResponse getConversationEvidence(Long eventId);
    CircumventionEventResponse review(Long eventId, ReviewCircumventionRequest request);
}
