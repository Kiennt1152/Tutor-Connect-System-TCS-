package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public long getTotalUsers() { return totalUsers; }
    public long getTotalClasses() { return totalClasses; }
    public RiskSummaryResponse getRiskSummary() { return riskSummary; }
    public FinancialFlowResponse getFinancialFlow() { return financialFlow; }
    public HealthMetricsResponse getTutorHealth() { return tutorHealth; }
    public HealthMetricsResponse getCenterHealth() { return centerHealth; }
    public HealthMetricsResponse getClassHealth() { return classHealth; }
    public List<ActivityTimelineEntry> getActivityTimeline() { return activityTimeline; }
    public List<TaskItemResponse> getQueuePreview() { return queuePreview; }
    public List<DashboardAlertResponse> getAlerts() { return alerts; }
}
