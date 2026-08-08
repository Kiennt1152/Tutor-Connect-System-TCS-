package com.tcs.module.messaging.service.impl;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.entity.NotificationQueue;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationQueueRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.EmailService;
import com.tcs.module.messaging.service.NotificationDispatchService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final NotificationQueueRepository notificationQueueRepository;
    private final EmailService emailService;

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
        notification.setTitle(title);
        notification.setContent(content);
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
}
