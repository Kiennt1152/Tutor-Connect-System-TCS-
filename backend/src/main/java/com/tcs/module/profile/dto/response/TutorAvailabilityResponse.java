package com.tcs.module.profile.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorAvailabilityResponse {

    private Long availabilityId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean recurring;
    private LocalDate specificDate;
    private String googleCalendarEventId;
}
