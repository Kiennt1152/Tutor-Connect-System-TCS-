package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.dto.response.CenterFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.ClientFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.FinancialLedgerItemResponse;
import com.tcs.module.platform.dto.response.TutorFinancialAnalyticsResponse;
import com.tcs.module.platform.service.PlatformAnalyticsService;
import com.tcs.module.platform.service.AuditLogService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * PHÂN HỆ PHÂN TÍCH TÀI CHÍNH, DOANH THU & SỔ CÁI TOÀN SÀN (FINANCIAL ANALYTICS)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Đức)
 * Các Use Case liên quan:
 *   - [UC-41] Báo cáo tài chính & Phân tích doanh thu toàn sàn
 *   - [UC-43] Xuất báo cáo tài chính dạng tệp CSV UTF-8 kèm ghi vết kiểm toán
 *   - [UC-58] Sổ cái kế toán giao dịch tài chính đa chiều (Financial Ledger)
 * 
 * Mục tiêu nghiệp vụ:
 *   - Cung cấp cho Quản trị viên bức tranh tài chính toàn diện và minh bạch:
 *     1. Dòng tiền vào (INFLOW): Nạp tiền vào ví, Ký quỹ lớp học (Escrow Deposit), Phí xử lý yêu cầu.
 *     2. Dòng tiền ra (OUTFLOW): Rút tiền về ngân hàng (Withdrawal Completed), Hoàn tiền ký quỹ (Escrow Refund).
 *     3. Doanh thu thực tế nền tảng (PLATFORM REVENUE): Khấu trừ 2% (hoặc tỷ lệ phí riêng của trung tâm) khi giải ngân.
 *     4. Tiền đang phong tỏa (FROZEN / ESCROW HOLDING): Các khoản học phí đang được bảo lưu an toàn.
 *     5. Phân rã chi tiết theo từng đối tượng: Trung tâm gia sư (Centers), Gia sư cá nhân (Tutors), Phụ huynh (Clients).
 *     6. Sổ cái giao dịch chi tiết (Ledger) đối soát từng mã bút toán (Reference Code), chiều dòng tiền, người thực hiện.
 */
@RestController
@RequestMapping("/api/platform/analytics")
@RequiredArgsConstructor
public class PlatformAnalyticsController {

    private final PlatformAnalyticsService analyticsService;
    private final AuditLogService auditLogService;

    /**
     * [UC-41]: Báo cáo tổng quan tài chính toàn sàn theo khoảng thời gian.
     * 
     * Các chỉ số tính toán bao gồm:
     *   - Tổng số dư khả dụng (Available Balance) của toàn bộ ví người dùng.
     *   - Tổng tiền đang phong tỏa ký quỹ (Frozen / Escrow Balance).
     *   - Tổng dòng tiền vào (Total Inflow), Dòng tiền ra (Total Outflow), Dòng tiền ròng (Net Flow).
     *   - Doanh thu phí sàn thực tế đã thu (Platform Fee Income).
     *   - Bảng phân rã chi tiết từng loại giao dịch phát sinh (Deposit, Withdrawal, Escrow, Refund, Fee).
     * 
     * @param from Ngày bắt đầu thống kê (tùy chọn)
     * @param to Ngày kết thúc thống kê (tùy chọn)
     * @return {@link AnalyticsSummaryResponse} Báo cáo tài chính tổng quan toàn sàn
     */
    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getSummary(from, to);
    }

    /**
     * [UC-41]: Phân tích chỉ số tài chính và hoạt động kinh doanh của từng Trung tâm Gia sư.
     * 
     * Các chỉ số chi tiết cho mỗi trung tâm:
     *   - Tổng lớp học đang mở / hoàn thành, số học viên ghi danh.
     *   - Tổng doanh thu lớp học, số tiền đã giải ngân về trung tâm.
     *   - Tiền đang giữ trong ký quỹ lớp học, số dư ví khả dụng.
     *   - Số tiền đã rút thành công và các yêu cầu rút tiền đang chờ duyệt.
     * 
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return Danh sách báo cáo tài chính của các trung tâm gia sư
     */
    @GetMapping("/entities/centers")
    public List<CenterFinancialAnalyticsResponse> getCenterAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getCenterAnalytics(from, to);
    }

    /**
     * [UC-41]: Phân tích chỉ số thu nhập và hoạt động của từng Gia sư cá nhân.
     * 
     * Các chỉ số:
     *   - Số lớp học phụ trách, số lớp hoàn thành, đánh giá sao trung bình.
     *   - Tổng thu nhập gia sư kiếm được, số tiền ký quỹ đang chờ giải ngân.
     *   - Số tiền đã rút về tài khoản ngân hàng cá nhân, số dư ví hiện tại.
     * 
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return Danh sách chỉ số tài chính của các gia sư
     */
    @GetMapping("/entities/tutors")
    public List<TutorFinancialAnalyticsResponse> getTutorAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getTutorAnalytics(from, to);
    }

    /**
     * [UC-41]: Phân tích mức chi tiêu và dòng tiền nạp/hoàn của từng Khách hàng / Phụ huynh (Client).
     * 
     * Các chỉ số:
     *   - Tổng số tiền nạp vào sàn (Total Deposited).
     *   - Tổng tiền đang ký quỹ cho các con theo học (Active Escrow).
     *   - Tổng tiền đã được hoàn trả sau phán xử/hủy lớp (Total Refunded).
     *   - Số dư ví khả dụng còn lại.
     * 
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return Danh sách chỉ số tài chính của phụ huynh
     */
    @GetMapping("/entities/clients")
    public List<ClientFinancialAnalyticsResponse> getClientAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getClientAnalytics(from, to);
    }

    /**
     * [UC-58]: Sổ cái tài chính toàn hệ thống (Financial Ledger) tra cứu đa chiều.
     * 
     * Tính năng:
     *   - Hỗ trợ lọc theo Vai trò thực hiện: CLIENT, TUTOR, TUTOR_CENTER, PLATFORM_ADMIN.
     *   - Lọc theo Chiều dòng tiền: IN (Tiền vào sàn) hoặc OUT (Tiền ra khỏi sàn).
     *   - Tìm kiếm linh hoạt theo Mã giao dịch (Reference Code), Tên người thực hiện, Email.
     *   - Lọc theo khoảng thời gian và phân trang dữ liệu chuẩn hóa.
     * 
     * @param role Vai trò người thực hiện giao dịch
     * @param direction Chiều dòng tiền ('IN' hoặc 'OUT')
     * @param search Từ khóa tìm kiếm
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @param page Số trang (0-indexed)
     * @param size Kích thước trang
     * @return {@link Page} Danh sách các bản ghi sổ cái tài chính
     */
    @GetMapping("/ledger")
    public Page<FinancialLedgerItemResponse> getFinancialLedger(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validateRange(from, to);
        return analyticsService.getFinancialLedger(role, direction, search, from, to, PageRequest.of(page, size));
    }

    /**
     * [UC-43]: Xuất báo cáo tài chính ra tệp tin CSV chuẩn UTF-8 (có BOM cho Excel).
     * 
     * Nghiệp vụ:
     *   - Hỗ trợ các loại báo cáo: 'summary' (tổng quan sàn), 'centers' (trung tâm),
     *     'tutors' (gia sư), 'clients' (phụ huynh), 'users' (toàn bộ người dùng).
     *   - Tự động ghi vết vào Nhật ký kiểm toán (Audit Log) với action {@code EXPORT_ANALYTICS}
     *     để kiểm soát việc tải xuất dữ liệu tài chính nhạy cảm.
     * 
     * @param type Loại báo cáo cần xuất ('summary', 'centers', 'tutors', 'clients', 'users')
     * @param format Định dạng xuất (chỉ hỗ trợ 'csv')
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return {@link ResponseEntity} Tệp tin CSV dạng binary byte[] kèm HTTP Header attachment
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "users") String type,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ định dạng xuất CSV: " + format);
        }
        validateRange(from, to);
        byte[] csvData = analyticsService.exportCsv(type, from, to);
        auditLogService.record("EXPORT_ANALYTICS", "AnalyticsExport", 0L, null,
                java.util.Map.of("type", type, "from", String.valueOf(from), "to", String.valueOf(to)));
        String filename = "tcs-analytics-" + type + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Kích hoạt thủ công tác vụ chốt số và lưu Snapshot báo cáo tài chính định kỳ.
     * 
     * @return Thông báo kết quả và số lượng bản ghi snapshot được tạo
     */
    @PostMapping("/scheduled-reports/trigger")
    public java.util.Map<String, Object> triggerScheduledReport() {
        int count = analyticsService.generateScheduledDailyReport();
        return java.util.Map.of("message", "Tạo báo cáo định kỳ tự động thành công", "count", count, "status", "SUCCESS");
    }

    /**
     * Xác thực tính hợp lệ của khoảng ngày tìm kiếm (from <= to).
     */
    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
    }
}
