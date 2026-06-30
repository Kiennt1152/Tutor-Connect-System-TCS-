package com.tcs.module.platform.dto.request;

import com.tcs.module.identity.enums.VerificationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewVerificationRequest {

    private VerificationStatus status;
    private String adminNotes;
}
