package com.tcs.module.center.dto.response;

import com.tcs.module.center.enums.CenterTutorMembershipStatus;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Một gia sư là thành viên của trung tâm (quản lý danh sách gia sư). */
@Getter
@Builder
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

    @Getter
    @Builder
    public static class AppliedPost {
        private Long recruitmentId;
        private String postTitle;
        private RecruitmentApplicationStatus applicationStatus;
        private LocalDateTime appliedAt;
    }
}
