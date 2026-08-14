package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HealthMetricsResponse {
    private long totalCount;
    private long activeCount;
    private long verifiedCount;
    private long newCount;
    private long recentlyActiveCount;
}
