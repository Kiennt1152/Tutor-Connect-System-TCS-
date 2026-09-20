package com.tcs.module.marketplace.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/** Một buổi của lớp rơi vào khoảng thời gian gia sư đã tự đăng ký là bận. */
@Getter
@Builder
public class BusyConflictResponse {

    private LocalDate date;
    /** Giờ của buổi học trong lớp. */
    private LocalTime startTime;
    private LocalTime endTime;
    /** Khoảng bận đã đăng ký; cùng {@code null} khi bận cả ngày. */
    private LocalTime busyStartTime;
    private LocalTime busyEndTime;
    private boolean allDay;
    private String note;
}
