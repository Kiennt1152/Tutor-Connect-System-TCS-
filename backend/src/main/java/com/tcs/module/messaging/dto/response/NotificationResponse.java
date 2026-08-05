package com.tcs.module.messaging.dto.response;

import com.tcs.module.messaging.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {

    private Long notificationId;
    private NotificationType type;
    private String title;
    private String content;
    private String referenceType;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String referenceType;
    private Long referenceId;
}
