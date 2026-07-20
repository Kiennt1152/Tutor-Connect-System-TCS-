package com.tcs.module.center.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/** Một yêu cầu dời buổi học để hiển thị cho gia sư và trung tâm. */
@Getter
@Builder
public class RescheduleResponse {

    private Long classId;
    private String className;
    private LocalDate originalDate;
    private LocalDate newDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime newStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime newEndTime;

    private String status; // PENDING | APPROVED | REJECTED
    private Long tutorId;
    private String tutorName;
    private String reason;
}
