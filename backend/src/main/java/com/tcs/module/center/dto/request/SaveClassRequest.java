package com.tcs.module.center.dto.request;

import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Dùng chung cho tạo mới và chỉnh sửa lớp học của Trung tâm gia sư (UC-14-B). */
@Getter
@Setter
public class SaveClassRequest {

    private String title;
    private String description;
    // Người dùng tự nhập tên (backend sẽ tìm-hoặc-tạo bản ghi danh mục tương ứng).
    private String categoryName;
    private String subjectName;
    private String gradeName;
    // Địa điểm theo mô hình 2 cấp: Tỉnh/Thành → Phường/Xã, kèm địa chỉ cụ thể tự nhập.
    private String provinceName;
    private String wardName;
    private String addressDetail;
    // (Cũ) địa chỉ tự nhập — giữ lại để tương thích, không bắt buộc.
    private String locationText;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private RecurringType recurringType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private Integer maxStudents;
    /** Số học sinh tối thiểu để kích hoạt lớp (tuỳ chọn; trống = cần ≥ 1). */
    private Integer minStudents;
    /** Loại lớp: EXTERNAL (yêu cầu ngoài, đã có học sinh) / SELF (trung tâm tự tạo). Mặc định SELF. */
    private String originType;
    /** Mẫu hợp đồng trung tâm chọn cho lớp (dùng khi sinh hợp đồng học viên). Tuỳ chọn. */
    private Long contractTemplateId;
    /** Nội dung điều khoản HĐ học viên center tự nhập/sửa khi tạo lớp (nạp sẵn từ mẫu). Tuỳ chọn. */
    private String contractContent;
    private List<ScheduleSlotRequest> schedule;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getGradeName() { return gradeName; }
    public void setGradeName(String gradeName) { this.gradeName = gradeName; }
    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public String getAddressDetail() { return addressDetail; }
    public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }
    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }
    public LessonMode getLessonMode() { return lessonMode; }
    public void setLessonMode(LessonMode lessonMode) { this.lessonMode = lessonMode; }
    public Integer getNumberOfSessions() { return numberOfSessions; }
    public void setNumberOfSessions(Integer numberOfSessions) { this.numberOfSessions = numberOfSessions; }
    public RecurringType getRecurringType() { return recurringType; }
    public void setRecurringType(RecurringType recurringType) { this.recurringType = recurringType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }
    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }
    public Integer getMinStudents() { return minStudents; }
    public void setMinStudents(Integer minStudents) { this.minStudents = minStudents; }
    public String getOriginType() { return originType; }
    public void setOriginType(String originType) { this.originType = originType; }
    public Long getContractTemplateId() { return contractTemplateId; }
    public void setContractTemplateId(Long contractTemplateId) { this.contractTemplateId = contractTemplateId; }
    public String getContractContent() { return contractContent; }
    public void setContractContent(String contractContent) { this.contractContent = contractContent; }
    public List<ScheduleSlotRequest> getSchedule() { return schedule; }
    public void setSchedule(List<ScheduleSlotRequest> schedule) { this.schedule = schedule; }
}
