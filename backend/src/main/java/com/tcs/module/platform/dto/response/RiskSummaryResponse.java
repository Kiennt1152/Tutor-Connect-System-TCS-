package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class RiskSummaryResponse {
    private long overdueTickets;
    private long openDisputes;
    private long pendingRefunds;
    private BigDecimal escrowExposure;
    private long unhandledReports;
}
