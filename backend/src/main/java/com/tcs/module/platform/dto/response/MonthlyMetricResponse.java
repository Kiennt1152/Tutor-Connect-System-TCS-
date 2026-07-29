package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;

@Value
@Builder
public class MonthlyMetricResponse {
    String month;
    long newUsers;
    long newClasses;
    BigDecimal revenue;
}
