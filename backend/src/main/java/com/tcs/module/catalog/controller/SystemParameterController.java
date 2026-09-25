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

    /**
     * [UC-38] [UC-46]: Tra cứu danh sách tham số cấu hình vận hành nền tảng có hỗ trợ lọc từ khóa.
     * 
     * @param prefix Tiền tố nhóm tham số (ví dụ: PLATFORM_, ESCROW_, SECURITY_)
     * @param keyword Từ khóa tìm kiếm trong khóa hoặc mô tả tham số
     * @return Danh sách cấu hình tham số {@link SystemParameterResponse}
     */
    @GetMapping
    public List<SystemParameterResponse> getParameters(
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) String keyword
    ) {
        return systemParameterService.getParameters(prefix, keyword);
    }

    /**
     * [UC-38]: Xem chi tiết giá trị và thuộc tính của một tham số cấu hình theo ID.
     * 
     * @param parameterId Mã định danh tham số
     * @return {@link SystemParameterResponse} Thông tin chi tiết tham số
     */
    @GetMapping("/{parameterId}")
    public SystemParameterResponse getParameter(@PathVariable Long parameterId) {
        return systemParameterService.getParameter(parameterId);
    }

    /**
     * [UC-38]: Thêm mới một tham số cấu hình tùy biến vào hệ thống.
     * 
     * @param request Dữ liệu tham số gồm key, value, kiểu dữ liệu và mô tả {@link UpsertSystemParameterRequest}
     * @return {@link SystemParameterResponse} Tham số vừa tạo thành công
     */
    @PostMapping
    public SystemParameterResponse createParameter(@Valid @RequestBody UpsertSystemParameterRequest request) {
        return systemParameterService.createParameter(request);
    }

    /**
     * [UC-38] [UC-46]: Điều chỉnh giá trị của một tham số hệ thống (ví dụ: cập nhật tỷ lệ phí sàn PLATFORM_FEE_RATE).
     * 
     * @param parameterId Mã định danh tham số cần sửa
     * @param request Dữ liệu cập nhật giá trị mới {@link UpsertSystemParameterRequest}
     * @return {@link SystemParameterResponse} Tham số sau khi cập nhật
     */
    @PatchMapping("/{parameterId}")
    public SystemParameterResponse updateParameter(
            @PathVariable Long parameterId,
            @Valid @RequestBody UpsertSystemParameterRequest request
    ) {
        return systemParameterService.updateParameter(parameterId, request);
    }

    /**
     * [UC-38]: Xóa một tham số cấu hình tùy biến không còn nhu cầu sử dụng (chặn xóa các tham số bắt buộc).
     * 
     * @param parameterId Mã định danh tham số cần xóa
     */
    @DeleteMapping("/{parameterId}")
    public void deleteParameter(@PathVariable Long parameterId) {
        systemParameterService.deleteParameter(parameterId);
    }
}
