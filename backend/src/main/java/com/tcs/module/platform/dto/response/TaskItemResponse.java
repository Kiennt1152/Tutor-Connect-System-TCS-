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
    String status;
    String priority; // URGENT, HIGH, MEDIUM, LOW
    LocalDateTime createdAt;
}
