package com.tcs.module.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public BigDecimal getMoneyIn() { return moneyIn; }
    public BigDecimal getMoneyOut() { return moneyOut; }
    public BigDecimal getNetMovement() { return netMovement; }
    public BigDecimal getEscrowHeld() { return escrowHeld; }
    public BigDecimal getDeposits() { return deposits; }
    public BigDecimal getEscrowDeposits() { return escrowDeposits; }
    public BigDecimal getWithdrawals() { return withdrawals; }
    public BigDecimal getRefunds() { return refunds; }
    public BigDecimal getPlatformFeeRevenue() { return platformFeeRevenue; }
    public int getOpenEscrowCount() { return openEscrowCount; }
    public int getSettledCount() { return settledCount; }
    public double getFeeRate() { return feeRate; }
}
