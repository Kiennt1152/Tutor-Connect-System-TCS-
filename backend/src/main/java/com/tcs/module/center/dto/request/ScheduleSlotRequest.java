package com.tcs.module.center.dto.request;

import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** Một khung lịch (thứ trong tuần + giờ bắt đầu/kết thúc) của lớp học. */
@Getter
@Setter
public class ScheduleSlotRequest {

    /** 1 = Thứ Hai ... 7 = Chủ Nhật. */
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
