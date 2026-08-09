package com.tcs.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Gia sư được trung tâm đề cử cho một yêu cầu (shortlist) — phụ huynh chọn 1 trong số này. */
@Getter
@Builder
public class CandidateTutorResponse {
    private Long tutorId;
    private String fullName;
    private Integer experienceYears;
    private BigDecimal ratingAvg;
    /** Bằng cấp / chứng chỉ đã xác minh của gia sư — để phụ huynh xem trước khi chọn. */
    private List<CertificateInfo> certificates;

    @Getter
    @Builder
    public static class CertificateInfo {
        private String documentType;
        private String fileName;
        private String fileUrl;
        private String mimeType;
        private Long fileSize;
    }
}
