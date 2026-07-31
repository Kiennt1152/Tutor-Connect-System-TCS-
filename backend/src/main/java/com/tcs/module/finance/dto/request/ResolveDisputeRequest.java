package com.tcs.module.finance.dto.request;

import com.tcs.module.finance.enums.DisputeResolutionAction;
import com.tcs.module.finance.enums.DisputeStatus;
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
}
