package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import java.time.LocalDate;

public interface PlatformAnalyticsService {
    AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to);
    byte[] exportCsv(String type, LocalDate from, LocalDate to);
    int generateScheduledDailyReport();
}
