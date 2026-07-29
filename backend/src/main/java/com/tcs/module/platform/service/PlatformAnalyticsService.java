package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;

public interface PlatformAnalyticsService {
    AnalyticsSummaryResponse getSummary();
    byte[] exportCsv(String type);
}
