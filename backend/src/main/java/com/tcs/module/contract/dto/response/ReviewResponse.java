package com.tcs.module.contract.dto.response;

import com.tcs.module.contract.enums.ReviewType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public ReviewResponse() {}

    public ReviewResponse(Long reviewId, Long assignmentId, Long reviewerId, Long revieweeId, ReviewType reviewType, BigDecimal rating, String comment, String tutorReply, LocalDateTime tutorReplyAt, String criteriaJson, String classTitle, String subjectName, boolean anonymous, String reviewerDisplayName, LocalDateTime createdAt) {
        this.reviewId = reviewId;
        this.assignmentId = assignmentId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.reviewType = reviewType;
        this.rating = rating;
        this.comment = comment;
        this.tutorReply = tutorReply;
        this.tutorReplyAt = tutorReplyAt;
        this.criteriaJson = criteriaJson;
        this.classTitle = classTitle;
        this.subjectName = subjectName;
        this.anonymous = anonymous;
        this.reviewerDisplayName = reviewerDisplayName;
        this.createdAt = createdAt;
    }

    public static ReviewResponseBuilder builder() {
        return new ReviewResponseBuilder();
    }

    public static class ReviewResponseBuilder {
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

        public ReviewResponseBuilder reviewId(Long reviewId) { this.reviewId = reviewId; return this; }
        public ReviewResponseBuilder assignmentId(Long assignmentId) { this.assignmentId = assignmentId; return this; }
        public ReviewResponseBuilder reviewerId(Long reviewerId) { this.reviewerId = reviewerId; return this; }
        public ReviewResponseBuilder revieweeId(Long revieweeId) { this.revieweeId = revieweeId; return this; }
        public ReviewResponseBuilder reviewType(ReviewType reviewType) { this.reviewType = reviewType; return this; }
        public ReviewResponseBuilder rating(BigDecimal rating) { this.rating = rating; return this; }
        public ReviewResponseBuilder comment(String comment) { this.comment = comment; return this; }
        public ReviewResponseBuilder tutorReply(String tutorReply) { this.tutorReply = tutorReply; return this; }
        public ReviewResponseBuilder tutorReplyAt(LocalDateTime tutorReplyAt) { this.tutorReplyAt = tutorReplyAt; return this; }
        public ReviewResponseBuilder criteriaJson(String criteriaJson) { this.criteriaJson = criteriaJson; return this; }
        public ReviewResponseBuilder classTitle(String classTitle) { this.classTitle = classTitle; return this; }
        public ReviewResponseBuilder subjectName(String subjectName) { this.subjectName = subjectName; return this; }
        public ReviewResponseBuilder anonymous(boolean anonymous) { this.anonymous = anonymous; return this; }
        public ReviewResponseBuilder reviewerDisplayName(String reviewerDisplayName) { this.reviewerDisplayName = reviewerDisplayName; return this; }
        public ReviewResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ReviewResponse build() {
            return new ReviewResponse(reviewId, assignmentId, reviewerId, revieweeId, reviewType, rating, comment, tutorReply, tutorReplyAt, criteriaJson, classTitle, subjectName, anonymous, reviewerDisplayName, createdAt);
        }
    }
}
