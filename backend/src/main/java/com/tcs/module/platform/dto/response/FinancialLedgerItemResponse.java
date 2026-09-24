package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinancialLedgerItemResponse {
    private Long transactionId;
    private String referenceCode;
    private String type;
    private String typeLabel;
    private String direction; // IN, OUT
    private BigDecimal amount;
    private String status;
    private String description;
    private Long actorUserId;
    private String actorName;
    private String actorEmail;
    private String actorRole;
    private String relatedEntity;
    private LocalDateTime createdAt;
}
