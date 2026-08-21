package com.tcs.module.profile.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorEducationRequest {

    private String institution;
    private String degree;
    private String fieldOfStudy;
    private Integer startYear;
    private Integer endYear;
}
