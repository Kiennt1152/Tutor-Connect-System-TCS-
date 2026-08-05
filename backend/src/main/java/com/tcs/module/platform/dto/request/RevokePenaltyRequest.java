package com.tcs.module.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RevokePenaltyRequest {
    @NotBlank
    private String revokedReason;
}
