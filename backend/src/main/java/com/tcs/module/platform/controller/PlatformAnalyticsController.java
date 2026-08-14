package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.service.PlatformAnalyticsService;
import com.tcs.module.platform.service.AuditLogService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/analytics")
@RequiredArgsConstructor
public class PlatformAnalyticsController {

    private final PlatformAnalyticsService analyticsService;
    private final AuditLogService auditLogService;

    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getSummary(from, to);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "users") String type,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ định dạng xuất CSV: " + format);
        }
        validateRange(from, to);
        byte[] csvData = analyticsService.exportCsv(type, from, to);
        auditLogService.record("EXPORT_ANALYTICS", "AnalyticsExport", 0L, null,
                java.util.Map.of("type", type, "from", String.valueOf(from), "to", String.valueOf(to)));
        String filename = "tcs-analytics-" + type + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
    }
}
