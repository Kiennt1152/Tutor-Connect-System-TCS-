package com.tcs.module.marketplace.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonResponse {

    private Long lessonId;
    private Long classId;
    private String classTitle;
    private Integer sequenceNo;

    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long subjectId;

    private String subjectName;

    private String attendanceStatus;
    private LocalDateTime tutorCheckInAt;
    private LocalDateTime tutorCheckOutAt;

    private boolean canCheckInToday;
}
