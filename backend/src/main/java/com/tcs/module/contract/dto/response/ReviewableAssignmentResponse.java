package com.tcs.module.contract.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * Mot lop da hoan thanh cua khach hang, kem thong tin gia su va trang thai da danh gia hay chua.
 * Dung cho man hinh "Danh gia cua toi" (UC-65).
 */
@Getter
@Builder
public class ReviewableAssignmentResponse {

    private Long assignmentId;
    private Long classId;
    private String classTitle;
    private String subjectName;
    private String classStatus;
    private Long tutorUserId;
    private String tutorName;

    private boolean reviewed;
    private Long reviewId;
    private Integer rating;
    private String comment;
    private LocalDateTime reviewedAt;
}
