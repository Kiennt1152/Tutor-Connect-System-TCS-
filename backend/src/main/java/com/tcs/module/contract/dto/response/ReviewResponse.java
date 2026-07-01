package com.tcs.module.contract.dto.response;

import com.tcs.module.contract.enums.ReviewType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewResponse {

    private Long reviewId;
    private Long assignmentId;
    private Long reviewerId;
    private Long revieweeId;
    private ReviewType reviewType;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
