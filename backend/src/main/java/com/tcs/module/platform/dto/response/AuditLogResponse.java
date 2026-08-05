package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AuditLogResponse {
    Long auditId;
    Long actorId;
    String actorEmail;
    String actorRole;
    String action;
    String entityType;
    Long entityId;
    String oldValue;
    String newValue;
    String ipAddress;
    LocalDateTime createdAt;
}
