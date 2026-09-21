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

    List<NotificationTemplateResponse> findAll();
    NotificationTemplateResponse findById(Long templateId);
    NotificationTemplateResponse create(UpsertNotificationTemplateRequest request);
    NotificationTemplateResponse update(Long templateId, UpsertNotificationTemplateRequest request);
    NotificationTemplateResponse disable(Long templateId);
    NotificationTemplatePreviewResponse preview(PreviewNotificationTemplateRequest request);
    Optional<RenderedTemplate> renderEnabled(String code, Map<String, ?> variables);
}
