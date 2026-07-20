package com.tcs.module.marketplace.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/** Một buổi dạy cụ thể trong lịch của gia sư. */
@Getter
@Builder
public class LessonResponse {

    private Long lessonId;
    private Long classId;
    private String classTitle;
    private Integer sequenceNo;

    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
    /** Cần cho form "thêm buổi" — FE dựng danh sách môn của lớp từ chính các buổi đã có. */
    private Long subjectId;

    private String subjectName;

    /** PENDING | COMPLETED | ABSENT | DISPUTED */
    private String attendanceStatus;
    private LocalDateTime tutorCheckInAt;
    private LocalDateTime tutorCheckOutAt;

    /** true khi buổi diễn ra đúng hôm nay — chỉ khi đó gia sư mới điểm danh được. */
    private boolean canCheckInToday;
}
