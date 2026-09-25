package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.PageTaskItemResponse;
import com.tcs.module.platform.dto.response.TaskQueueSummaryResponse;
import com.tcs.module.platform.service.PlatformTaskQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * [UC-64] ĐIỀU PHỐI HÀNG ĐỢI TÁC VỤ QUẢN TRỊ (PLATFORM TASK CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Điểm kết nối API phục vụ hàng đợi trực ban khẩn cấp của Quản trị viên hệ thống.
 *   - Tập trung các luồng phê duyệt và giải quyết khiếu nại giúp nâng cao tốc độ phản hồi vận hành.
 * 
 * Chức năng chính:
 *   1. Báo cáo tổng quan hàng đợi: Thống kê tổng task tồn đọng, task vi phạm SLA và tổng số tiền rủi ro tài chính.
 *   2. Tra cứu tác vụ phân trang: Danh sách việc cần xử lý tập hợp từ KYC, Báo cáo lách sàn, Ticket, Rút tiền, Hoàn tiền, Tranh chấp.
 *   3. Bộ lọc điều phối: Phân luồng theo loại hình tác vụ, mức độ ưu tiên nghiệp vụ và cờ cảnh báo quá hạn cam kết SLA.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Dashboard trực ban gọi API `getSummary` để hiển thị các chỉ số đo lường hiệu suất.
 *   - Bước 2: Quản trị viên truy vấn danh sách công việc `listTasks` kèm bộ lọc theo thẩm quyền.
 *   - Bước 3: `taskQueueService` truy xuất và tính toán thứ tự ưu tiên, trả về danh sách đã phân trang.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/platform/tasks")
@RequiredArgsConstructor
public class PlatformTaskController {
    private final PlatformTaskQueueService taskQueueService;

    /**
     * [UC-59]: Lấy số liệu tổng hợp về hàng đợi tác vụ nền (Queue Health & SLA Summary).
     * 
     * @return {@link TaskQueueSummaryResponse} Tổng số tác vụ đang chờ, đang chạy, lỗi và số ca vi phạm SLA
     */
    @GetMapping("/summary")
    public TaskQueueSummaryResponse getSummary() {
        return taskQueueService.getSummary();
    }

    /**
     * [UC-59]: Tra cứu, lọc và phân trang danh sách các tác vụ nền đang được giám sát.
     * 
     * @param type Phân loại tác vụ (ESCROW_RELEASE, EMAIL_DISPATCH, SYNC_DATA...)
     * @param priority Mức độ ưu tiên (HIGH, MEDIUM, LOW)
     * @param slaBreached Lọc các tác vụ đã vi phạm thời hạn cam kết xử lý SLA
     * @param page Số trang truy vấn (mặc định 0)
     * @param size Số bản ghi mỗi trang (mặc định 20)
     * @return {@link PageTaskItemResponse} Danh sách tác vụ kèm trạng thái phân trang
     */
    @GetMapping
    public PageTaskItemResponse listTasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return taskQueueService.listTasks(type, priority, slaBreached, page, size);
    }
}
