package com.tcs.module.profile.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorBusyTimeResponse {

    private Long busyTimeId;
    private LocalDate busyDate;
    /** {@code null} cùng {@link #endTime} = bận cả ngày. */
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean allDay;
    private String note;
}
