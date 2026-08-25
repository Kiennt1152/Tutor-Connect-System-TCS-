package com.tcs.module.contract.dto.response;

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

    public static ReviewableAssignmentResponseBuilder builder() {
        return new ReviewableAssignmentResponseBuilder();
    }

    public static class ReviewableAssignmentResponseBuilder {
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

        public ReviewableAssignmentResponseBuilder assignmentId(Long assignmentId) { this.assignmentId = assignmentId; return this; }
        public ReviewableAssignmentResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public ReviewableAssignmentResponseBuilder classTitle(String classTitle) { this.classTitle = classTitle; return this; }
        public ReviewableAssignmentResponseBuilder subjectName(String subjectName) { this.subjectName = subjectName; return this; }
        public ReviewableAssignmentResponseBuilder classStatus(String classStatus) { this.classStatus = classStatus; return this; }
        public ReviewableAssignmentResponseBuilder tutorUserId(Long tutorUserId) { this.tutorUserId = tutorUserId; return this; }
        public ReviewableAssignmentResponseBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
        public ReviewableAssignmentResponseBuilder reviewed(boolean reviewed) { this.reviewed = reviewed; return this; }
        public ReviewableAssignmentResponseBuilder reviewId(Long reviewId) { this.reviewId = reviewId; return this; }
        public ReviewableAssignmentResponseBuilder rating(BigDecimal rating) { this.rating = rating; return this; }
        public ReviewableAssignmentResponseBuilder comment(String comment) { this.comment = comment; return this; }
        public ReviewableAssignmentResponseBuilder criteriaJson(String criteriaJson) { this.criteriaJson = criteriaJson; return this; }
        public ReviewableAssignmentResponseBuilder anonymous(boolean anonymous) { this.anonymous = anonymous; return this; }
        public ReviewableAssignmentResponseBuilder reviewerDisplayName(String reviewerDisplayName) { this.reviewerDisplayName = reviewerDisplayName; return this; }
        public ReviewableAssignmentResponseBuilder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public ReviewableAssignmentResponseBuilder tutorReply(String tutorReply) { this.tutorReply = tutorReply; return this; }
        public ReviewableAssignmentResponseBuilder tutorReplyAt(LocalDateTime tutorReplyAt) { this.tutorReplyAt = tutorReplyAt; return this; }
        public ReviewableAssignmentResponseBuilder reviewable(boolean reviewable) { this.reviewable = reviewable; return this; }
        public ReviewableAssignmentResponseBuilder reviewsSubmitted(int reviewsSubmitted) { this.reviewsSubmitted = reviewsSubmitted; return this; }
        public ReviewableAssignmentResponseBuilder reviewOverdue(boolean reviewOverdue) { this.reviewOverdue = reviewOverdue; return this; }

        public ReviewableAssignmentResponse build() {
            return new ReviewableAssignmentResponse(assignmentId, classId, classTitle, subjectName, classStatus, tutorUserId, tutorName, reviewed, reviewId, rating, comment, criteriaJson, anonymous, reviewerDisplayName, reviewedAt, tutorReply, tutorReplyAt, reviewable, reviewsSubmitted, reviewOverdue);
        }
    }
}
