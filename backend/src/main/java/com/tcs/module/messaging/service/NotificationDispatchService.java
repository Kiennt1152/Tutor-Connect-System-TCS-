package com.tcs.module.messaging.service;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.enums.NotificationType;
import java.util.Map;

/**
 * ============================================================================
 * [UC-53] PHÂN PHỐI THÔNG BÁO ĐA KÊNH HỆ THỐNG (NOTIFICATION DISPATCH SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Đồng tác giả: tienanh6677 (Nguyễn Tiến Anh)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Định nghĩa dịch vụ điều phối và gửi thông báo đa kênh tới người dùng trên toàn hệ thống.
 *   - Hỗ trợ gửi thông báo tức thời trong ứng dụng (In-App Push) và gửi thư điện tử (Email).
 * 
 * Chức năng chính:
 *   1. Thông báo In-App: Khởi tạo thông báo chuông trực tiếp trên giao diện kèm thực thể liên kết.
 *   2. Gửi Email thông báo: Phân phối nội dung thông báo quan trọng tới hòm thư cá nhân của người dùng.
 *   3. Gửi thông báo từ Template: Tự động nội suy từ mẫu thông báo động kèm cơ chế dự phòng an toàn.
 * ============================================================================
 */
public interface NotificationDispatchService {

    /**
     * [UC-53] Gửi thông báo trực tiếp trong ứng dụng (In-App) cho người dùng.
     * 
     * @param user Người dùng nhận thông báo
     * @param type Phân loại thông báo (SYSTEM, CONTRACT, CHAT, PAYMENT)
     * @param title Tiêu đề thông báo
     * @param content Nội dung thông báo
     * @param referenceType Loại thực thể liên kết (ví dụ: CONTRACT, CLASS, PENALTY)
     * @param referenceId Định danh thực thể liên kết
     */
    void notifyUser(User user, NotificationType type, String title, String content, String referenceType, Long referenceId);

    /**
     * [UC-53] Gửi thư điện tử (Email) thông báo trực tiếp cho người dùng.
     * 
     * @param user Người dùng nhận email
     * @param subject Tiêu đề thư điện tử
     * @param body Nội dung thư điện tử
     */
    void notifyUserByEmail(User user, String subject, String body);

    /**
     * [UC-53] Gửi thông báo đa kênh được tạo tự động từ mẫu Notification Template kèm nội dung dự phòng.
     * 
     * Luồng xử lý:
     * 1. Tìm kiếm và nội suy mẫu thông báo qua NotificationTemplateService.renderEnabled.
     * 2. Nếu mẫu chưa tồn tại hoặc bị tắt, tự động sử dụng fallbackTitle và fallbackContent.
     * 3. Chuyển giao sang notifyUser để phát thông báo chuông và đẩy vào hàng đợi thông báo.
     * 
     * @param user Người nhận thông báo
     * @param type Loại thông báo
     * @param templateCode Mã mẫu thông báo nghiệp vụ
     * @param variables Bộ giá trị nội suy vào placeholder của template
     * @param fallbackTitle Tiêu đề dự phòng nếu template không khả dụng
     * @param fallbackContent Nội dung dự phòng nếu template không khả dụng
     * @param referenceType Loại thực thể liên kết
     * @param referenceId Định danh thực thể liên kết
     */
    void notifyUserFromTemplate(
            User user,
            NotificationType type,
            String templateCode,
            Map<String, ?> variables,
            String fallbackTitle,
            String fallbackContent,
            String referenceType,
            Long referenceId);
}
