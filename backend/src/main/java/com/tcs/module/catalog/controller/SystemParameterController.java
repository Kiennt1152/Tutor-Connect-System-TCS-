package com.tcs.module.catalog.controller;

import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.dto.response.SystemParameterResponse;
import com.tcs.module.catalog.service.SystemParameterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * [UC-57] CẤU HÌNH THAM SỐ VẬN HÀNH HỆ THỐNG (SYSTEM PARAMETER CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Quản trị viên điều chỉnh các tham số vận hành nền tảng theo thời gian thực (Hot-reload).
 *   - Kiểm soát các thiết lập cốt lõi: Tỷ lệ phí nền tảng, trạng thái bảo trì, giới hạn OTP, thời gian giữ tiền Escrow.
 * 
 * Chức năng chính:
 *   1. Danh sách tham số: Hỗ trợ tìm kiếm theo từ khóa và lọc theo tiền tố nhóm chức năng.
 *   2. Thêm mới & Cập nhật: Lưu trữ cấu hình dạng khóa - giá trị (Key - Value) kèm mô tả chi tiết.
 *   3. Xóa tham số: Gỡ bỏ các tham số không còn hiệu lực sử dụng trong hệ thống.
 *   4. Ghi vết kiểm toán: Tự động ghi nhật ký Audit Log khi có bất kỳ điều chỉnh cấu hình nào.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên gửi yêu cầu truy vấn danh sách cấu hình (`listParameters`).
 *   - Bước 2: Chọn tham số để xem chi tiết hoặc điền form tạo mới (`UpsertSystemParameterRequest`).
 *   - Bước 3: Gửi yêu cầu cập nhật (`upsertParameter`), hệ thống xác thực quyền Quản trị viên và lưu vào CSDL.
 *   - Bước 4: Khi cần thu hồi tham số, gọi API xóa (`deleteParameter`) kèm ghi nhận lịch sử kiểm toán.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/catalog/parameters")
@RequiredArgsConstructor
public class SystemParameterController {

    private final SystemParameterService systemParameterService;

    // =========================================================================
    // LUỒNG 10: CẤU HÌNH THAM SỐ NỀN TẢNG & TỶ LỆ PHÍ ĐỘNG (UC-46)
    // =========================================================================

    // Luồng 10 - Tra cứu danh sách tham số nền tảng
    @GetMapping
    public List<SystemParameterResponse> getParameters(
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) String keyword
    ) {
        return systemParameterService.getParameters(prefix, keyword);
    }

    @GetMapping("/{parameterId}")
    public SystemParameterResponse getParameter(@PathVariable Long parameterId) {
        return systemParameterService.getParameter(parameterId);
    }

    // Luồng 10 - Tạo mới tham số hệ thống
    @PostMapping
    public SystemParameterResponse createParameter(@Valid @RequestBody UpsertSystemParameterRequest request) {
        return systemParameterService.createParameter(request);
    }

    // Luồng 10 - Bước 1: Tiếp nhận PATCH request cập nhật tham số (như PLATFORM_FEE_RATE)
    @PatchMapping("/{parameterId}")
    public SystemParameterResponse updateParameter(
            @PathVariable Long parameterId,
            @Valid @RequestBody UpsertSystemParameterRequest request
    ) {
        return systemParameterService.updateParameter(parameterId, request);
    }

    // Luồng 10 - Xóa tham số tùy chỉnh (có chặn MANDATORY_KEYS ở Service)
    @DeleteMapping("/{parameterId}")
    public void deleteParameter(@PathVariable Long parameterId) {
        systemParameterService.deleteParameter(parameterId);
    }
}
