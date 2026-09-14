package com.tcs.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
    private Map<String, BigDecimal> proposedRates;
    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;

    private Integer matchScore;
    private boolean recommended;

    /** Số buổi của lớp rơi vào thời gian bận gia sư đã đăng ký (0 = không trùng). */
    private int busyConflictCount;
    /** Tóm tắt các buổi trùng, ví dụ "15/09 (12:00–12:30)"; null khi không trùng. */
    private String busyConflictSummary;
}
