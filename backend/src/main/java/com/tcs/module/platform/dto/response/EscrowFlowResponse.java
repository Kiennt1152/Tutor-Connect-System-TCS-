package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EscrowFlowResponse {
    BigDecimal deposited;
    BigDecimal released;
    BigDecimal refunded;
    BigDecimal held;
    BigDecimal platformFee;
}
