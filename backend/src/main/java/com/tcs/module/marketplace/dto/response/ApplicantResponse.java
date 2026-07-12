package com.tcs.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Một gia sư đã ứng tuyển vào lớp, kèm điểm gợi ý của AI cho Client. */
@Getter
@Setter
@Builder
public class ApplicantResponse {

    private Long applicationId;
    private Long tutorId;
    private Long userId;
    private String fullName;
    private String avatar;
    private String bio;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal ratingAvg;
    private String verificationStatus;

    private BigDecimal proposedRate;
    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;

    /** Điểm AI gợi ý 0–100 (càng cao càng phù hợp với lớp). */
    private Integer matchScore;
    /** true nếu nằm trong Top 5 AI gợi ý cho Client. */
    private boolean recommended;
}
