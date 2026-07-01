package com.tcs.module.marketplace.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorSearchResponse {

    private Long tutorId;
    private Long userId;
    private String fullName;
    private String bio;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal ratingAvg;
    private String verificationStatus;
}
