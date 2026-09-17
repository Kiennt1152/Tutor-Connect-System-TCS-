package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.dto.response.CenterFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.ClientFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.FinancialLedgerItemResponse;
import com.tcs.module.platform.dto.response.TutorFinancialAnalyticsResponse;
import com.tcs.module.platform.service.PlatformAnalyticsService;
import com.tcs.module.platform.service.AuditLogService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/entities/centers")
    public List<CenterFinancialAnalyticsResponse> getCenterAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getCenterAnalytics(from, to);
    }

    @GetMapping("/entities/tutors")
    public List<TutorFinancialAnalyticsResponse> getTutorAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getTutorAnalytics(from, to);
    }

    @GetMapping("/entities/clients")
    public List<ClientFinancialAnalyticsResponse> getClientAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        validateRange(from, to);
        return analyticsService.getClientAnalytics(from, to);
    }

    @GetMapping("/ledger")
    public Page<FinancialLedgerItemResponse> getFinancialLedger(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validateRange(from, to);
        return analyticsService.getFinancialLedger(role, direction, search, from, to, PageRequest.of(page, size));
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

    @PostMapping("/scheduled-reports/trigger")
    public java.util.Map<String, Object> triggerScheduledReport() {
        int count = analyticsService.generateScheduledDailyReport();
        return java.util.Map.of("message", "Tạo báo cáo định kỳ tự động thành công", "count", count, "status", "SUCCESS");
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
    }
}
