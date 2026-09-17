package com.tcs.module.messaging.controller;

import com.tcs.module.messaging.dto.request.PreviewNotificationTemplateRequest;
import com.tcs.module.messaging.dto.request.UpsertNotificationTemplateRequest;
import com.tcs.module.messaging.dto.response.NotificationTemplatePreviewResponse;
import com.tcs.module.messaging.dto.response.NotificationTemplateResponse;
import com.tcs.module.messaging.service.NotificationTemplateService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * ====================================================================================================
 * [UC-35] CẤU HÌNH MẪU THÔNG BÁO HỆ THỐNG (SYSTEM NOTIFICATION TEMPLATES MANAGEMENT)
 * ====================================================================================================
 * Controller quản trị cho phép Platform Admin định nghĩa, tùy biến nội dung và xem trước (Preview)
 * các mẫu thông báo tự động trên toàn sàn (In-App notification, Email, SMS, Webhook).
 * 
 * Các chức năng cốt lõi:
 * 1. Danh sách mẫu thông báo: Truy xuất tất cả template đang hoạt động hoặc vô hiệu hóa.
 * 2. Chi tiết template: Tra cứu cấu trúc biến thay thế {{userName}}, {{classCode}}, {{amount}},...
 * 3. Thêm mới / Cập nhật template: Soạn thảo tiêu đề, nội dung, kênh gửi và mã sự kiện (event code).
 * 4. Vô hiệu hóa template: Tạm ngưng kích hoạt mà không xóa vĩnh viễn khỏi CSDL.
 * 5. Xem trước (Preview): Điền dữ liệu giả lập (mock payload) vào template để kiểm tra định dạng trước khi áp dụng.
 * 
 * @author mduc1011-swp (Đức)
 */
@RestController
@RequestMapping("/api/platform/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {
    private final NotificationTemplateService service;

    /**
     * [UC-35] Lấy danh sách toàn bộ mẫu thông báo trong hệ thống.
     * 
     * @return Danh sách các mẫu thông báo {@link NotificationTemplateResponse}
     */
    @GetMapping
    public List<NotificationTemplateResponse> findAll() { return service.findAll(); }

    /**
     * [UC-35] Tra cứu chi tiết một mẫu thông báo theo ID.
     * 
     * @param templateId ID định danh của mẫu thông báo
     * @return Thông tin chi tiết mẫu thông báo {@link NotificationTemplateResponse}
     */
    @GetMapping("/{templateId}")
    public NotificationTemplateResponse findById(@PathVariable Long templateId) { return service.findById(templateId); }

    /**
     * [UC-35] Tạo mới mẫu thông báo hệ thống.
     * 
     * @param request Dữ liệu tạo mẫu thông báo {@link UpsertNotificationTemplateRequest} (eventCode, title, body, channel,...)
     * @return Mẫu thông báo vừa tạo thành công
     */
    @PostMapping
    public NotificationTemplateResponse create(@Valid @RequestBody UpsertNotificationTemplateRequest request) {
        return service.create(request);
    }

    /**
     * [UC-35] Cập nhật nội dung hoặc cấu hình mẫu thông báo hiện có.
     * 
     * @param templateId ID của mẫu thông báo cần cập nhật
     * @param request Dữ liệu cập nhật {@link UpsertNotificationTemplateRequest}
     * @return Mẫu thông báo sau khi chỉnh sửa
     */
    @PatchMapping("/{templateId}")
    public NotificationTemplateResponse update(@PathVariable Long templateId,
            @Valid @RequestBody UpsertNotificationTemplateRequest request) {
        return service.update(templateId, request);
    }

    /**
     * [UC-35] Vô hiệu hóa (Disable/Soft-delete) mẫu thông báo.
     * 
     * @param templateId ID của mẫu thông báo cần vô hiệu hóa
     * @return Trạng thái mẫu thông báo sau khi vô hiệu hóa
     */
    @DeleteMapping("/{templateId}")
    public NotificationTemplateResponse disable(@PathVariable Long templateId) { return service.disable(templateId); }

    /**
     * [UC-35] Xem trước mẫu thông báo (Preview) với biến số giả lập.
     * Hỗ trợ Admin kiểm tra hiển thị placeholder và cú pháp định dạng trước khi lưu hoặc gửi hàng loạt.
     * 
     * @param request Yêu cầu xem trước {@link PreviewNotificationTemplateRequest} kèm template content và tham số mock
     * @return Kết quả nội dung sau khi render placeholder {@link NotificationTemplatePreviewResponse}
     */
    @PostMapping("/preview")
    public NotificationTemplatePreviewResponse preview(@Valid @RequestBody PreviewNotificationTemplateRequest request) {
        return service.preview(request);
    }
}
