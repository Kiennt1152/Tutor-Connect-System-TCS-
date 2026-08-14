package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionTypeBreakdown {
    String type;
    String label;
    long count;
    BigDecimal totalAmount;
    String direction;  // "IN" or "OUT"
}
