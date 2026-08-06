package com.tcs.module.finance.service;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public void notifyPayment(Long userId, String title, String content, String referenceType, Long referenceId) {
        if (userId == null) {
            return;
        }
        userRepository.findById(userId)
                .ifPresent(user -> notifyPayment(user, title, content, referenceType, referenceId));
    }

    public void notifyPayment(User user, String title, String content, String referenceType, Long referenceId) {
        if (user == null || content == null || content.isBlank()) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.PAYMENT);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }
}
