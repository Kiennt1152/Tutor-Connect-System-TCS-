package com.tcs.module.center.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một gia sư trong danh sách để trung tâm chọn gán vào lớp. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorOptionResponse {

    private Long tutorId;
    private String fullName;
    private Integer experienceYears;
    private BigDecimal ratingAvg;
    private String verificationStatus;
    private String phone;
    private String avatar;
    private String bio;

    /** Gia sư bị trùng lịch dạy với lớp đang xét (chỉ có ý nghĩa khi truyền classId). */
    private boolean scheduleConflict;
    /** Tên lớp gây trùng lịch (null nếu không trùng). */
    private String conflictClassTitle;

    public TutorOptionResponse() {}

    public TutorOptionResponse(Long tutorId, String fullName, Integer experienceYears, BigDecimal ratingAvg, String verificationStatus, String phone, String avatar, String bio, boolean scheduleConflict, String conflictClassTitle) {
        this.tutorId = tutorId;
        this.fullName = fullName;
        this.experienceYears = experienceYears;
        this.ratingAvg = ratingAvg;
        this.verificationStatus = verificationStatus;
        this.phone = phone;
        this.avatar = avatar;
        this.bio = bio;
        this.scheduleConflict = scheduleConflict;
        this.conflictClassTitle = conflictClassTitle;
    }

    public Long getTutorId() { return tutorId; }
    public void setTutorId(Long tutorId) { this.tutorId = tutorId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public boolean isScheduleConflict() { return scheduleConflict; }
    public void setScheduleConflict(boolean scheduleConflict) { this.scheduleConflict = scheduleConflict; }
    public String getConflictClassTitle() { return conflictClassTitle; }
    public void setConflictClassTitle(String conflictClassTitle) { this.conflictClassTitle = conflictClassTitle; }

    public static TutorOptionResponseBuilder builder() {
        return new TutorOptionResponseBuilder();
    }

    public static class TutorOptionResponseBuilder {
        private Long tutorId;
        private String fullName;
        private Integer experienceYears;
        private BigDecimal ratingAvg;
        private String verificationStatus;
        private String phone;
        private String avatar;
        private String bio;
        private boolean scheduleConflict;
        private String conflictClassTitle;

        public TutorOptionResponseBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
        public TutorOptionResponseBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public TutorOptionResponseBuilder experienceYears(Integer experienceYears) { this.experienceYears = experienceYears; return this; }
        public TutorOptionResponseBuilder ratingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; return this; }
        public TutorOptionResponseBuilder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public TutorOptionResponseBuilder phone(String phone) { this.phone = phone; return this; }
        public TutorOptionResponseBuilder avatar(String avatar) { this.avatar = avatar; return this; }
        public TutorOptionResponseBuilder bio(String bio) { this.bio = bio; return this; }
        public TutorOptionResponseBuilder scheduleConflict(boolean scheduleConflict) { this.scheduleConflict = scheduleConflict; return this; }
        public TutorOptionResponseBuilder conflictClassTitle(String conflictClassTitle) { this.conflictClassTitle = conflictClassTitle; return this; }
        public TutorOptionResponse build() {
            return new TutorOptionResponse(tutorId, fullName, experienceYears, ratingAvg, verificationStatus, phone, avatar, bio, scheduleConflict, conflictClassTitle);
        }
    }
}
