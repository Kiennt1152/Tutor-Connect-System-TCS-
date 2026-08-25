package com.tcs.module.finance.dto.request;

import com.tcs.module.finance.enums.DisputeResolutionAction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveDisputeRequest {

    private DisputeResolutionAction action;
    private DisputeStatus status;
    private String resolution;
    private BigDecimal releaseToBeneficiary;
    private BigDecimal refundToPayer;
    private RefundPayoutInfo refundPayoutInfo;

    public DisputeResolutionAction getAction() { return action; }
    public void setAction(DisputeResolutionAction action) { this.action = action; }
    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public BigDecimal getReleaseToBeneficiary() { return releaseToBeneficiary; }
    public void setReleaseToBeneficiary(BigDecimal releaseToBeneficiary) { this.releaseToBeneficiary = releaseToBeneficiary; }
    public BigDecimal getRefundToPayer() { return refundToPayer; }
    public void setRefundToPayer(BigDecimal refundToPayer) { this.refundToPayer = refundToPayer; }
    public RefundPayoutInfo getRefundPayoutInfo() { return refundPayoutInfo; }
    public void setRefundPayoutInfo(RefundPayoutInfo refundPayoutInfo) { this.refundPayoutInfo = refundPayoutInfo; }
}
