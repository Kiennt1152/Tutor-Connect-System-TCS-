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
    /**
     * [UC-61] Ghi nhận nhật ký kiểm toán bất biến tự động xác định người dùng từ phiên đăng nhập hiện tại.
     * 
     * Luồng xử lý:
     * 1. Lấy thông tin định danh người dùng (userId) từ SecurityContext thông qua AuthHelper.
     * 2. Chuyển tiếp tới hàm ghi log chi tiết kèm ngữ cảnh mạng (IP, User-Agent) và chuỗi JSON Diff.
     * 
     * @param action Tên hành động nghiệp vụ thực hiện (ví dụ: APPROVE_TUTOR, LOCK_USER, REFUND_ESCROW)
     * @param entityType Loại thực thể bị tác động (ví dụ: User, Contract, EscrowTransaction)
     * @param entityId Định danh thực thể bị tác động
     * @param oldValue Trạng thái hoặc giá trị dữ liệu trước khi thay đổi (tuần tự hóa JSON)
     * @param newValue Trạng thái hoặc giá trị dữ liệu mới sau khi thay đổi (tuần tự hóa JSON)
     */
    void record(String action, String entityType, Long entityId, Object oldValue, Object newValue);

    /**
     * [UC-61] Ghi nhận nhật ký kiểm toán với người thực hiện được chỉ định rõ ràng.
     * Phù hợp cho các thao tác diễn ra trước hoặc ngoài phiên JWT (ví dụ: đăng ký tài khoản, đăng nhập, callback webhook).
     * 
     * @param actorUserId Định danh người thực hiện hành động
     * @param action Tên hành động nghiệp vụ thực hiện
     * @param entityType Loại thực thể bị tác động
     * @param entityId Định danh thực thể bị tác động
     * @param oldValue Trạng thái hoặc giá trị trước khi thay đổi
     * @param newValue Trạng thái hoặc giá trị mới sau khi thay đổi
     */
    void record(Long actorUserId, String action, String entityType, Long entityId, Object oldValue, Object newValue);

    /**
     * [UC-61] Tra cứu và phân trang danh sách nhật ký kiểm toán đa tiêu chí phục vụ thanh tra an ninh.
     * 
     * @param actorId Lọc theo định danh người thực hiện
     * @param actorRole Lọc theo vai trò người thực hiện (PLATFORM_ADMIN, TUTOR, CLIENT, TUTOR_CENTER)
     * @param action Lọc theo mã hành vi nghiệp vụ
     * @param entityType Lọc theo loại đối tượng thực thể
     * @param keyword Từ khóa tìm kiếm tự do trong email, hành động, hoặc chi tiết thay đổi
     * @param from Mốc thời gian bắt đầu
     * @param to Mốc thời gian kết thúc
     * @param page Số trang truy vấn (bắt đầu từ 0)
     * @param size Số lượng bản ghi trên một trang
     * @return PageAuditLogResponse kết quả phân trang nhật ký kiểm toán kèm thông tin đầy đủ người thực hiện
     */
    PageAuditLogResponse search(Long actorId, String actorRole, String action, String entityType, String keyword,
            LocalDateTime from, LocalDateTime to, int page, int size);
}
