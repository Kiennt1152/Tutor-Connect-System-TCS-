package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.PageAuditLogResponse;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * [UC-61] GHI VẾT & TRA CỨU NHẬT KÝ KIỂM TOÁN HỆ THỐNG (AUDIT LOG SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Ghi nhận lịch sử kiểm toán bất biến (Immutable Audit Trail) cho toàn bộ các thao tác nhạy cảm trên sàn.
 *   - Đảm bảo tính minh bạch, tuân thủ pháp lý và quy trách nhiệm rõ ràng cho mọi hành động quản trị.
 * 
 * Chức năng chính:
 *   1. Ghi vết tự động: Lưu nhật ký kèm người thực hiện, loại hành động, đối tượng tác động và dữ liệu thay đổi.
 *   2. Ghi nhận ngữ cảnh mạng: Tự động trích xuất IP client và User-Agent của request.
 *   3. Tìm kiếm và phân trang: Cung cấp API tra cứu đa tiêu chí phục vụ thanh tra an ninh.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Service nghiệp vụ gọi hàm ghi vết (`record`).
 *   - Bước 2: Tự động trích xuất thông tin phiên người dùng và địa chỉ mạng hiện hành.
 *   - Bước 3: Lưu bản ghi kiểm toán mới vào CSDL với nguyên tắc không cho phép sửa/xóa.
 *   - Bước 4: Cung cấp kết quả tra cứu phục vụ kiểm toán khi có yêu cầu (`search`).
 * ============================================================================
 */
public interface AuditLogService {
    void record(String action, String entityType, Long entityId, Object oldValue, Object newValue);

    /**
     * Ghi log voi actor duoc chi dinh ro (dung cho cac hanh dong xay ra truoc khi
     * co JWT trong request, vi du: dang ky, dang nhap).
     */
    void record(Long actorUserId, String action, String entityType, Long entityId, Object oldValue, Object newValue);

    PageAuditLogResponse search(Long actorId, String actorRole, String action, String entityType, String keyword,
            LocalDateTime from, LocalDateTime to, int page, int size);
}
