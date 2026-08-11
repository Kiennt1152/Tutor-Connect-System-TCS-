package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class AnalyticsSummaryResponse {
    long totalUsers;
    long totalTutors;
    long totalParents;
    long totalCenters;
    long totalStudents;
    long totalClasses;
    long activeClasses;
    long completedClasses;
    BigDecimal totalRevenue;
    BigDecimal platformFeeRevenue;
    BigDecimal deposits;
    BigDecimal withdrawals;
    BigDecimal escrowHeld;
    BigDecimal escrowReleased;
    BigDecimal escrowRefunded;
    double verificationConversionRate;
    double disputeRate;
    double contractCompletionRate;
    List<MonthlyMetricResponse> monthlyMetrics;
}
