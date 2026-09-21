package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.PageAuditLogResponse;
import com.tcs.module.platform.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * [UC-61] GIÁM SÁT & TRA CỨU NHẬT KÝ KIỂM TOÁN HỆ THỐNG (AUDIT LOG CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Cung cấp API giám sát và tra cứu lịch sử kiểm toán (Audit Trail) bất biến trên toàn hệ thống.
 *   - Hỗ trợ thanh tra pháp lý, đối soát giao dịch và quy trách nhiệm rõ ràng cho mọi hành động quản trị.
 * 
 * Chức năng chính:
 *   1. Tìm kiếm và phân trang nhật ký: Tra cứu toàn bộ các thao tác nhạy cảm (Tài chính, Phí sàn, Phê duyệt KYC, Chế tài).
 *   2. Bộ lọc đa chiều: Lọc theo người thao tác (actorId, actorRole), loại hành vi (action), đối tượng tác động (entityType).
 *   3. Lọc theo mốc thời gian: Giới hạn khoảng thời gian kiểm toán (`from` -> `to`).
 *   4. Tìm kiếm từ khóa: Tìm kiếm nội dung mô tả hoặc dữ liệu thay đổi trong payload JSON.
 *   5. Minh bạch dữ liệu: Trả về trạng thái cũ (oldValue) và trạng thái mới (newValue) cùng địa chỉ IP và trình duyệt thực hiện.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên gửi request tra cứu nhật ký với các tiêu chí lọc (`getAuditLogs`).
 *   - Bước 2: Hệ thống tiếp nhận bộ lọc và gọi `auditLogService.getAuditLogs`.
 *   - Bước 3: Truy vấn bản ghi từ CSDL, ánh xạ thông tin người thực hiện thông qua `PlatformMapper`.
 *   - Bước 4: Trả về dữ liệu phân trang `PageAuditLogResponse` hiển thị trên bảng nhật ký kiểm toán.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/platform/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * [UC-61]: Tìm kiếm, lọc đa chiều và phân trang nhật ký kiểm toán hệ thống.
     *     * Bộ lọc nghiệp vụ:
     *   - {@code actorId}: ID người thực hiện thao tác (Admin, User).
     *   - {@code actorRole}: Vai trò người thao tác (PLATFORM_ADMIN, TUTOR_CENTER, v.v.).
     *   - {@code action}: Loại hành động (UPDATE_USER_STATUS, APPROVE_VERIFICATION, RESOLVE_DISPUTE, ISSUE_PENALTY, UPDATE_CENTER_FEE...).
     *   - {@code entityType}: Loại đối tượng bị tác động (USER, TUTOR_CENTER, ESCROW, VERIFICATION, REVIEW...).
     *   - {@code keyword}: Tìm kiếm nội dung mô tả hoặc thay đổi dữ liệu JSON.
     *   - {@code from}, {@code to}: Khoảng thời gian phát sinh hành động.
     *     * @return {@link PageAuditLogResponse} Danh sách bản ghi nhật ký kiểm toán phân trang
     */
    @GetMapping
    public PageAuditLogResponse search(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditLogService.search(actorId, actorRole, action, entityType, keyword, from, to, page, size);
    }
}
