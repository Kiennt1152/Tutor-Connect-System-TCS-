package com.tcs.module.center.dto.response;

import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một đơn ứng tuyển — dùng cho cả trung tâm (xem/duyệt) và gia sư (xem đơn của mình). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * Bằng cấp / chứng chỉ gia sư đã nộp và được admin xác minh (chỉ loại CERTIFICATE).
     * KHÔNG bao gồm ảnh CCCD — đó là dữ liệu định danh nhạy cảm, admin đã xác minh danh tính,
     * trung tâm không cần thấy.
     */
    private List<CertificateInfo> certificates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateInfo {
        /** Loại giấy tờ: ID_CARD (CCCD mặt trước), DEGREE (CCCD mặt sau), CERTIFICATE, LICENSE. */
        private String documentType;
        private Long fileId;
        private String fileName;
        private String fileUrl;
        private String mimeType;
        private Long fileSize;

        public static CertificateInfoBuilder builder() { return new CertificateInfoBuilder(); }

        public static class CertificateInfoBuilder {
            private String documentType;
            private Long fileId;
            private String fileName;
            private String fileUrl;
            private String mimeType;
            private Long fileSize;

            public CertificateInfoBuilder documentType(String documentType) { this.documentType = documentType; return this; }
            public CertificateInfoBuilder fileId(Long fileId) { this.fileId = fileId; return this; }
            public CertificateInfoBuilder fileName(String fileName) { this.fileName = fileName; return this; }
            public CertificateInfoBuilder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
            public CertificateInfoBuilder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
            public CertificateInfoBuilder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
            public CertificateInfo build() {
                return new CertificateInfo(documentType, fileId, fileName, fileUrl, mimeType, fileSize);
            }
        }
    }

    public static RecruitmentApplicationResponseBuilder builder() {
        return new RecruitmentApplicationResponseBuilder();
    }

    public static class RecruitmentApplicationResponseBuilder {
        private Long recruitmentAppId;
        private Long recruitmentId;
        private String postTitle;
        private String centerName;
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
        private List<CertificateInfo> certificates;

        public RecruitmentApplicationResponseBuilder recruitmentAppId(Long recruitmentAppId) { this.recruitmentAppId = recruitmentAppId; return this; }
        public RecruitmentApplicationResponseBuilder recruitmentId(Long recruitmentId) { this.recruitmentId = recruitmentId; return this; }
        public RecruitmentApplicationResponseBuilder postTitle(String postTitle) { this.postTitle = postTitle; return this; }
        public RecruitmentApplicationResponseBuilder centerName(String centerName) { this.centerName = centerName; return this; }
        public RecruitmentApplicationResponseBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
        public RecruitmentApplicationResponseBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
        public RecruitmentApplicationResponseBuilder tutorPhone(String tutorPhone) { this.tutorPhone = tutorPhone; return this; }
        public RecruitmentApplicationResponseBuilder tutorAvatar(String tutorAvatar) { this.tutorAvatar = tutorAvatar; return this; }
        public RecruitmentApplicationResponseBuilder experienceYears(Integer experienceYears) { this.experienceYears = experienceYears; return this; }
        public RecruitmentApplicationResponseBuilder ratingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; return this; }
        public RecruitmentApplicationResponseBuilder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public RecruitmentApplicationResponseBuilder coverLetter(String coverLetter) { this.coverLetter = coverLetter; return this; }
        public RecruitmentApplicationResponseBuilder status(RecruitmentApplicationStatus status) { this.status = status; return this; }
        public RecruitmentApplicationResponseBuilder appliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; return this; }
        public RecruitmentApplicationResponseBuilder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public RecruitmentApplicationResponseBuilder certificates(List<CertificateInfo> certificates) { this.certificates = certificates; return this; }

        public RecruitmentApplicationResponse build() {
            return new RecruitmentApplicationResponse(recruitmentAppId, recruitmentId, postTitle, centerName, tutorId, tutorName, tutorPhone, tutorAvatar, experienceYears, ratingAvg, verificationStatus, coverLetter, status, appliedAt, reviewedAt, certificates);
        }
    }
}
