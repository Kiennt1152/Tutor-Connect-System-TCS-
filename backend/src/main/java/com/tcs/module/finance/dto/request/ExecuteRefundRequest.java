package com.tcs.module.finance.dto.request;

import com.tcs.module.finance.dto.RefundPayoutInfo;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecuteRefundRequest {

    private Long escrowId;
    private BigDecimal releaseToBeneficiary;
    private BigDecimal refundToPayer;
    private String reason;
    private RefundPayoutInfo refundPayoutInfo;
}
