package com.tcs.module.profile.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorEducationResponse {

    private Long educationId;
    private String institution;
    private String degree;
    private String fieldOfStudy;
    private Integer startYear;
    private Integer endYear;
}
