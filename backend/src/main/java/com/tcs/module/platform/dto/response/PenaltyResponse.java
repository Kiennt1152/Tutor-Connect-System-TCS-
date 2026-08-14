package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PenaltyResponse {
    Long penaltyId;
    Long userId;
    String userEmail;
    String userName;
    String penaltyType;
    String reason;
    String evidenceUrls;
    String restrictionDetails;
    LocalDateTime startsAt;
    LocalDateTime expiresAt;
    String status;
    LocalDateTime revokedAt;
    String revokedReason;
    LocalDateTime createdAt;
    String issuedByName;
    String sourceType;
    Long sourceId;
    String sourceTaskId;
}
