package com.tcs.module.marketplace.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RescheduleLessonRequest {

    private LocalDate newDate;

    private LocalTime newStartTime;

    private LocalTime newEndTime;

    private String reason;
}
