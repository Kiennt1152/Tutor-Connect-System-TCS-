package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Dữ liệu tạo/sửa tin tuyển gia sư của trung tâm (FT-33). */
@Getter
@Setter
public class SaveRecruitmentPostRequest {

    /** Lớp cần tuyển gia sư (tuỳ chọn). Null = tin tuyển chung, không gắn lớp. */
    private Long classId;
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private Integer requiredExperience;
    private Integer maxPositions;
    /** Môn học nhập tự do — tìm-hoặc-tạo theo tên (giữ FK toàn vẹn mà không cần dropdown). */
    private String subjectName;
    /** Địa điểm làm việc (tuỳ chọn): cần cả tỉnh + địa chỉ cụ thể thì mới lưu. */
    private String provinceName;
    private String wardName;
    private String addressDetail;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
    public Integer getRequiredExperience() { return requiredExperience; }
    public void setRequiredExperience(Integer requiredExperience) { this.requiredExperience = requiredExperience; }
    public Integer getMaxPositions() { return maxPositions; }
    public void setMaxPositions(Integer maxPositions) { this.maxPositions = maxPositions; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public String getAddressDetail() { return addressDetail; }
    public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }
}
