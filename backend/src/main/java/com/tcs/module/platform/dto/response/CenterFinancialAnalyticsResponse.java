package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CenterFinancialAnalyticsResponse {
    private Long centerId;
    private String companyName;
    private String licenseNo;
    private String phone;
    private String email;
    private String address;
    private long totalClasses;
    private long activeClasses;
    private long completedClasses;
    private long totalTutors;
    private long newTutorsInPeriod;
    private BigDecimal moneyIn;
    private BigDecimal moneyOut;
    private BigDecimal escrowHeld;
    private BigDecimal escrowReleased;
    private BigDecimal platformFeePaid;
    private BigDecimal walletBalance;
}
