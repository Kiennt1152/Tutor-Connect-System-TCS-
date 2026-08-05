package com.tcs.module.platform.dto.response;

import com.tcs.module.contract.enums.ReviewStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminReviewResponse {

    private Long reviewId;
    private BigDecimal rating;
    private String comment;
    private String criteriaJson;
    private ReviewStatus status;

    private Long reviewerId;
    private String reviewerName;
    private String reviewerEmail;
    private boolean anonymous;
    private String publicDisplayName;

    private Long tutorUserId;
    private String tutorName;

    private Long classId;
    private String classTitle;
    private String subjectName;

    private String tutorReply;
    private LocalDateTime createdAt;
}
