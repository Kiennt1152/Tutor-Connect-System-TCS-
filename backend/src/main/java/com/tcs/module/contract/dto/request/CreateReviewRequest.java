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

    // Danh gia theo tung tieu chi. Neu co, rating tong duoc tinh = trung binh lam tron cua cac score.
    private List<ReviewCriterionDto> criteria;
}
