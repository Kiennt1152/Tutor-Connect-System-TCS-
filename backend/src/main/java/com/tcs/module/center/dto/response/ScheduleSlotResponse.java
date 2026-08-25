package com.tcs.module.center.dto.response;

import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSlotResponse {

    private Long slotId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    public ScheduleSlotResponse() {}

    public ScheduleSlotResponse(Long slotId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.slotId = slotId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public static ScheduleSlotResponseBuilder builder() {
        return new ScheduleSlotResponseBuilder();
    }

    public static class ScheduleSlotResponseBuilder {
        private Long slotId;
        private Integer dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;

        public ScheduleSlotResponseBuilder slotId(Long slotId) { this.slotId = slotId; return this; }
        public ScheduleSlotResponseBuilder dayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; return this; }
        public ScheduleSlotResponseBuilder startTime(LocalTime startTime) { this.startTime = startTime; return this; }
        public ScheduleSlotResponseBuilder endTime(LocalTime endTime) { this.endTime = endTime; return this; }
        public ScheduleSlotResponse build() {
            return new ScheduleSlotResponse(slotId, dayOfWeek, startTime, endTime);
        }
    }
}
