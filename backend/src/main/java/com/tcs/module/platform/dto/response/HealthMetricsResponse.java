package com.tcs.module.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricsResponse {
    private long totalCount;
    private long activeCount;
    private long verifiedCount;
    private long newCount;
    private long recentlyActiveCount;

    public long getTotal() {
        return totalCount;
    }

    public long getActive() {
        return activeCount;
    }

    public long getVerified() {
        return verifiedCount;
    }

    public long getNewTutors() {
        return newCount;
    }

    public long getNewCenters() {
        return newCount;
    }
}
