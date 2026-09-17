package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.AnalyticsSummaryResponse;
import com.tcs.module.platform.dto.response.CenterFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.ClientFinancialAnalyticsResponse;
import com.tcs.module.platform.dto.response.FinancialLedgerItemResponse;
import com.tcs.module.platform.dto.response.TutorFinancialAnalyticsResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlatformAnalyticsService {
    AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to);
    byte[] exportCsv(String type, LocalDate from, LocalDate to);
    int generateScheduledDailyReport();

    List<CenterFinancialAnalyticsResponse> getCenterAnalytics(LocalDate from, LocalDate to);
    CenterFinancialAnalyticsResponse getCenterAnalyticsByCenterId(Long centerId, LocalDate from, LocalDate to);
    List<TutorFinancialAnalyticsResponse> getTutorAnalytics(LocalDate from, LocalDate to);
    List<ClientFinancialAnalyticsResponse> getClientAnalytics(LocalDate from, LocalDate to);
    Page<FinancialLedgerItemResponse> getFinancialLedger(
            String role, String direction, String search, LocalDate from, LocalDate to, Pageable pageable);
}
