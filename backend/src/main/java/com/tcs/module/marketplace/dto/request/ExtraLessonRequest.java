package com.tcs.module.marketplace.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/** Xin thêm một buổi ngoài lịch đã sinh — học bù hoặc học thêm (UC-36). */
@Getter
@Setter
public class ExtraLessonRequest {

    private Long classId;

    private LocalDate lessonDate;

    private LocalTime startTime;

    private LocalTime endTime;

    /** Môn của buổi thêm. Bỏ trống nếu lớp không phân môn. */
    private Long subjectId;

    private String reason;
}
