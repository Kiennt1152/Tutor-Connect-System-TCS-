package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaskQueueSummaryResponse {
    long pendingVerifications;
    long openReports;
    long openTickets;
    long pendingWithdrawals;
    long pendingRefunds;
    long openDisputes;
    long totalPendingTasks;
}
