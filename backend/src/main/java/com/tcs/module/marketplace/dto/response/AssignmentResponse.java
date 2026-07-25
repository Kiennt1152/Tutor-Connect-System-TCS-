package com.tcs.module.marketplace.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Một lớp mà gia sư được Client chọn — lời mời (PENDING) hoặc lớp đang dạy (ACTIVE). */
@Getter
@Builder
public class AssignmentResponse {

    private Long assignmentId;
    private Long classId;
    private String classTitle;
    private String clientName;
    /** Gia sư được chọn — Client cần biết ai đang dạy lớp của mình. */
    private String tutorName;
    /** PENDING | ACTIVE | DECLINED | TERMINATED */
    private String status;
    private LocalDateTime assignedDate;

    private List<String> subjectNames;
    private String gradeName;
    private String address;
    private String lessonMode;
    private LocalDate startDate;
    private LocalDate endDate;

    /** Số buổi đã sinh (0 khi chưa nhận lớp). */
    private long lessonCount;
}
