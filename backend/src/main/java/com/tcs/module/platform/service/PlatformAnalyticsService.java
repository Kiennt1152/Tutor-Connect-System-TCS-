package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.dto.response.CenterFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.ClientFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.FinancialLedgerItemResponse;
import com.tcs.module.platform.dto.response.TutorFinancialAnalyticsResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ============================================================================
 * [UC-41] [UC-43] [UC-58] PHÂN TÍCH TÀI CHÍNH & SỔ CÁI KẾ TOÁN SÀN (ANALYTICS SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Cung cấp dịch vụ phân tích dữ liệu tài chính, doanh thu phí sàn và dòng tiền luân chuyển toàn hệ thống.
 *   - Cung cấp sổ cái kế toán đa chiều đối soát từng giao dịch và hỗ trợ xuất báo cáo kế toán CSV.
 * 
 * Chức năng chính:
 *   1. Tổng hợp tài chính: Đo lường dòng tiền vào, dòng tiền ra, tiền ký quỹ bảo chứng và doanh thu nền tảng.
 *   2. Phân tích đối tượng: Báo cáo hiệu quả tài chính riêng biệt cho Trung tâm, Gia sư và Phụ huynh.
 *   3. Sổ cái giao dịch: Đối soát từng bút toán giao dịch chi tiết theo chiều dòng tiền.
 *   4. Xuất báo cáo CSV: Tạo tệp dữ liệu kế toán chuẩn UTF-8 BOM phục vụ lưu trữ kiểm toán.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận khoảng thời gian cần thống kê từ Quản trị viên (`getSummary`).
 *   - Bước 2: Truy vấn dữ liệu luân chuyển tài chính từ các sổ cái kế toán.
 *   - Bước 3: Phân rã số liệu theo từng phân khúc đối tượng người dùng.
 *   - Bước 4: Tạo dữ liệu báo cáo tổng hợp hoặc kết xuất định dạng tệp CSV (`exportCsv`).
 * ============================================================================
 */
public interface PlatformAnalyticsService {
    AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to);
    byte[] exportCsv(String type, LocalDate from, LocalDate to);
    int generateScheduledDailyReport();

    List<CenterFinancialAnalyticsResponse> getCenterAnalytics(LocalDate from, LocalDate to);
    CenterFinancialAnalyticsResponse getCenterAnalyticsByCenterId(Long centerId, LocalDate from, LocalDate to);
    List<TutorFinancialAnalyticsResponse> getTutorAnalytics(LocalDate from, LocalDate to);
    List<ClientFinancialAnalyticsResponse> getClientAnalytics(LocalDate from, LocalDate to);
    Page<FinancialLedgerItemResponse> getFinancialLedger(
            String role, String direction, String search, LocalDate from, LocalDate to, Pageable pageable);
}
