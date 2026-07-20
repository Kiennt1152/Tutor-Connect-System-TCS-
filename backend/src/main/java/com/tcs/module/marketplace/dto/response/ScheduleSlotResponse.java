package com.tcs.module.marketplace.dto.response;

import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleSlotResponse {

    private Long slotId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
