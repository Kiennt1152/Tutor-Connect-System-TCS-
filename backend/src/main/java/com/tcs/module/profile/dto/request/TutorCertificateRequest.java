package com.tcs.module.profile.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorCertificateRequest {

    private String name;
    private String issuer;
    private LocalDate issueDate;
}
