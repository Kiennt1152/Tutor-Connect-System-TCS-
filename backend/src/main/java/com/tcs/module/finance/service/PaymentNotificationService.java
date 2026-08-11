package com.tcs.module.finance.service;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final UserRepository userRepository;
    private final NotificationDispatchService notificationDispatchService;

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

        String templateCode = "PAYMENT_" + (referenceType == null ? "EVENT" : referenceType);
        notificationDispatchService.notifyUserFromTemplate(
                user,
                NotificationType.PAYMENT,
                templateCode,
                Map.of("title", title, "content", content),
                title,
                content,
                referenceType,
                referenceId);
    }
}
