package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponse {

    private Long transactionId;
    private PaymentTransactionType type;
    private PaymentTransactionStatus status;
    private BigDecimal amount;
    private String description;
    private String referenceCode;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
