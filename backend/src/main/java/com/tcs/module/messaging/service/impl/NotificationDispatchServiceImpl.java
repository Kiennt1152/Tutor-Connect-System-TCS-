package com.tcs.module.messaging.service.impl;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.entity.NotificationQueue;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationQueueRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationTemplateService;
import com.tcs.module.messaging.service.EmailService;
import com.tcs.module.messaging.service.NotificationDispatchService;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ====================================================================================================
 * [UC-53] DỊCH VỤ PHÂN PHỐI THÔNG BÁO ĐA KÊNH (NOTIFICATION DISPATCH SERVICE IMPLEMENTATION)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Điều phối gửi thông báo đồng thời qua các kênh: Chuông In-App, Email và Webhook.
 * 2. Theo dõi trạng thái đã gửi, lỗi gửi và cơ chế thử lại tự động khi gặp sự cố mạng.
 * * @author Hoàng Minh Đức (mduc1011-swp)
 * @author Nguyễn Tiến Anh (tienanh6677)
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final NotificationQueueRepository notificationQueueRepository;
    private final EmailService emailService;
    private final NotificationTemplateService notificationTemplateService;

    /**
     * [UC-53] Hiện thực gửi thông báo trong ứng dụng và lưu vào hàng đợi phân phối.
     * 
     * Luồng xử lý:
     * 1. Chuẩn hóa các thuật ngữ hiển thị cho người dùng qua normalizeUserFacingText.
     * 2. Tạo bản ghi thực thể Notification và lưu vào CSDL với trạng thái SENT.
     * 3. Tạo bản ghi NotificationQueue kênh IN_APP để quản lý lịch sử phân phối.
     * 
     * @param user Người nhận
     * @param type Phân loại thông báo
     * @param title Tiêu đề
     * @param content Nội dung
     * @param referenceType Loại thực thể liên kết
     * @param referenceId ID thực thể liên kết
     */
    @Override
    @Transactional
    public void notifyUser(
            User user,
            NotificationType type,
            String title,
            String content,
            String referenceType,
            Long referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(normalizeUserFacingText(title));
        notification.setContent(normalizeUserFacingText(content));
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        Notification saved = notificationRepository.save(notification);

        NotificationQueue queue = new NotificationQueue();
        queue.setNotification(saved);
        queue.setChannel("IN_APP");
        queue.setStatus("SENT");
        queue.setSentAt(LocalDateTime.now());
        notificationQueueRepository.save(queue);
    }

    /**
     * [UC-53] Hiện thực gửi thư thông báo email và lưu vết lịch sử thông báo.
     * 
     * Luồng xử lý:
     * 1. Gửi email văn bản thuần túy tới địa chỉ email người dùng qua EmailService.
     * 2. Lưu bản ghi Notification loại SYSTEM ở trạng thái đã đọc (isRead = true).
     * 3. Tạo bản ghi NotificationQueue kênh EMAIL lưu vết phân phối.
     * 
     * @param user Người nhận email
     * @param subject Tiêu đề email
     * @param body Nội dung email
     */
    @Override
    @Transactional
    public void notifyUserByEmail(User user, String subject, String body) {
        emailService.sendPlainText(user.getEmail(), subject, body);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle(subject);
        notification.setContent(body);
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);

        NotificationQueue queue = new NotificationQueue();
        queue.setNotification(saved);
        queue.setChannel("EMAIL");
        queue.setStatus("SENT");
        queue.setSentAt(LocalDateTime.now());
        notificationQueueRepository.save(queue);
    }

    /**
     * [UC-53] Gửi thông báo tự động từ mẫu template động hoặc văn bản dự phòng.
     * 
     * Luồng xử lý:
     * 1. Yêu cầu NotificationTemplateService nội suy mẫu template tương ứng với biến truyền vào.
     * 2. Nếu template không hoạt động, sử dụng tiêu đề và nội dung fallback dự phòng.
     * 3. Gọi hàm notifyUser để phát thông báo tới người dùng.
     * 
     * @param user Người nhận
     * @param type Phân loại thông báo
     * @param templateCode Mã template
     * @param variables Bản đồ biến số
     * @param fallbackTitle Tiêu đề dự phòng
     * @param fallbackContent Nội dung dự phòng
     * @param referenceType Loại thực thể
     * @param referenceId ID thực thể
     */
    @Override
    @Transactional
    public void notifyUserFromTemplate(
            User user,
            NotificationType type,
            String templateCode,
            Map<String, ?> variables,
            String fallbackTitle,
            String fallbackContent,
            String referenceType,
            Long referenceId) {
        NotificationTemplateService.RenderedTemplate rendered = notificationTemplateService
                .renderEnabled(templateCode, variables)
                .orElse(new NotificationTemplateService.RenderedTemplate(fallbackTitle, fallbackContent));
        notifyUser(user, type, rendered.title(), rendered.content(), referenceType, referenceId);
    }

    private String normalizeUserFacingText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value
                .replaceAll("\\bEscrow\\b(?!-)", "Khoản ký quỹ")
                .replaceAll("\\bescrow\\b(?!-)", "khoản ký quỹ")
                .replaceAll("\\bESCROW\\b(?!-)", "khoản ký quỹ")
                .replaceAll("\\bDispute\\b(?!-)", "Tranh chấp")
                .replaceAll("\\bdispute\\b(?!-)", "tranh chấp")
                .replaceAll("\\bDISPUTE\\b(?!-)", "tranh chấp")
                .replaceAll("\\bCancel\\b(?!-)", "Chấm dứt")
                .replaceAll("\\bcancel\\b(?!-)", "chấm dứt")
                .replaceAll("\\bCANCEL\\b(?!-)", "chấm dứt");
    }
}
