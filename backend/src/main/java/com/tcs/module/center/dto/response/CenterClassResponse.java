package com.tcs.module.center.dto.response;

import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterClassResponse {

    private Long classId;
    private String title;
    private String description;
    private Long creatorId;
    private Long centerId;
    private Long categoryId;
    private String categoryName;
    private Long subjectId;
    private String subjectName;
    private Long gradeId;
    private String gradeName;
    private Long locationId;
    private String locationLabel;
    /** Địa chỉ thô (address_line) để hiển thị. */
    private String locationText;
    /** Địa điểm 2 cấp + địa chỉ cụ thể, để đổ lại form khi chỉnh sửa. */
    private String provinceName;
    private String wardName;
    private String addressDetail;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private RecurringType recurringType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private Integer maxStudents;
    private Integer minStudents;
    /** Số học sinh đã ghi danh (ENROLLED) — để hiển thị tiến độ & điều kiện kích hoạt. */
    private long enrolledCount;
    /** BF-04: hạn ghi danh, 30 ngày kể từ lúc đăng tải. Null khi lớp chưa mở ghi danh. */
    private LocalDate enrollmentDeadline;
    /**
     * Mốc ghi danh thực sự đóng, dùng cho đồng hồ đếm ngược ở giao diện.
     * Bằng hết ngày {@code enrollmentDeadline} — khớp đúng thời điểm bộ lịch tự đóng lớp.
     */
    private LocalDateTime enrollmentExpiresAt;
    /**
     * BR-06 / AF-03: true nếu trung tâm còn được sửa thông tin lớp.
     * Thành false ngay khi có học sinh đăng ký (kể cả đang chờ ký hợp đồng).
     */
    private boolean editable;
    /** Lý do lớp bị khoá sửa, null khi {@code editable = true}. */
    private String editLockReason;
    /** Loại lớp: EXTERNAL (yêu cầu ngoài) / SELF (tự tạo). */
    private String originType;
    /** Mẫu hợp đồng học viên đã chọn cho lớp (để đổ lại form khi sửa). */
    private Long contractTemplateId;
    /** Nội dung điều khoản HĐ học viên đã lưu cho lớp (để đổ lại form khi sửa). */
    private String contractContent;
    private TutoringClassStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ScheduleSlotResponse> schedule;

    // Gia sư đang được gán dạy lớp (null nếu chưa gán).
    private Long assignedTutorId;
    private String assignedTutorName;

    // Gia sư phụ (backup) của lớp, dùng để dạy thay khi gia sư chính báo ốm/bận (null nếu chưa gán).
    private Long assistantTutorId;
    private String assistantTutorName;

    // Danh sách học sinh đã ghi danh (ENROLLED) – dùng cho phần xem chi tiết.
    private List<StudentAttendanceResponse> students;

    /** true nếu GIA SƯ đã xác nhận hoàn thành — trung tâm cần xác nhận để đóng lớp. */
    private boolean tutorCompletionConfirmed;

    public Long getClassId() { return classId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getCreatorId() { return creatorId; }
    public Long getCenterId() { return centerId; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public Long getGradeId() { return gradeId; }
    public String getGradeName() { return gradeName; }
    public Long getLocationId() { return locationId; }
    public String getLocationLabel() { return locationLabel; }
    public String getLocationText() { return locationText; }
    public String getProvinceName() { return provinceName; }
    public String getWardName() { return wardName; }
    public String getAddressDetail() { return addressDetail; }
    public LessonMode getLessonMode() { return lessonMode; }
    public Integer getNumberOfSessions() { return numberOfSessions; }
    public RecurringType getRecurringType() { return recurringType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getTuitionFee() { return tuitionFee; }
    public Integer getMaxStudents() { return maxStudents; }
    public Integer getMinStudents() { return minStudents; }
    public long getEnrolledCount() { return enrolledCount; }
    public LocalDate getEnrollmentDeadline() { return enrollmentDeadline; }
    public LocalDateTime getEnrollmentExpiresAt() { return enrollmentExpiresAt; }
    public boolean isEditable() { return editable; }
    public String getEditLockReason() { return editLockReason; }
    public String getOriginType() { return originType; }
    public Long getContractTemplateId() { return contractTemplateId; }
    public String getContractContent() { return contractContent; }
    public TutoringClassStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ScheduleSlotResponse> getSchedule() { return schedule; }
    public Long getAssignedTutorId() { return assignedTutorId; }
    public String getAssignedTutorName() { return assignedTutorName; }
    public Long getAssistantTutorId() { return assistantTutorId; }
    public String getAssistantTutorName() { return assistantTutorName; }
    public List<StudentAttendanceResponse> getStudents() { return students; }
    public boolean isTutorCompletionConfirmed() { return tutorCompletionConfirmed; }

    public static CenterClassResponseBuilder builder() {
        return new CenterClassResponseBuilder();
    }

    public static class CenterClassResponseBuilder {
        private Long classId;
        private String title;
        private String description;
        private Long creatorId;
        private Long centerId;
        private Long categoryId;
        private String categoryName;
        private Long subjectId;
        private String subjectName;
        private Long gradeId;
        private String gradeName;
        private Long locationId;
        private String locationLabel;
        private String locationText;
        private String provinceName;
        private String wardName;
        private String addressDetail;
        private LessonMode lessonMode;
        private Integer numberOfSessions;
        private RecurringType recurringType;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal tuitionFee;
        private Integer maxStudents;
        private Integer minStudents;
        private long enrolledCount;
        private LocalDate enrollmentDeadline;
        private LocalDateTime enrollmentExpiresAt;
        private boolean editable;
        private String editLockReason;
        private String originType;
        private Long contractTemplateId;
        private String contractContent;
        private TutoringClassStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ScheduleSlotResponse> schedule;
        private Long assignedTutorId;
        private String assignedTutorName;
        private Long assistantTutorId;
        private String assistantTutorName;
        private List<StudentAttendanceResponse> students;
        private boolean tutorCompletionConfirmed;

        public CenterClassResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public CenterClassResponseBuilder title(String title) { this.title = title; return this; }
        public CenterClassResponseBuilder description(String description) { this.description = description; return this; }
        public CenterClassResponseBuilder creatorId(Long creatorId) { this.creatorId = creatorId; return this; }
        public CenterClassResponseBuilder centerId(Long centerId) { this.centerId = centerId; return this; }
        public CenterClassResponseBuilder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public CenterClassResponseBuilder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public CenterClassResponseBuilder subjectId(Long subjectId) { this.subjectId = subjectId; return this; }
        public CenterClassResponseBuilder subjectName(String subjectName) { this.subjectName = subjectName; return this; }
        public CenterClassResponseBuilder gradeId(Long gradeId) { this.gradeId = gradeId; return this; }
        public CenterClassResponseBuilder gradeName(String gradeName) { this.gradeName = gradeName; return this; }
        public CenterClassResponseBuilder locationId(Long locationId) { this.locationId = locationId; return this; }
        public CenterClassResponseBuilder locationLabel(String locationLabel) { this.locationLabel = locationLabel; return this; }
        public CenterClassResponseBuilder locationText(String locationText) { this.locationText = locationText; return this; }
        public CenterClassResponseBuilder provinceName(String provinceName) { this.provinceName = provinceName; return this; }
        public CenterClassResponseBuilder wardName(String wardName) { this.wardName = wardName; return this; }
        public CenterClassResponseBuilder addressDetail(String addressDetail) { this.addressDetail = addressDetail; return this; }
        public CenterClassResponseBuilder lessonMode(LessonMode lessonMode) { this.lessonMode = lessonMode; return this; }
        public CenterClassResponseBuilder numberOfSessions(Integer numberOfSessions) { this.numberOfSessions = numberOfSessions; return this; }
        public CenterClassResponseBuilder recurringType(RecurringType recurringType) { this.recurringType = recurringType; return this; }
        public CenterClassResponseBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public CenterClassResponseBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public CenterClassResponseBuilder tuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; return this; }
        public CenterClassResponseBuilder maxStudents(Integer maxStudents) { this.maxStudents = maxStudents; return this; }
        public CenterClassResponseBuilder minStudents(Integer minStudents) { this.minStudents = minStudents; return this; }
        public CenterClassResponseBuilder enrolledCount(long enrolledCount) { this.enrolledCount = enrolledCount; return this; }
        public CenterClassResponseBuilder enrollmentDeadline(LocalDate enrollmentDeadline) { this.enrollmentDeadline = enrollmentDeadline; return this; }
        public CenterClassResponseBuilder enrollmentExpiresAt(LocalDateTime enrollmentExpiresAt) { this.enrollmentExpiresAt = enrollmentExpiresAt; return this; }
        public CenterClassResponseBuilder editable(boolean editable) { this.editable = editable; return this; }
        public CenterClassResponseBuilder editLockReason(String editLockReason) { this.editLockReason = editLockReason; return this; }
        public CenterClassResponseBuilder originType(String originType) { this.originType = originType; return this; }
        public CenterClassResponseBuilder contractTemplateId(Long contractTemplateId) { this.contractTemplateId = contractTemplateId; return this; }
        public CenterClassResponseBuilder contractContent(String contractContent) { this.contractContent = contractContent; return this; }
        public CenterClassResponseBuilder status(TutoringClassStatus status) { this.status = status; return this; }
        public CenterClassResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CenterClassResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public CenterClassResponseBuilder schedule(List<ScheduleSlotResponse> schedule) { this.schedule = schedule; return this; }
        public CenterClassResponseBuilder assignedTutorId(Long assignedTutorId) { this.assignedTutorId = assignedTutorId; return this; }
        public CenterClassResponseBuilder assignedTutorName(String assignedTutorName) { this.assignedTutorName = assignedTutorName; return this; }
        public CenterClassResponseBuilder assistantTutorId(Long assistantTutorId) { this.assistantTutorId = assistantTutorId; return this; }
        public CenterClassResponseBuilder assistantTutorName(String assistantTutorName) { this.assistantTutorName = assistantTutorName; return this; }
        public CenterClassResponseBuilder students(List<StudentAttendanceResponse> students) { this.students = students; return this; }
        public CenterClassResponseBuilder tutorCompletionConfirmed(boolean tutorCompletionConfirmed) { this.tutorCompletionConfirmed = tutorCompletionConfirmed; return this; }

        public CenterClassResponse build() {
            return new CenterClassResponse(classId, title, description, creatorId, centerId, categoryId, categoryName, subjectId, subjectName, gradeId, gradeName, locationId, locationLabel, locationText, provinceName, wardName, addressDetail, lessonMode, numberOfSessions, recurringType, startDate, endDate, tuitionFee, maxStudents, minStudents, enrolledCount, enrollmentDeadline, enrollmentExpiresAt, editable, editLockReason, originType, contractTemplateId, contractContent, status, createdAt, updatedAt, schedule, assignedTutorId, assignedTutorName, assistantTutorId, assistantTutorName, students, tutorCompletionConfirmed);
        }
    }
}
