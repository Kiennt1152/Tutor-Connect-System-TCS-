package com.tcs.module.marketplace.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RescheduleRequestResponse {

    private Long requestId;
    private Long classId;
    private String classTitle;

    private String requestType;

    private String status;

    private Long lessonId;
    private LocalDate oldDate;
    private LocalTime oldStartTime;
    private LocalTime oldEndTime;

    private LocalDate newDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;
    private String subjectName;

    private String reason;
    private String requestedByName;
    private LocalDateTime createdAt;

    private String decidedByName;
    private LocalDateTime decidedAt;
    private String decisionNote;

    private boolean canDecide;

    private boolean canCancel;
}
