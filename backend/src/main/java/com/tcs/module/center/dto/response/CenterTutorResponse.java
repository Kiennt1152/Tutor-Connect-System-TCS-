package com.tcs.module.center.dto.response;

import com.tcs.module.center.enums.CenterTutorMembershipStatus;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một gia sư là thành viên của trung tâm (quản lý danh sách gia sư). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterTutorResponse {

    private Long membershipId;
    private Long tutorId;
    private String tutorName;
    private String tutorPhone;
    private String tutorAvatar;
    private Integer experienceYears;
    private BigDecimal ratingAvg;
    private String verificationStatus;

    private LocalDateTime joinedAt;
    private CenterTutorMembershipStatus status;

    /** Các tin tuyển dụng của trung tâm mà gia sư này đã ứng tuyển (kèm trạng thái đơn). */
    private List<AppliedPost> appliedPosts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedPost {
        private Long recruitmentId;
        private String postTitle;
        private RecruitmentApplicationStatus applicationStatus;
        private LocalDateTime appliedAt;

        public static AppliedPostBuilder builder() { return new AppliedPostBuilder(); }

        public static class AppliedPostBuilder {
            private Long recruitmentId;
            private String postTitle;
            private RecruitmentApplicationStatus applicationStatus;
            private LocalDateTime appliedAt;

            public AppliedPostBuilder recruitmentId(Long recruitmentId) { this.recruitmentId = recruitmentId; return this; }
            public AppliedPostBuilder postTitle(String postTitle) { this.postTitle = postTitle; return this; }
            public AppliedPostBuilder applicationStatus(RecruitmentApplicationStatus applicationStatus) { this.applicationStatus = applicationStatus; return this; }
            public AppliedPostBuilder appliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; return this; }
            public AppliedPost build() { return new AppliedPost(recruitmentId, postTitle, applicationStatus, appliedAt); }
        }
    }

    public static CenterTutorResponseBuilder builder() { return new CenterTutorResponseBuilder(); }

    public static class CenterTutorResponseBuilder {
        private Long membershipId;
        private Long tutorId;
        private String tutorName;
        private String tutorPhone;
        private String tutorAvatar;
        private Integer experienceYears;
        private BigDecimal ratingAvg;
        private String verificationStatus;
        private LocalDateTime joinedAt;
        private CenterTutorMembershipStatus status;
        private List<AppliedPost> appliedPosts;

        public CenterTutorResponseBuilder membershipId(Long membershipId) { this.membershipId = membershipId; return this; }
        public CenterTutorResponseBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
        public CenterTutorResponseBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
        public CenterTutorResponseBuilder tutorPhone(String tutorPhone) { this.tutorPhone = tutorPhone; return this; }
        public CenterTutorResponseBuilder tutorAvatar(String tutorAvatar) { this.tutorAvatar = tutorAvatar; return this; }
        public CenterTutorResponseBuilder experienceYears(Integer experienceYears) { this.experienceYears = experienceYears; return this; }
        public CenterTutorResponseBuilder ratingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; return this; }
        public CenterTutorResponseBuilder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public CenterTutorResponseBuilder joinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; return this; }
        public CenterTutorResponseBuilder status(CenterTutorMembershipStatus status) { this.status = status; return this; }
        public CenterTutorResponseBuilder appliedPosts(List<AppliedPost> appliedPosts) { this.appliedPosts = appliedPosts; return this; }
        public CenterTutorResponse build() {
            return new CenterTutorResponse(membershipId, tutorId, tutorName, tutorPhone, tutorAvatar, experienceYears, ratingAvg, verificationStatus, joinedAt, status, appliedPosts);
        }
    }
}
