package com.tcs.module.messaging.service;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.enums.NotificationType;
import java.util.Map;

public interface NotificationDispatchService {

    void notifyUser(User user, NotificationType type, String title, String content, String referenceType, Long referenceId);

    void notifyUserByEmail(User user, String subject, String body);

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
