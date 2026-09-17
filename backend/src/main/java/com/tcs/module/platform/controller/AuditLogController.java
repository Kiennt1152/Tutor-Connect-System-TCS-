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
 * PHÂN HỆ GIÁM SÁT NHẬT KÝ KIỂM TOÁN HỆ THỐNG (SYSTEM AUDIT LOG CONTROLLER)
 * ============================================================================
 * 
 * Mã Use Case: [UC-61] Giám sát & Tra cứu nhật ký kiểm toán hệ thống (Audit Trail)
 * Tác giả: mduc1011-swp (Đức)
 * 
 * Nghiệp vụ bảo mật & Pháp lý:
 *   - Mọi thao tác quản trị nhạy cảm trên sàn đều bắt buộc phải được ghi nhận bất biến (Immutable Audit Log):
 *       * Khóa/mở tài khoản người dùng, đổi quyền, reset mật khẩu.
 *       * Duyệt/từ chối hồ sơ xác minh căn cước, bằng cấp, giấy phép kinh doanh.
 *       * Phán xử tranh chấp tiền ký quỹ Escrow, phê duyệt lệnh hoàn tiền.
 *       * Ban hành hoặc thu hồi án phạt vi phạm quy chế sàn.
 *       * Điều chỉnh tỷ lệ phí nền tảng, xuất báo cáo tài chính.
 *   - Nhật ký kiểm toán lưu lại đầy đủ: Actor ID, Actor Role, Action, Entity Type, Entity ID,
 *     Giá trị cũ (Old Value), Giá trị mới (New Value), IP/User Agent và Dấu thời gian (Timestamp).
 */
@RestController
@RequestMapping("/api/platform/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * [UC-61]: Tìm kiếm, lọc đa chiều và phân trang nhật ký kiểm toán hệ thống.
     * 
     * Bộ lọc nghiệp vụ:
     *   - {@code actorId}: ID người thực hiện thao tác (Admin, User).
     *   - {@code actorRole}: Vai trò người thao tác (PLATFORM_ADMIN, TUTOR_CENTER, v.v.).
     *   - {@code action}: Loại hành động (UPDATE_USER_STATUS, APPROVE_VERIFICATION, RESOLVE_DISPUTE, ISSUE_PENALTY, UPDATE_CENTER_FEE...).
     *   - {@code entityType}: Loại đối tượng bị tác động (USER, TUTOR_CENTER, ESCROW, VERIFICATION, REVIEW...).
     *   - {@code keyword}: Tìm kiếm nội dung mô tả hoặc thay đổi dữ liệu JSON.
     *   - {@code from}, {@code to}: Khoảng thời gian phát sinh hành động.
     * 
     * @return {@link PageAuditLogResponse} Danh sách bản ghi nhật ký kiểm toán phân trang
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
