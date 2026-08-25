package com.tcs.module.contract.dto.request;

import com.tcs.module.contract.enums.ReviewType;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {

    private Long assignmentId;
    private Long revieweeId;
    private ReviewType reviewType;
    private Integer rating;
    private String comment;

    private Boolean anonymous;

    private String displayName;

    private List<ReviewCriterionDto> criteria;
}
