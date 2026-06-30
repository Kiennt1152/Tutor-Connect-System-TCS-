package com.tcs.module.profile.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorAvailabilityRequest {

    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean recurring;
    private LocalDate specificDate;
}
