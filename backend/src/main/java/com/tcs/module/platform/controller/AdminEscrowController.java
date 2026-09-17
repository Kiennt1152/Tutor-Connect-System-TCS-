package com.tcs.module.platform.controller;

import com.tcs.module.finance.dto.response.AdminEscrowPageResponse;
import com.tcs.module.finance.dto.response.AdminEscrowResponse;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.service.AdminEscrowService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * PHÂN HỆ QUẢN TRỊ GIAO DỊCH KÝ QUỸ ESCROW TOÀN SÀN (ADMIN ESCROW CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Đức)
 * Các Use Case liên quan:
 *   - [UC-40] Quản lý & Phê duyệt giải ngân thanh toán Escrow
 *   - [UC-58] Quản trị vòng đời & trạng thái giao dịch Ký quỹ Escrow (FUNDED, RELEASED, DISPUTED, ON_HOLD, REFUNDED)
 * 
 * Nghiệp vụ cốt lõi:
 *   - Escrow là cơ chế bảo vệ kép giữa Phụ huynh (Payer) và Gia sư/Trung tâm (Beneficiary):
 *     1. Phụ huynh ký quỹ 100% học phí trước khi lớp bắt đầu. Tiền được đóng băng an toàn trong ví Escrow hệ thống.
 *     2. Trong suốt quá trình học, tiền không bị thất thoát và được bảo đảm bởi TCS Smart Escrow Contract.
 *     3. Khi lớp học hoàn thành hợp lệ (hoặc theo từng cột mốc), tiền được giải ngân (RELEASE) về ví người thụ hưởng.
 *     4. Trường hợp có sự cố hoặc hủy lớp, tiền được phán xử hoàn lại (REFUND) minh bạch.
 */
@RestController
@RequestMapping("/api/platform/escrows")
@RequiredArgsConstructor
public class AdminEscrowController {

    private final AdminEscrowService service;

    /**
     * [UC-58 & UC-40]: Tra cứu, lọc và phân trang toàn bộ giao dịch ký quỹ trên sàn.
     * 
     * Bộ lọc nghiệp vụ:
     *   - {@code status}: Lọc theo 6 trạng thái ký quỹ:
     *       * PENDING: Chờ client nạp tiền
     *       * FUNDED: Đã nạp thành công, đang phong tỏa bảo vệ quyền lợi
     *       * ON_HOLD: Đang tạm giữ do có khiếu nại hoặc kiểm tra bảo mật
     *       * DISPUTED: Đang trong quá trình phán xử tranh chấp BF-08
     *       * RELEASED: Đã giải ngân thành công về ví gia sư/trung tâm
     *       * REFUNDED: Đã hoàn tiền cho phụ huynh
     *   - {@code from}, {@code to}: Lọc theo khoảng ngày ký quỹ
     *   - {@code reference}: Tìm theo mã giao dịch (ESC-CLS-xxx)
     *   - {@code payer}: Tìm theo email hoặc tên phụ huynh thanh toán
     *   - {@code beneficiary}: Tìm theo email hoặc tên gia sư/trung tâm nhận tiền
     * 
     * @return {@link AdminEscrowPageResponse} Danh sách giao dịch phân trang kèm tổng số tiền ký quỹ
     */
    @GetMapping
    public AdminEscrowPageResponse search(
            @RequestParam(required = false) EscrowStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String payer,
            @RequestParam(required = false) String beneficiary,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ: 'from' không được lớn hơn 'to'.");
        }
        return service.search(status, from, to, reference, payer, beneficiary, page, size);
    }

    /**
     * [UC-40]: Xem chi tiết hồ sơ một khoản ký quỹ Escrow.
     * 
     * Thông tin bao gồm:
     *   - Số tiền ký quỹ gốc, phí nền tảng ước tính hoặc đã trừ, số tiền thực nhận.
     *   - Thông tin hợp đồng liên kết (Contract ID, Assignment ID, Class ID).
     *   - Tiến độ buổi học, trạng thái điểm danh và lịch sử giải ngân/hoàn tiền.
     * 
     * @param escrowId ID của bản ghi ký quỹ cần tra cứu
     * @return {@link AdminEscrowResponse} Chi tiết toàn diện của giao dịch ký quỹ
     */
    @GetMapping("/{escrowId}")
    public AdminEscrowResponse get(@PathVariable Long escrowId) {
        return service.get(escrowId);
    }
}
