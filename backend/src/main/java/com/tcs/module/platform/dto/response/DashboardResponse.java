package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {

    private long totalUsers;
    private long totalClasses;
    
    // 5 Zones of Command Center
    private RiskSummaryResponse riskSummary;
    private FinancialFlowResponse financialFlow;
    private HealthMetricsResponse tutorHealth;
    private HealthMetricsResponse centerHealth;
    private HealthMetricsResponse classHealth;
    private List<ActivityTimelineEntry> activityTimeline;
    private List<TaskItemResponse> queuePreview;
    
    private List<DashboardAlertResponse> alerts;
}
