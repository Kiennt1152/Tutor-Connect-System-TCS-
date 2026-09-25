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
    /**
     * [UC-41] [UC-58] Tổng hợp các chỉ số tài chính, doanh thu phí sàn và dòng tiền luân chuyển toàn hệ thống.
     * 
     * Luồng xử lý:
     * 1. Tiếp nhận khoảng ngày from - to (mặc định lấy toàn bộ nếu null).
     * 2. Thống kê số lượng người dùng mới, lớp học đang mở, đang học và đã hoàn thành.
     * 3. Tổng hợp dòng tiền nạp (DEPOSIT), rút (WITHDRAWAL), tiền ký quỹ bảo chứng (ESCROW_DEPOSIT), giải ngân (ESCROW_RELEASE), và hoàn tiền (REFUND).
     * 4. Tính toán doanh thu phí nền tảng (Platform Fee) dựa trên tham số PLATFORM_FEE_RATE hoặc các giao dịch phí thực tế.
     * 5. Tổng hợp chỉ số rủi ro, tỷ lệ tranh chấp, tỷ lệ chuyển đổi xác minh danh tính và xu hướng 6 tháng gần nhất.
     * 
     * @param from Ngày bắt đầu thống kê
     * @param to Ngày kết thúc thống kê
     * @return AnalyticsSummaryResponse bảng chỉ số tài chính tổng quan sàn
     */
    AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to);

    /**
     * [UC-41] Xuất báo cáo tài chính kế toán ra định dạng tệp CSV chuẩn UTF-8 BOM.
     * Có cơ chế phòng chống cạn kiệt bộ nhớ máy chủ (OOM Protection) và ngăn chặn mã độc Formula Injection.
     * 
     * @param type Phân loại báo cáo CSV cần xuất (SUMMARY, LEDGER, CENTERS, TUTORS, CLIENTS)
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return Mảng byte chứa nội dung tệp CSV kèm UTF-8 BOM Header
     */
    byte[] exportCsv(String type, LocalDate from, LocalDate to);

    /**
     * [UC-41] Tác vụ chạy nền định kỳ tự động tổng hợp báo cáo chỉ số tài chính hàng ngày.
     * 
     * @return Số lượng bản ghi báo cáo được khởi tạo thành công
     */
    int generateScheduledDailyReport();

    /**
     * [UC-43] Báo cáo phân tích hiệu quả tài chính và doanh thu của tất cả các Trung tâm gia sư.
     * 
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return Danh sách CenterFinancialAnalyticsResponse cho từng trung tâm
     */
    List<CenterFinancialAnalyticsResponse> getCenterAnalytics(LocalDate from, LocalDate to);

    /**
     * [UC-43] Báo cáo chi tiết phân tích tài chính của một Trung tâm gia sư cụ thể theo ID.
     * 
     * @param centerId Định danh trung tâm gia sư
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return CenterFinancialAnalyticsResponse thông tin tài chính chi tiết của trung tâm
     */
    CenterFinancialAnalyticsResponse getCenterAnalyticsByCenterId(Long centerId, LocalDate from, LocalDate to);

    /**
     * [UC-41] Báo cáo phân tích tài chính thu nhập và số lớp giảng dạy của toàn bộ Gia sư.
     * 
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return Danh sách TutorFinancialAnalyticsResponse
     */
    List<TutorFinancialAnalyticsResponse> getTutorAnalytics(LocalDate from, LocalDate to);

    /**
     * [UC-41] Báo cáo phân tích chi tiêu học phí và số lớp học của toàn bộ Phụ huynh / Học sinh.
     * 
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @return Danh sách ClientFinancialAnalyticsResponse
     */
    List<ClientFinancialAnalyticsResponse> getClientAnalytics(LocalDate from, LocalDate to);

    /**
     * [UC-58] Sổ cái kế toán đa chiều phân trang đối soát toàn bộ các bút toán giao dịch trên sàn.
     * 
     * @param role Lọc theo vai trò đối tượng giao dịch (TUTOR, CLIENT, TUTOR_CENTER)
     * @param direction Lọc theo chiều dòng tiền (INFLOW, OUTFLOW)
     * @param search Từ khóa tìm kiếm mã tham chiếu hoặc email người dùng
     * @param from Ngày bắt đầu
     * @param to Ngày kết thúc
     * @param pageable Cấu hình phân trang và sắp xếp
     * @return Trang kết quả FinancialLedgerItemResponse chi tiết từng bút toán
     */
    Page<FinancialLedgerItemResponse> getFinancialLedger(
            String role, String direction, String search, LocalDate from, LocalDate to, Pageable pageable);
}
