package com.tcs.module.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskSummaryResponse {
    private long overdueTickets;
    private long openDisputes;
    private long pendingRefunds;
    private BigDecimal escrowExposure;
    private long unhandledReports;

    public long getOverdueTickets() { return overdueTickets; }
    public long getOpenDisputes() { return openDisputes; }
    public long getPendingRefunds() { return pendingRefunds; }
    public BigDecimal getEscrowExposure() { return escrowExposure; }
    public long getUnhandledReports() { return unhandledReports; }
}
