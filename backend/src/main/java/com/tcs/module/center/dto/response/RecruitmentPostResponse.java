package com.tcs.module.center.dto.response;

import com.tcs.module.center.enums.RecruitmentPostStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một tin tuyển gia sư (FT-33) — dùng cho trung tâm quản lý và gia sư xem tin đang mở. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentPostResponse {

    private Long recruitmentId;
    private Long centerId;
    private String centerName;

    /** Lớp mà tin này tuyển cho (nếu có). Null = tin tuyển chung. */
    private Long classId;
    private String classTitle;

    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private Integer requiredExperience;
    private Integer maxPositions;

    private Long subjectId;
    private String subjectName;

    private Long locationId;
    /** Địa điểm gộp để hiển thị, VD: "12 Trần Phú, Hà Nội". */
    private String locationLabel;
    /** Tách sẵn để đổ lại form khi sửa tin. */
    private String provinceName;
    private String wardName;
    private String addressDetail;

    /**
     * true khi gia sư đang xem đã thuộc đội ngũ của trung tâm này — không được ứng tuyển.
     * Luôn false ở các màn không phải gia sư xem (trung tâm quản lý tin của mình).
     */
    private boolean alreadyCenterTutor;

    private RecruitmentPostStatus status;
    private LocalDateTime publishedAt;
    /**
     * Mốc tin hết hạn hiển thị: 30 ngày kể từ {@code publishedAt}. Quá hạn tin tự gỡ
     * về nháp. Null khi tin chưa đăng. Dùng cho đồng hồ đếm ngược ở giao diện.
     */
    private LocalDateTime expiresAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Số đơn đã nộp (trung tâm dùng để biết tin có ứng viên chưa). */
    private long applicationCount;

    public static RecruitmentPostResponseBuilder builder() {
        return new RecruitmentPostResponseBuilder();
    }

    public static class RecruitmentPostResponseBuilder {
        private Long recruitmentId;
        private Long centerId;
        private String centerName;
        private Long classId;
        private String classTitle;
        private String title;
        private String description;
        private String requirements;
        private String benefits;
        private Integer requiredExperience;
        private Integer maxPositions;
        private Long subjectId;
        private String subjectName;
        private Long locationId;
        private String locationLabel;
        private String provinceName;
        private String wardName;
        private String addressDetail;
        private boolean alreadyCenterTutor;
        private RecruitmentPostStatus status;
        private LocalDateTime publishedAt;
        private LocalDateTime expiresAt;
        private LocalDateTime closedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private long applicationCount;

        public RecruitmentPostResponseBuilder recruitmentId(Long recruitmentId) { this.recruitmentId = recruitmentId; return this; }
        public RecruitmentPostResponseBuilder centerId(Long centerId) { this.centerId = centerId; return this; }
        public RecruitmentPostResponseBuilder centerName(String centerName) { this.centerName = centerName; return this; }
        public RecruitmentPostResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public RecruitmentPostResponseBuilder classTitle(String classTitle) { this.classTitle = classTitle; return this; }
        public RecruitmentPostResponseBuilder title(String title) { this.title = title; return this; }
        public RecruitmentPostResponseBuilder description(String description) { this.description = description; return this; }
        public RecruitmentPostResponseBuilder requirements(String requirements) { this.requirements = requirements; return this; }
        public RecruitmentPostResponseBuilder benefits(String benefits) { this.benefits = benefits; return this; }
        public RecruitmentPostResponseBuilder requiredExperience(Integer requiredExperience) { this.requiredExperience = requiredExperience; return this; }
        public RecruitmentPostResponseBuilder maxPositions(Integer maxPositions) { this.maxPositions = maxPositions; return this; }
        public RecruitmentPostResponseBuilder subjectId(Long subjectId) { this.subjectId = subjectId; return this; }
        public RecruitmentPostResponseBuilder subjectName(String subjectName) { this.subjectName = subjectName; return this; }
        public RecruitmentPostResponseBuilder locationId(Long locationId) { this.locationId = locationId; return this; }
        public RecruitmentPostResponseBuilder locationLabel(String locationLabel) { this.locationLabel = locationLabel; return this; }
        public RecruitmentPostResponseBuilder provinceName(String provinceName) { this.provinceName = provinceName; return this; }
        public RecruitmentPostResponseBuilder wardName(String wardName) { this.wardName = wardName; return this; }
        public RecruitmentPostResponseBuilder addressDetail(String addressDetail) { this.addressDetail = addressDetail; return this; }
        public RecruitmentPostResponseBuilder alreadyCenterTutor(boolean alreadyCenterTutor) { this.alreadyCenterTutor = alreadyCenterTutor; return this; }
        public RecruitmentPostResponseBuilder status(RecruitmentPostStatus status) { this.status = status; return this; }
        public RecruitmentPostResponseBuilder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public RecruitmentPostResponseBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public RecruitmentPostResponseBuilder closedAt(LocalDateTime closedAt) { this.closedAt = closedAt; return this; }
        public RecruitmentPostResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RecruitmentPostResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public RecruitmentPostResponseBuilder applicationCount(long applicationCount) { this.applicationCount = applicationCount; return this; }

        public RecruitmentPostResponse build() {
            return new RecruitmentPostResponse(recruitmentId, centerId, centerName, classId, classTitle, title, description, requirements, benefits, requiredExperience, maxPositions, subjectId, subjectName, locationId, locationLabel, provinceName, wardName, addressDetail, alreadyCenterTutor, status, publishedAt, expiresAt, closedAt, createdAt, updatedAt, applicationCount);
        }
    }
}
