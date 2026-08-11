package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.PreviewNotificationTemplateRequest;
import com.tcs.module.messaging.dto.request.UpsertNotificationTemplateRequest;
import com.tcs.module.messaging.dto.response.NotificationTemplatePreviewResponse;
import com.tcs.module.messaging.dto.response.NotificationTemplateResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NotificationTemplateService {
    record RenderedTemplate(String title, String content) {}

    List<NotificationTemplateResponse> findAll();
    NotificationTemplateResponse findById(Long templateId);
    NotificationTemplateResponse create(UpsertNotificationTemplateRequest request);
    NotificationTemplateResponse update(Long templateId, UpsertNotificationTemplateRequest request);
    NotificationTemplateResponse disable(Long templateId);
    NotificationTemplatePreviewResponse preview(PreviewNotificationTemplateRequest request);
    Optional<RenderedTemplate> renderEnabled(String code, Map<String, ?> variables);
}
