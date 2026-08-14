package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TaskItemResponse {
    String taskId;
    String taskType; // VERIFICATION, REPORT, SUPPORT_TICKET, WITHDRAWAL, DISPUTE
    String title;
    String description;
    Long entityId;
    String targetRoute;
    String targetQuery;
    String status;
    String priority; // URGENT, HIGH, MEDIUM, LOW
    LocalDateTime createdAt;
    LocalDateTime dueAt;
    Boolean slaBreached;
    String assigneeName;
    String riskReason;
    java.math.BigDecimal amount;
    String currency;
    String relatedEntityType;
    Long relatedEntityId;
}
