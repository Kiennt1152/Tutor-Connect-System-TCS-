package com.tcs.module.center.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentAttendanceResponse {

    private Long classStudentId;
    private String studentName;
    private String studentPhone;
    /** PRESENT / ABSENT / EXCUSED; null nếu chưa điểm danh buổi này. */
    private String status;
}
