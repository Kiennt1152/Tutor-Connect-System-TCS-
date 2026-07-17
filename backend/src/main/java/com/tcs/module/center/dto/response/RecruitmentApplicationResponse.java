package com.tcs.module.center.dto.response;

import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Một đơn ứng tuyển — dùng cho cả trung tâm (xem/duyệt) và gia sư (xem đơn của mình). */
@Getter
@Builder
public class RecruitmentApplicationResponse {

    private Long recruitmentAppId;
    private Long recruitmentId;
    private String postTitle;
    private String centerName;

    // Thông tin gia sư ứng tuyển (để trung tâm xét duyệt).
    private Long tutorId;
    private String tutorName;
    private String tutorPhone;
    private String tutorAvatar;
    private Integer experienceYears;
    private BigDecimal ratingAvg;
    private String verificationStatus;

    private String coverLetter;
    private RecruitmentApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}
