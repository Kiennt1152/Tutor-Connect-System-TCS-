package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRecruitmentPostRequest {

    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private Integer requiredExperience;
    private Long subjectId;
    private Long locationId;
    private Integer maxPositions;
}
