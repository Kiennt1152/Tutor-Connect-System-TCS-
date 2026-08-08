package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefundExecutionResponse {

    private Long refundId;
    private Long escrowId;
    private EscrowStatus escrowStatus;
    private RefundRequestStatus refundStatus;
    private BigDecimal escrowAmount;
    private BigDecimal releaseToBeneficiary;
    private BigDecimal refundToPayer;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String message;
}
