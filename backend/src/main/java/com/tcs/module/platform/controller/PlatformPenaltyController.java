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
 * PHÂN HỆ QUẢN TRỊ XỬ PHẠT VI PHẠM & CHẾ TÀI TÀI KHOẢN (PLATFORM PENALTIES)
 * ============================================================================
 * 
 * Mã Use Case: [UC-60] Xử lý vi phạm & Ban hành chế tài người dùng
 * Tác giả: mduc1011-swp (Đức)
 * 
 * Nghiệp vụ xử lý chế tài:
 *   - Khi phát hiện vi phạm quy chế sàn (từ Lách sàn UC-59, Tranh chấp UC-49, Báo cáo UC-52, Gian lận đánh giá UC-55):
 *     Quản trị viên có thẩm quyền ban hành các chế tài tương ứng theo thang bậc:
 *       1. WARNING: Cảnh cáo chính thức gửi qua thông báo hệ thống và email.
 *       2. TEMPORARY_CHAT_BAN: Tạm khóa quyền gửi tin nhắn trong 3 ngày / 7 ngày / 30 ngày.
 *       3. TEMPORARY_CLASS_CREATION_BAN: Khóa quyền đăng tin mở lớp hoặc ứng tuyển dạy.
 *       4. DEDUCT_REPUTATION: Trừ điểm uy tín gia sư/trung tâm trên bảng xếp hạng tìm kiếm.
 *       5. ACCOUNT_LOCK: Khóa tài khoản tạm thời hoặc vĩnh viễn (chuyển UserStatus sang SUSPENDED/BANNED).
 *   - Hỗ trợ cơ chế Phúc khảo / Thu hồi chế tài (Revoke Penalty) nếu người dùng khiếu nại thành công hoặc chấp hành xong.
 */
@RestController
@RequestMapping("/api/platform/penalties")
@RequiredArgsConstructor
public class PlatformPenaltyController {

    private final PenaltyService penaltyService;

    /**
     * [UC-60]: Tra cứu, lọc và phân trang danh sách các án phạt đã ban hành.
     * 
     * @param page Số trang (mặc định 0)
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
     * 
     * Luồng thực thi:
     *   1. Xác thực thông tin: userId người vi phạm, loại hình phạt, thời hạn áp dụng, lý do xử phạt.
     *   2. Áp dụng hiệu lực tức thì: Nếu là ACCOUNT_LOCK, cập nhật trạng thái User thành SUSPENDED ngay lập tức.
     *   3. Gửi thông báo cảnh cáo tự động qua kênh Notification và Email kèm lý do chi tiết.
     *   4. Ghi nhận dấu vết vào Nhật ký kiểm toán hệ thống [UC-61].
     * 
     * @param request Dữ liệu quyết định xử phạt {@link IssuePenaltyRequest}
     * @return {@link PenaltyResponse} Bản ghi án phạt vừa được ban hành thành công
     */
    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public PenaltyResponse issue(@Valid @RequestBody IssuePenaltyRequest request) {
        return penaltyService.issuePenalty(request);
    }

    /**
     * [UC-60]: Thu hồi / Hủy bỏ quyết định xử phạt (Revoke Penalty).
     * 
     * Nghiệp vụ:
     *   - Áp dụng khi người dùng giải trình hợp lý, khiếu nại thành công hoặc có quyết định ân xá của Admin.
     *   - Chuyển trạng thái án phạt sang REVOKED.
     *   - Tự động mở khóa tài khoản / gỡ bỏ hạn chế tính năng tương ứng.
     *   - Ghi chú lý do thu hồi và định danh Admin thực hiện.
     * 
     * @param penaltyId ID của án phạt cần thu hồi
     * @param request Lý do thu hồi quyết định phạt
     * @return {@link PenaltyResponse} Bản ghi án phạt sau khi thu hồi
     */
    @PatchMapping("/{penaltyId}/revoke")
    public PenaltyResponse revoke(
            @PathVariable Long penaltyId, @Valid @RequestBody RevokePenaltyRequest request) {
        return penaltyService.revokePenalty(penaltyId, request);
    }
}
