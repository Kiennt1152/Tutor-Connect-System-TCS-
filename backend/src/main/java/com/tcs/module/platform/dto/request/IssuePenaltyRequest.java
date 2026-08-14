package com.tcs.module.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IssuePenaltyRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String penaltyType;

    @NotBlank
    private String reason;

    private String evidenceUrls;

    private String restrictionDetails;

    private LocalDateTime expiresAt;

    private String sourceType;

    private Long sourceId;

    private String sourceTaskId;
}
