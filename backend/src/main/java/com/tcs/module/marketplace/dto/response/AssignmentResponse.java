package com.tcs.module.marketplace.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssignmentResponse {

    private Long assignmentId;
    private Long classId;
    private String classTitle;
    private String clientName;
    private String tutorName;
    private String status;
    private LocalDateTime assignedDate;

    private List<String> subjectNames;
    private String gradeName;
    private String address;
    private String lessonMode;
    private LocalDate startDate;
    private LocalDate endDate;

    private long lessonCount;

    private LocalDateTime tutorSignedAt;
    private LocalDateTime clientSignedAt;
    private String paymentMethod;

    /** UC "Xác nhận lớp đã hoàn thành" (lớp PRIVATE). */
    private boolean classCompleted;
    private String completionState;
    private String completionBlockedReason;
}
