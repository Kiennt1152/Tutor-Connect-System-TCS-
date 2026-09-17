package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorFinancialAnalyticsResponse {
    private Long tutorId;
    private String fullName;
    private String email;
    private String phone;
    private String verificationStatus;
    private long totalClasses;
    private long activeClasses;
    private long completedClasses;
    private long newClassesInPeriod;
    private BigDecimal totalEarnings;
    private BigDecimal totalWithdrawn;
    private BigDecimal pendingWithdrawals;
    private BigDecimal escrowHolding;
    private BigDecimal availableBalance;
    private Double averageRating;
}
