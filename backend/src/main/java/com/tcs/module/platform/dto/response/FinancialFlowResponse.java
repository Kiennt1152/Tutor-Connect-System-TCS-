package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class FinancialFlowResponse {
    private BigDecimal moneyIn;
    private BigDecimal moneyOut;
    private BigDecimal netMovement;
    private BigDecimal escrowHeld;
    private BigDecimal deposits;
    private BigDecimal escrowDeposits;
    private BigDecimal withdrawals;
    private BigDecimal refunds;
    private BigDecimal platformFeeRevenue;
    private int openEscrowCount;
    private int settledCount;
    private double feeRate;
}
