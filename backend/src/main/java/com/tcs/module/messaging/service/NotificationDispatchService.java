package com.tcs.module.messaging.service;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.enums.NotificationType;

public interface NotificationDispatchService {

    void notifyUser(User user, NotificationType type, String title, String content, String referenceType, Long referenceId);

    void notifyUserByEmail(User user, String subject, String body);
}
