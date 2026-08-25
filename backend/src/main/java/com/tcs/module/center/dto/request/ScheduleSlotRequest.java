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

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
