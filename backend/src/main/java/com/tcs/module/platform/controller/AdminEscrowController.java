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
 * [BF-10] [UC-58] [UC-40] PHÂN HỆ QUẢN TRỊ GIAO DỊCH KÝ QUỸ ESCROW (ADMIN ESCROW CONTROLLER)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-07-29
 * * 1. Mục đích & Chức năng:
 *   - Quản trị toàn bộ các giao dịch ký quỹ Escrow bảo chứng giữa Phụ huynh, Gia sư và Trung tâm.
 *   - Quản lý vòng đời ký quỹ: FUNDED (Đã nạp), RELEASED (Đã giải ngân), DISPUTED (Tranh chấp), ON_HOLD (Tạm giữ), REFUNDED (Hoàn trả).
 *   - Hỗ trợ Admin can thiệp đóng băng hoặc giải ngân trong các trường hợp phán quyết tranh chấp.
 * * 2. Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận các bộ lọc trạng thái và khoảng thời gian từ Admin Console.
 *   - Bước 2: Gọi AdminEscrowService để truy vấn dữ liệu từ bảng escrow_transactions.
 *   - Bước 3: Trả về danh sách phân trang AdminEscrowPageResponse kèm số liệu thống kê.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/platform/escrows")
@RequiredArgsConstructor
public class AdminEscrowController {

    private final AdminEscrowService service;

    /**
     * [UC-58 & UC-40]: Tra cứu, lọc và phân trang toàn bộ giao dịch ký quỹ trên sàn.
     *     * Bộ lọc nghiệp vụ:
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
     *     * @return {@link AdminEscrowPageResponse} Danh sách giao dịch phân trang kèm tổng số tiền ký quỹ
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
     *     * Thông tin bao gồm:
     *   - Số tiền ký quỹ gốc, phí nền tảng ước tính hoặc đã trừ, số tiền thực nhận.
     *   - Thông tin hợp đồng liên kết (Contract ID, Assignment ID, Class ID).
     *   - Tiến độ buổi học, trạng thái điểm danh và lịch sử giải ngân/hoàn tiền.
     *     * @param escrowId ID của bản ghi ký quỹ cần tra cứu
     * @return {@link AdminEscrowResponse} Chi tiết toàn diện của giao dịch ký quỹ
     */
    @GetMapping("/{escrowId}")
    public AdminEscrowResponse get(@PathVariable Long escrowId) {
        return service.get(escrowId);
    }
}
