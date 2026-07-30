package com.tcs.module.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

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
    private BigDecimal rating;
    private String comment;
    private String criteriaJson;
    private boolean anonymous;
    private String reviewerDisplayName;
    private LocalDateTime reviewedAt;
    private String tutorReply;
    private LocalDateTime tutorReplyAt;

    private boolean reviewable;
    private int reviewsSubmitted;
    private boolean reviewOverdue;
}
