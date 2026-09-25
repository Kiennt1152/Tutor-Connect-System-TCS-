package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.PageTaskItemResponse;
import com.tcs.module.platform.dto.response.TaskQueueSummaryResponse;

/**
 * ============================================================================
 * [UC-64] ĐIỀU PHỐI HÀNG ĐỢI TÁC VỤ TRỰC BAN KHẨN CẤP (TASK QUEUE SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Điều phối hàng đợi trực ban khẩn cấp, tổng hợp công việc từ toàn bộ các phân hệ trên nền tảng.
 *   - Đảm bảo thời gian xử lý cam kết dịch vụ (SLA) và kiểm soát rủi ro tài chính kịp thời.
 * 
 * Chức năng chính:
 *   1. Báo cáo tổng quan hàng đợi: Thống kê số lượng việc tồn đọng, việc quá hạn SLA và tiền rủi ro.
 *   2. Danh sách tác vụ tập trung: Phân trang các nhiệm vụ cần xử lý từ KYC, Báo cáo, Ticket, Rút tiền, Tranh chấp.
 *   3. Phân cấp ưu tiên: Tự động phân loại mức độ khẩn cấp giúp Admin xử lý các vấn đề nghiêm trọng trước.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên truy vấn số liệu tổng quan bàn trực ban (`getSummary`).
 *   - Bước 2: Tải danh sách tác vụ theo các bộ lọc phân hệ và mức độ ưu tiên (`listTasks`).
 *   - Bước 3: Điều hướng trực ban giải quyết các tác vụ theo thứ tự khẩn cấp.
 * ============================================================================
 */
public interface PlatformTaskQueueService {
    /**
     * [UC-64] Tổng hợp báo cáo số liệu hàng đợi tác vụ trực ban khẩn cấp.
     * 
     * Luồng xử lý:
     * 1. Tập hợp tất cả tác vụ đang mở từ 6 repository nguồn (Xác minh, Báo cáo, Ticket, Rút tiền, Hoàn tiền, Tranh chấp).
     * 2. Thống kê số lượng tồn đọng theo từng phân hệ và theo mức độ ưu tiên (URGENT, HIGH, MEDIUM, LOW).
     * 3. Tính tổng số tác vụ đã vi phạm cam kết SLA và tổng giá trị tiền ký quỹ/giao dịch đang chịu rủi ro (Money At Risk).
     * 
     * @return TaskQueueSummaryResponse chứa các chỉ số đếm phân loại, số vụ trễ hạn và tổng giá trị tiền rủi ro
     */
    TaskQueueSummaryResponse getSummary();

    /**
     * [UC-64] Truy vấn danh sách tác vụ trực ban theo bộ lọc đa chiều và phân trang.
     * 
     * Luồng xử lý:
     * 1. Tập hợp các tác vụ đang chờ xử lý từ các nguồn nghiệp vụ nền tảng.
     * 2. Áp dụng các bộ lọc: loại tác vụ (type), mức ưu tiên (priority), trạng thái vi phạm SLA (slaBreached).
     * 3. Sắp xếp danh sách theo trọng số ưu tiên giảm dần, hạn xử lý (due date) tăng dần và thời điểm tạo.
     * 4. Cắt phân trang theo số trang (page) và kích thước (size) yêu cầu.
     * 
     * @param type Phân loại tác vụ cần lọc (VERIFICATION, REPORT, SUPPORT_TICKET, WITHDRAWAL, REFUND_REQUEST, DISPUTE, CIRCUMVENTION hoặc ALL)
     * @param priority Mức độ ưu tiên cần lọc (URGENT, HIGH, MEDIUM, LOW hoặc ALL)
     * @param slaBreached Cờ lọc theo tình trạng trễ hạn cam kết dịch vụ (true: quá hạn, false: trong hạn, null: tất cả)
     * @param page Số trang truy vấn (bắt đầu từ 0)
     * @param size Số lượng bản ghi trên một trang
     * @return PageTaskItemResponse danh sách tác vụ phân trang kèm thông tin SLA và đường dẫn điều hướng xử lý
     */
    PageTaskItemResponse listTasks(String type, String priority, Boolean slaBreached, int page, int size);

    /**
     * [UC-64] Phương thức tiện ích truy vấn tác vụ phân trang chỉ theo loại tác vụ (mặc định không lọc mức ưu tiên và SLA).
     * 
     * @param type Phân loại tác vụ cần lọc hoặc ALL
     * @param page Số trang truy vấn (bắt đầu từ 0)
     * @param size Số lượng bản ghi trên một trang
     * @return PageTaskItemResponse danh sách tác vụ phân trang
     */
    default PageTaskItemResponse listTasks(String type, int page, int size) {
        return listTasks(type, null, null, page, size);
    }
}
