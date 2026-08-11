package com.tcs.module.messaging.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.tcs.module.messaging.dto.request.PreviewNotificationTemplateRequest;
import com.tcs.module.messaging.entity.NotificationTemplate;
import com.tcs.module.messaging.repository.NotificationTemplateRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceImplTest {
    @Mock NotificationTemplateRepository repository;
    @Mock AuditLogService auditLogService;
    @InjectMocks NotificationTemplateServiceImpl service;

    @Test
    void renderEnabled_replacesKnownVariablesAndKeepsUnknownOnes() {
        NotificationTemplate template = new NotificationTemplate();
        template.setCode("PENALTY_ISSUED");
        template.setEnabled(true);
        template.setTitleTemplate("Xin chào {{name}}");
        template.setContentTemplate("Lý do: {{reason}}");
        when(repository.findByCodeIgnoreCase("PENALTY_ISSUED")).thenReturn(Optional.of(template));

        var rendered = service.renderEnabled("PENALTY_ISSUED", Map.of("name", "An")).orElseThrow();

        assertEquals("Xin chào An", rendered.title());
        assertEquals("Lý do: {{reason}}", rendered.content());
    }

    @Test
    void preview_rejectsMalformedPlaceholder() {
        PreviewNotificationTemplateRequest request = new PreviewNotificationTemplateRequest();
        request.setTitleTemplate("Tiêu đề {{bad value}}");
        request.setContentTemplate("Nội dung");
        assertThrows(IllegalArgumentException.class, () -> service.preview(request));
    }
}
