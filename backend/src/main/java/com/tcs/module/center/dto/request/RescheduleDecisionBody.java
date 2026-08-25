package com.tcs.module.center.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Trung tâm duyệt/từ chối một yêu cầu dời buổi học. */
@Getter
@Setter
public class RescheduleDecisionBody {

    private Long classId;
    private LocalDate originalDate;
    private boolean approve;
}
