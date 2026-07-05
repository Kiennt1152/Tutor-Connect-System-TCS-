package com.tcs.module.marketplace.dto.response;

import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorApplicationResponse {

    private Long applicationId;

    // Class info
    private Long classId;
    private String classTitle;

    // Tutor info
    private Long tutorId;
    private String tutorName;
    private String tutorAvatarUrl;
    private BigDecimal tutorRatingAvg;
    private String tutorVerificationStatus;

    // Application detail
    private BigDecimal proposedRate;
    private String coverLetter;
    private TutorApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}