package com.tcs.module.contract.dto.response;

import com.tcs.module.contract.enums.ReviewType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long reviewId;
    private Long assignmentId;
    private Long reviewerId;
    private Long revieweeId;
    private ReviewType reviewType;
    private BigDecimal rating;
    private String comment;
    private String tutorReply;
    private LocalDateTime tutorReplyAt;
    private String criteriaJson;
    private String classTitle;
    private String subjectName;
    private boolean anonymous;
    private String reviewerDisplayName;
    private LocalDateTime createdAt;
}
