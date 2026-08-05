package com.tcs.module.profile.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorCertificateResponse {

    private Long certificateId;
    private String name;
    private String issuer;
    private LocalDate issueDate;
}
