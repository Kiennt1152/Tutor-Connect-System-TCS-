package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.PreviewNotificationTemplateRequest;
import com.tcs.module.messaging.dto.request.UpsertNotificationTemplateRequest;
import com.tcs.module.messaging.dto.response.NotificationTemplatePreviewResponse;
import com.tcs.module.messaging.dto.response.NotificationTemplateResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * [UC-35] QUẢN TRỊ MẪU THÔNG BÁO HỆ THỐNG (NOTIFICATION TEMPLATE SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Định nghĩa các dịch vụ quản lý mẫu thông báo động hệ thống (System Notification Templates).
 *   - Hỗ trợ thay thế placeholder thời gian thực, xem trước bản dựng và kiểm soát kích hoạt mẫu.
 * 
 * Chức năng chính:
 *   1. Danh sách & tra cứu mẫu: Lấy thông tin các mẫu thông báo phục vụ cấu hình hệ thống.
 *   2. Quản lý cấu hình mẫu: Tạo mới, cập nhật tiêu đề, nội dung và vô hiệu hóa mẫu thông báo theo mã nghiệp vụ.
 *   3. Xem trước và nội suy mẫu: Hỗ trợ điền dữ liệu giả lập để xem trước giao diện thông báo trước khi áp dụng.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu quản lý hoặc render mẫu thông báo từ người dùng quản trị.
 *   - Bước 2: Tìm kiếm và bóc tách các biến số placeholder dạng {{variableName}} trong nội dung mẫu.
 *   - Bước 3: Nội suy dữ liệu thực tế vào mẫu hoặc lưu cấu hình mới vào CSDL.
 *   - Bước 4: Trả về kết quả hiển thị hoặc bản ghi cấu hình hoàn chỉnh.
 * ============================================================================
 */
public interface NotificationTemplateService {
    record RenderedTemplate(String title, String content) {}

    /**
     * [UC-35] Lấy danh sách toàn bộ các mẫu thông báo hệ thống được sắp xếp theo mã code.
     * 
     * @return Danh sách NotificationTemplateResponse đại diện cho tất cả mẫu thông báo
     */
    List<NotificationTemplateResponse> findAll();

    /**
     * [UC-35] Tìm kiếm thông tin chi tiết một mẫu thông báo theo ID.
     * 
     * @param templateId Định danh mẫu thông báo
     * @return NotificationTemplateResponse chi tiết mẫu thông báo
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy mẫu
     */
    NotificationTemplateResponse findById(Long templateId);

    /**
     * [UC-35] Tạo mới một mẫu thông báo hệ thống.
     * 
     * Luồng xử lý:
     * 1. Chuẩn hóa và kiểm tra tính duy nhất của mã template code.
     * 2. Xác thực cú pháp placeholder {{variable}} trong tiêu đề và nội dung.
     * 3. Lưu mẫu mới vào CSDL và ghi nhận vết kiểm toán CREATE_NOTIFICATION_TEMPLATE.
     * 
     * @param request DTO dữ liệu tạo mới mẫu thông báo
     * @return NotificationTemplateResponse thông tin mẫu vừa khởi tạo
     * @throws IllegalArgumentException nếu mã code đã tồn tại hoặc cú pháp template sai
     */
    NotificationTemplateResponse create(UpsertNotificationTemplateRequest request);

    /**
     * [UC-35] Cập nhật nội dung và cấu hình của mẫu thông báo đã có.
     * 
     * @param templateId Định danh mẫu cần chỉnh sửa
     * @param request Dữ liệu cập nhật mẫu
     * @return NotificationTemplateResponse thông tin mẫu sau khi cập nhật
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy mẫu
     * @throws IllegalArgumentException nếu mã code bị trùng lặp với mẫu khác
     */
    NotificationTemplateResponse update(Long templateId, UpsertNotificationTemplateRequest request);

    /**
     * [UC-35] Vô hiệu hóa một mẫu thông báo (chuyển enabled thành false).
     * 
     * @param templateId Định danh mẫu cần tắt
     * @return NotificationTemplateResponse thông tin mẫu sau khi vô hiệu hóa
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy mẫu
     * @throws IllegalArgumentException nếu mẫu đã ở trạng thái tắt từ trước
     */
    NotificationTemplateResponse disable(Long templateId);

    /**
     * [UC-35] Xem trước kết quả nội suy dữ liệu vào mẫu thông báo kèm danh sách biến chưa giải quyết.
     * 
     * Luồng xử lý:
     * 1. Xác thực cú pháp template của chuỗi tiêu đề và nội dung xem trước.
     * 2. Nội suy các giá trị từ bản đồ biến số variables vào các vị trí {{placeholder}}.
     * 3. Thu thập và trả về danh sách các placeholder còn sót lại chưa có giá trị thay thế.
     * 
     * @param request DTO yêu cầu xem trước chứa mẫu văn bản và các cặp biến số
     * @return NotificationTemplatePreviewResponse bản xem trước tiêu đề, nội dung và các biến chưa giải quyết
     */
    NotificationTemplatePreviewResponse preview(PreviewNotificationTemplateRequest request);

    /**
     * [UC-35] Nội suy và kết xuất mẫu thông báo đang kích hoạt dựa trên mã nghiệp vụ và bộ biến số.
     * 
     * Luồng xử lý:
     * 1. Tìm mẫu thông báo theo mã code không phân biệt hoa thường.
     * 2. Kiểm tra nếu mẫu đang được bật (enabled = true).
     * 3. Điền các giá trị từ variables vào các placeholder {{...}} trong tiêu đề và nội dung.
     * 
     * @param code Mã định danh mẫu thông báo nghiệp vụ (ví dụ: PENALTY_ISSUED, CONTRACT_SIGNED)
     * @param variables Bản đồ tên biến và giá trị cần nội suy
     * @return Optional chứa RenderedTemplate (title, content) nếu mẫu hợp lệ và đang bật; Optional.empty() nếu không tìm thấy hoặc bị vô hiệu hóa
     */
    Optional<RenderedTemplate> renderEnabled(String code, Map<String, ?> variables);
}
