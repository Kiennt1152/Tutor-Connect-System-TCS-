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
    TaskQueueSummaryResponse getSummary();
    PageTaskItemResponse listTasks(String type, String priority, Boolean slaBreached, int page, int size);

    default PageTaskItemResponse listTasks(String type, int page, int size) {
        return listTasks(type, null, null, page, size);
    }
}
