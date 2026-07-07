package com.tcs.module.platform.dto.request;

import com.tcs.module.identity.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewVerificationRequest {

    @NotNull(message = "Trạng thái xác minh không được để trống")
    private VerificationStatus status;

    private String adminNotes;
}
