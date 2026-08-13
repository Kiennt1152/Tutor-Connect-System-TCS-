package com.tcs.module.messaging.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationTemplateResponse {
    private Long templateId;
    private String code;
    private String titleTemplate;
    private String contentTemplate;
    private String channel;
    private String description;
    private boolean enabled;
    private Set<String> placeholders;
    private LocalDateTime updatedAt;
}
