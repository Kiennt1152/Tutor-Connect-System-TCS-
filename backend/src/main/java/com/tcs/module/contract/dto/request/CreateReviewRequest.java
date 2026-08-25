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

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public Long getRevieweeId() { return revieweeId; }
    public void setRevieweeId(Long revieweeId) { this.revieweeId = revieweeId; }
    public ReviewType getReviewType() { return reviewType; }
    public void setReviewType(ReviewType reviewType) { this.reviewType = reviewType; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<ReviewCriterionDto> getCriteria() { return criteria; }
    public void setCriteria(List<ReviewCriterionDto> criteria) { this.criteria = criteria; }
}
