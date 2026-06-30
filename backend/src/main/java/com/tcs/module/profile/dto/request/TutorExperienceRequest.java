package com.tcs.module.profile.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorExperienceRequest {

    private String role;
    private String organization;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
