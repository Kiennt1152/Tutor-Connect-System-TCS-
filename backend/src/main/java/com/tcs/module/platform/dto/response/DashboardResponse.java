package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {

    private long totalUsers;
    private long totalTutors;
    private long totalClasses;
    private long activeClasses;
    private long pendingVerifications;
    private long openReports;
    private long openTickets;
    private long pendingWithdrawals;
    private long openDisputes;
    private BigDecimal totalRevenue;
    private BigDecimal platformFeeRevenue;
    private List<DashboardAlertResponse> alerts;
}
