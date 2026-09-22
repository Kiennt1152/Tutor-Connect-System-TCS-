package com.tcs.module.catalog.service;

import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.dto.response.SystemParameterResponse;
import java.util.List;

/**
 * ============================================================================
 * [UC-57] CẤU HÌNH THAM SỐ VẬN HÀNH HỆ THỐNG (SYSTEM PARAMETER SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Định nghĩa dịch vụ quản lý các tham số cấu hình động toàn hệ thống Tutor Connect.
 *   - Hỗ trợ thay đổi tham số vận hành nghiệp vụ ngay lập tức mà không cần khởi động lại máy chủ.
 * 
 * Chức năng chính:
 *   1. Danh sách tham số: Hỗ trợ tìm kiếm theo từ khóa và phân nhóm theo tiền tố tính năng.
 *   2. Thêm mới và cập nhật: Thiết lập giá trị cấu hình dạng chuỗi, số hoặc JSON.
 *   3. Xóa tham số: Thu hồi các tham số không còn sử dụng nhưng bảo vệ tuyệt đối các khóa cốt lõi sàn.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tra cứu danh sách tham số cấu hình hệ thống (`getParameters`).
 *   - Bước 2: Tiếp nhận yêu cầu tạo mới hoặc cập nhật giá trị tham số (`createParameter`, `updateParameter`).
 *   - Bước 3: Kiểm tra tính hợp lệ và cập nhật dữ liệu cấu hình trong CSDL.
 *   - Bước 4: Xóa tham số khi có yêu cầu hợp lệ (`deleteParameter`).
 * ============================================================================
 */
public interface SystemParameterService {

    /** Liệt kê tham số hệ thống, có thể lọc theo tiền tố khóa (prefix) hoặc từ khóa. */
    List<SystemParameterResponse> getParameters(String prefix, String keyword);

    SystemParameterResponse getParameter(Long parameterId);

    SystemParameterResponse createParameter(UpsertSystemParameterRequest request);

    SystemParameterResponse updateParameter(Long parameterId, UpsertSystemParameterRequest request);

    void deleteParameter(Long parameterId);
}
