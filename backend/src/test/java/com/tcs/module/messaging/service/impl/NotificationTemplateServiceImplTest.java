package com.tcs.module.messaging.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.messaging.dto.request.PreviewNotificationTemplateRequest;
import com.tcs.module.messaging.entity.NotificationTemplate;
import com.tcs.module.messaging.repository.NotificationTemplateRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    /** Sheet ntRenderEnabled - UTCID01 (N): có template đang bật -> thay biến đã biết, giữ nguyên biến lạ */
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

    /** Ngoài phạm vi Report 5.1 (MethodList không có preview) - test bổ sung */
    @Test
    void preview_rejectsMalformedPlaceholder() {
        PreviewNotificationTemplateRequest request = new PreviewNotificationTemplateRequest();
        request.setTitleTemplate("Tiêu đề {{bad value}}");
        request.setContentTemplate("Nội dung");
        assertThrows(IllegalArgumentException.class, () -> service.preview(request));
    }

    // ===================================================================
    //  Sheet: ntRenderEnabled
    // ===================================================================
    @Nested
    @DisplayName("ntRenderEnabled")
    class NtRenderEnabled {

        private NotificationTemplate enabledTemplate() {
            NotificationTemplate t = new NotificationTemplate();
            t.setCode("CLASS_CONFIRMED");
            t.setEnabled(true);
            t.setTitleTemplate("Xin chao {{name}}");
            t.setContentTemplate("Lop: {{className}} - Ly do: {{reason}}");
            return t;
        }

        @Test
        @DisplayName("UTCID01 (N) - mau ton tai va dang bat -> tra ve title/content da thay bien")
        void utcid01_renderSuccessfully() {
            when(repository.findByCodeIgnoreCase("CLASS_CONFIRMED")).thenReturn(Optional.of(enabledTemplate()));

            var rendered = service.renderEnabled("CLASS_CONFIRMED",
                    Map.of("name", "An", "className", "Toan 9")).orElseThrow();

            assertEquals("Xin chao An", rendered.title());
            assertEquals("Lop: Toan 9 - Ly do: {{reason}}", rendered.content());
        }

        @Test
        @DisplayName("UTCID02 (A) - code = null -> Optional.empty(), khong truy van repository")
        void utcid02_nullCode() {
            assertTrue(service.renderEnabled(null, Map.of()).isEmpty());
            verify(repository, never()).findByCodeIgnoreCase(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - code rong/toan khoang trang -> Optional.empty()")
        void utcid03_blankCode() {
            assertTrue(service.renderEnabled("", Map.of()).isEmpty());
            assertTrue(service.renderEnabled("   ", Map.of()).isEmpty());
            verify(repository, never()).findByCodeIgnoreCase(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - khong tim thay mau -> Optional.empty()")
        void utcid04_templateNotFound() {
            when(repository.findByCodeIgnoreCase("KHONG_TON_TAI")).thenReturn(Optional.empty());

            assertTrue(service.renderEnabled("KHONG_TON_TAI", Map.of()).isEmpty());
        }

        @Test
        @DisplayName("UTCID05 (A) - mau bi tat (enabled = false) -> Optional.empty()")
        void utcid05_templateDisabled() {
            NotificationTemplate disabled = enabledTemplate();
            disabled.setEnabled(false);
            when(repository.findByCodeIgnoreCase("CLASS_CONFIRMED")).thenReturn(Optional.of(disabled));

            assertTrue(service.renderEnabled("CLASS_CONFIRMED", Map.of("name", "An")).isEmpty());
        }

        @Test
        @DisplayName("UTCID06 (B) - variables = null -> coi nhu map rong, placeholder giu nguyen")
        void utcid06_nullVariables() {
            when(repository.findByCodeIgnoreCase("CLASS_CONFIRMED")).thenReturn(Optional.of(enabledTemplate()));

            var rendered = service.renderEnabled("CLASS_CONFIRMED", null).orElseThrow();

            assertEquals("Xin chao {{name}}", rendered.title());
            assertEquals("Lop: {{className}} - Ly do: {{reason}}", rendered.content());
        }

        @Test
        @DisplayName("UTCID07 (B) - code co khoang trang thua -> duoc trim truoc khi truy van")
        void utcid07_codeIsTrimmed() {
            when(repository.findByCodeIgnoreCase("CLASS_CONFIRMED")).thenReturn(Optional.of(enabledTemplate()));

            var rendered = service.renderEnabled("   CLASS_CONFIRMED   ", Map.of("name", "An")).orElseThrow();

            assertEquals("Xin chao An", rendered.title());
            verify(repository).findByCodeIgnoreCase("CLASS_CONFIRMED");
        }
    }
}
