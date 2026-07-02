package com.tcs.module.platform.dto.response;

import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerificationRequestResponse {

    private Long verificationId;
    private Long userId;
    private String userEmail;
    private String submitterName;
    private VerificationType verificationType;
    private VerificationStatus status;
    private String adminNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
