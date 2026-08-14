package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ActivityTimelineEntry {
    String label;
    long newUsers;
    long newTutors;
    long newCenters;
    long newClasses;
    long newTickets;
    long activeTutors;
    long activeCenters;
    BigDecimal moneyIn;
    BigDecimal moneyOut;
    BigDecimal netMovement;
    BigDecimal platformFeeRevenue;
}
