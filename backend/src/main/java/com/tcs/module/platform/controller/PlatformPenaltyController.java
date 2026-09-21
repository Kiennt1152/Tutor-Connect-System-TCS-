package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.service.PenaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * [UC-60] [UC-63] QUẢN TRỊ XỬ PHẠT VI PHẠM & CHẾ TÀI TÀI KHOẢN (PENALTY CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Quản trị viên ban hành các chế tài xử lý vi phạm quy chế nền tảng nhằm giữ gìn kỷ cương hoạt động của sàn.
 *   - Thực thi các biện pháp ngăn chặn kịp thời các hành vi gian lận, lách sàn, xúc phạm hoặc vi phạm hợp đồng giảng dạy.
 * 
 * Chức năng chính:
 *   1. Danh sách chế tài: Phân trang và tra cứu án phạt theo người dùng, trạng thái, loại hình phạt và nguồn vi phạm.
 *   2. Ban hành án phạt: Áp dụng các mức phạt theo thang bậc (Cảnh cáo, Cấm chat, Cấm đăng lớp, Khóa tài khoản).
 *   3. Thu hồi án phạt: Hủy bỏ hiệu lực án phạt trước hạn khi người dùng khiếu nại thành công.
 *   4. Đồng bộ trạng thái: Tự động đổi trạng thái người dùng (ACTIVE <-> BANNED) và kích hoạt gửi thông báo cảnh báo.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên lọc và xem danh sách các vi phạm cần xử lý (`list`).
 *   - Bước 2: Ban hành án phạt (`issue`), kiểm tra ràng buộc và gọi `penaltyService.issuePenalty`.
 *   - Bước 3: Xem lại thông tin chi tiết biên bản xử phạt (`getById`).
 *   - Bước 4: Xem xét đơn khiếu nại và tiến hành thu hồi án phạt trước thời hạn nếu hợp lệ (`revoke`).
 * ============================================================================
 */
@RestController
@RequestMapping("/api/platform/penalties")
@RequiredArgsConstructor
public class PlatformPenaltyController {

    private final PenaltyService penaltyService;

    /**
     * [UC-60]: Tra cứu, lọc và phân trang danh sách các án phạt đã ban hành.
     *     * @param page Số trang (mặc định 0)
     * @param size Số bản ghi mỗi trang (mặc định 20)
     * @param status Trạng thái án phạt (ACTIVE: Đang hiệu lực, REVOKED: Đã thu hồi, EXPIRED: Đã hết hạn)
     * @param type Loại chế tài (WARNING, CHAT_BAN, CLASS_BAN, ACCOUNT_LOCK...)
     * @param userId Lọc theo ID người dùng bị phạt cụ thể
     * @param sourceType Nguồn gốc phát hiện vi phạm (CIRCUMVENTION, DISPUTE, REPORT, ADMIN_MANUAL)
     * @return {@link PagePenaltyResponse} Danh sách án phạt kèm thông tin người ra quyết định và thời hạn
     */
    @GetMapping
    public PagePenaltyResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserPenaltyStatus status,
            @RequestParam(required = false) UserPenaltyType type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sourceType) {
        return penaltyService.listPenalties(userId, status, type, sourceType, page, size);
    }

    /**
     * [UC-60]: Ban hành quyết định xử phạt mới đối với người dùng vi phạm.
     *     * Luồng thực thi:
     *   1. Xác thực thông tin: userId người vi phạm, loại hình phạt, thời hạn áp dụng, lý do xử phạt.
     *   2. Áp dụng hiệu lực tức thì: Nếu là ACCOUNT_LOCK, cập nhật trạng thái User thành SUSPENDED ngay lập tức.
     *   3. Gửi thông báo cảnh cáo tự động qua kênh Notification và Email kèm lý do chi tiết.
     *   4. Ghi nhận dấu vết vào Nhật ký kiểm toán hệ thống [UC-61].
     *     * @param request Dữ liệu quyết định xử phạt {@link IssuePenaltyRequest}
     * @return {@link PenaltyResponse} Bản ghi án phạt vừa được ban hành thành công
     */
    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public PenaltyResponse issue(@Valid @RequestBody IssuePenaltyRequest request) {
        return penaltyService.issuePenalty(request);
    }

    /**
     * [UC-60]: Thu hồi / Hủy bỏ quyết định xử phạt (Revoke Penalty).
     *     * Nghiệp vụ:
     *   - Áp dụng khi người dùng giải trình hợp lý, khiếu nại thành công hoặc có quyết định ân xá của Admin.
     *   - Chuyển trạng thái án phạt sang REVOKED.
     *   - Tự động mở khóa tài khoản / gỡ bỏ hạn chế tính năng tương ứng.
     *   - Ghi chú lý do thu hồi và định danh Admin thực hiện.
     *     * @param penaltyId ID của án phạt cần thu hồi
     * @param request Lý do thu hồi quyết định phạt
     * @return {@link PenaltyResponse} Bản ghi án phạt sau khi thu hồi
     */
    @PatchMapping("/{penaltyId}/revoke")
    public PenaltyResponse revoke(
            @PathVariable Long penaltyId, @Valid @RequestBody RevokePenaltyRequest request) {
        return penaltyService.revokePenalty(penaltyId, request);
    }
}
