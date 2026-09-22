package com.tcs.module.platform.service;

/**
 * ============================================================================
 * [UC-63] CỔNG CHẮN KIỂM SOÁT TÍNH NĂNG THEO CHẾ TÀI (PENALTY ACCESS GUARD)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Cổng chắn kiểm tra quyền sử dụng tính năng của người dùng dựa trên các án phạt đang có hiệu lực.
 *   - Chặn đứng các hành vi sử dụng tính năng bị kỷ luật (Chat, Đăng lớp, Nhận lớp, Đăng tuyển).
 * 
 * Chức năng chính:
 *   1. Kiểm tra quyền tính năng: Xác minh tài khoản có bị hạn chế tính năng cụ thể hay không.
 *   2. Ngăn chặn tự động: Ném ngoại lệ từ chối truy cập (ForbiddenException) khi vi phạm án phạt.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Các service nghiệp vụ gọi hàm `requireFeature` trước khi thực hiện thao tác nhạy cảm.
 *   - Bước 2: Truy vấn danh sách các án phạt `FEATURE_RESTRICTION` đang còn hiệu lực của người dùng.
 *   - Bước 3: So khớp mã tính năng yêu cầu với danh sách hạn chế; ném ngoại lệ nếu bị chặn.
 * ============================================================================
 */
public interface PenaltyAccessService {
    void requireFeature(Long userId, String featureCode);
}
