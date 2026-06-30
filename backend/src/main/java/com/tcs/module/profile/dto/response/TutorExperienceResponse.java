package com.tcs.module.profile.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorExperienceResponse {

    private Long experienceId;
    private String role;
    private String organization;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
