package com.tcs.module.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PlatformAnalyticsService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PlatformAnalyticsControllerTest {

    @Mock
    private PlatformAnalyticsService analyticsService;

    @Mock
    private AuditLogService auditLogService;

    /** Ngoài phạm vi Report 5.1 (test controller - PlatformAnalyticsController, không có trong MethodList) */
    @Test
    void exportCsvReturnsAttachmentAndRecordsValidAuditEntityId() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 12);
        byte[] csv = "ID,Email\n1,user@example.com\n".getBytes(StandardCharsets.UTF_8);
        when(analyticsService.exportCsv("users", from, to)).thenReturn(csv);
        PlatformAnalyticsController controller = new PlatformAnalyticsController(analyticsService, auditLogService);

        ResponseEntity<byte[]> response = controller.exportCsv("users", "csv", from, to);

        assertThat(response.getBody()).isEqualTo(csv);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename=\"tcs-analytics-users-");
        verify(auditLogService).record(
                eq("EXPORT_ANALYTICS"), eq("AnalyticsExport"), eq(0L), isNull(),
                eq(java.util.Map.of("type", "users", "from", from.toString(), "to", to.toString())));
    }
}
