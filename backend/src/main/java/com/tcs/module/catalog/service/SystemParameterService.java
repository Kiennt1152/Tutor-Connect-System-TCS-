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

    /**
     * [UC-57] Liệt kê và lọc danh sách tham số cấu hình hệ thống theo tiền tố khóa hoặc từ khóa.
     * 
     * @param prefix Tiền tố khóa cần lọc (ví dụ: PLATFORM_, ESCROW_, SYSTEM_)
     * @param keyword Từ khóa tìm kiếm tự do trong tên khóa hoặc giá trị tham số
     * @return Danh sách SystemParameterResponse sắp xếp theo thứ tự chữ cái của paramKey
     */
    List<SystemParameterResponse> getParameters(String prefix, String keyword);

    /**
     * [UC-57] Lấy thông tin chi tiết một tham số cấu hình hệ thống theo ID.
     * 
     * @param parameterId Định danh tham số hệ thống
     * @return SystemParameterResponse chi tiết khóa, giá trị và mô tả
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy tham số
     */
    SystemParameterResponse getParameter(Long parameterId);

    /**
     * [UC-57] Tạo mới một tham số cấu hình vận hành hệ thống.
     * 
     * Luồng xử lý:
     * 1. Chuẩn hóa tên khóa (in hoa, loại bỏ khoảng trắng).
     * 2. Kiểm tra tính duy nhất của khóa; ném lỗi nếu đã tồn tại.
     * 3. Kiểm tra định dạng giá trị cấu hình theo loại tham số (số thập phân, JSON, v.v.).
     * 4. Lưu tham số vào CSDL và ghi nhận vết kiểm toán CREATE_SYSTEM_PARAMETER.
     * 
     * @param request Dữ liệu tạo mới tham số
     * @return SystemParameterResponse thông tin tham số vừa tạo
     * @throws IllegalArgumentException nếu khóa đã tồn tại hoặc giá trị không hợp lệ
     */
    SystemParameterResponse createParameter(UpsertSystemParameterRequest request);

    /**
     * [UC-57] Cập nhật giá trị và mô tả của tham số cấu hình hệ thống hiện có.
     * 
     * Luồng xử lý:
     * 1. Tìm bản ghi tham số theo parameterId.
     * 2. Kiểm tra các quy tắc an toàn: Chặn đổi tên khóa đối với các tham số cốt lõi (MANDATORY_KEYS).
     * 3. Xác thực định dạng giá trị mới và lưu cập nhật vào CSDL.
     * 4. Ghi nhận nhật ký kiểm toán UPDATE_SYSTEM_PARAMETER so vết JSON Diff giữa giá trị cũ và mới.
     * 
     * @param parameterId Định danh tham số cần sửa
     * @param request Dữ liệu cập nhật mới
     * @return SystemParameterResponse thông tin tham số sau khi cập nhật
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy tham số
     * @throws IllegalArgumentException nếu cố tình đổi tên khóa bắt buộc hoặc giá trị sai định dạng
     */
    SystemParameterResponse updateParameter(Long parameterId, UpsertSystemParameterRequest request);

    /**
     * [UC-57] Xóa bỏ một tham số cấu hình tùy chỉnh khỏi hệ thống.
     * 
     * Luồng xử lý:
     * 1. Tìm bản ghi tham số theo parameterId.
     * 2. Kiểm tra an toàn: Chặn tuyệt đối hành vi xóa các tham số bắt buộc sàn (MANDATORY_KEYS).
     * 3. Xóa tham số khỏi CSDL và ghi nhận vết kiểm toán DELETE_SYSTEM_PARAMETER.
     * 
     * @param parameterId Định danh tham số cần xóa
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy tham số
     * @throws IllegalArgumentException nếu tham số thuộc danh sách khóa bắt buộc không thể xóa
     */
    void deleteParameter(Long parameterId);
}
