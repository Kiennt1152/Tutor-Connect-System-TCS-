package com.tcs.module.marketplace.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExtraLessonRequest {

    private Long classId;

    private LocalDate lessonDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Long subjectId;

    private String reason;
}
