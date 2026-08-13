package com.tcs.module.messaging.dto.response;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationTemplatePreviewResponse {
    private String title;
    private String content;
    private Set<String> unresolvedPlaceholders;
}
