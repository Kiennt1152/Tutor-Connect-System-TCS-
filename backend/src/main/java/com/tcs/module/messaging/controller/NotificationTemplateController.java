package com.tcs.module.messaging.controller;

import com.tcs.module.messaging.dto.request.PreviewNotificationTemplateRequest;
import com.tcs.module.messaging.dto.request.UpsertNotificationTemplateRequest;
import com.tcs.module.messaging.dto.response.NotificationTemplatePreviewResponse;
import com.tcs.module.messaging.dto.response.NotificationTemplateResponse;
import com.tcs.module.messaging.service.NotificationTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {
    private final NotificationTemplateService service;

    @GetMapping
    public List<NotificationTemplateResponse> findAll() { return service.findAll(); }

    @GetMapping("/{templateId}")
    public NotificationTemplateResponse findById(@PathVariable Long templateId) { return service.findById(templateId); }

    @PostMapping
    public NotificationTemplateResponse create(@Valid @RequestBody UpsertNotificationTemplateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{templateId}")
    public NotificationTemplateResponse update(@PathVariable Long templateId,
            @Valid @RequestBody UpsertNotificationTemplateRequest request) {
        return service.update(templateId, request);
    }

    @DeleteMapping("/{templateId}")
    public NotificationTemplateResponse disable(@PathVariable Long templateId) { return service.disable(templateId); }

    @PostMapping("/preview")
    public NotificationTemplatePreviewResponse preview(@Valid @RequestBody PreviewNotificationTemplateRequest request) {
        return service.preview(request);
    }
}
