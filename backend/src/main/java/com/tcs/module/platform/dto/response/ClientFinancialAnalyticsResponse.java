package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientFinancialAnalyticsResponse {
    private Long clientId;
    private String fullName;
    private String email;
    private String phone;
    private long totalClassesRegistered;
    private long activeClasses;
    private long completedClasses;
    private BigDecimal totalDeposited;
    private BigDecimal totalRefunded;
    private BigDecimal activeEscrow;
    private BigDecimal availableBalance;
}
