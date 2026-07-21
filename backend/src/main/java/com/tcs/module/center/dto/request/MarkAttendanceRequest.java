package com.tcs.module.center.dto.request;

import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkAttendanceRequest {

    private Long classStudentId;
    private LessonAttendanceStatus status;
}
