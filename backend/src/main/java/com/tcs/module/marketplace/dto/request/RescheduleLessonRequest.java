package com.tcs.module.marketplace.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** Xin dời một buổi đã có sang ngày/giờ khác (UC-36). */
@Getter
@Setter
public class RescheduleLessonRequest {

    private LocalDate newDate;

    private LocalTime newStartTime;

    private LocalTime newEndTime;

    /** Lý do dời buổi — bên duyệt cần biết để quyết định. */
    private String reason;
}
