package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {

    private long totalUsers;
    private long totalTutors;
    private long totalClasses;
    private long pendingVerifications;
    private long openReports;
}
