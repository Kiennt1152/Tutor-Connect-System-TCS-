package com.tcs.module.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông tin CCCD của một người (gia sư / phụ huynh) dùng để điền khối "BÊN B" trong hợp đồng.
 * Lưu dưới dạng JSON trong system_parameters (key cccd:{userId}) — không cần cột/migration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CccdInfoDto {

    // ----- Trường định danh (đọc từ mã QR CCCD, user chỉ xác nhận) -----
    private String fullName;
    private String cccdNumber;
    private String dateOfBirth;      // dd/MM/yyyy
    private String gender;
    private String permanentAddress; // địa chỉ thường trú trên CCCD
    private String issueDate;        // dd/MM/yyyy
    private String issuePlace;       // nơi cấp (mặc định Cục CS QLHC về TTXH)

    // ----- Trường bổ sung user tự nhập (không có trên CCCD) -----
    private String workplace;        // nơi công tác / nơi học
    private String tempResidence;    // nơi tạm trú
    private String phone;            // ĐT liên hệ

    public CccdInfoDto() {}

    public CccdInfoDto(String fullName, String cccdNumber, String dateOfBirth, String gender, String permanentAddress, String issueDate, String issuePlace, String workplace, String tempResidence, String phone, Boolean complete) {
        this.fullName = fullName;
        this.cccdNumber = cccdNumber;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.permanentAddress = permanentAddress;
        this.issueDate = issueDate;
        this.issuePlace = issuePlace;
        this.workplace = workplace;
        this.tempResidence = tempResidence;
        this.phone = phone;
        this.complete = complete;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCccdNumber() { return cccdNumber; }
    public void setCccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }
    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
    public String getIssuePlace() { return issuePlace; }
    public void setIssuePlace(String issuePlace) { this.issuePlace = issuePlace; }
    public String getWorkplace() { return workplace; }
    public void setWorkplace(String workplace) { this.workplace = workplace; }
    public String getTempResidence() { return tempResidence; }
    public void setTempResidence(String tempResidence) { this.tempResidence = tempResidence; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getComplete() { return complete; }
    public void setComplete(Boolean complete) { this.complete = complete; }

    public static CccdInfoDtoBuilder builder() {
        return new CccdInfoDtoBuilder();
    }

    public static class CccdInfoDtoBuilder {
        private String fullName;
        private String cccdNumber;
        private String dateOfBirth;
        private String gender;
        private String permanentAddress;
        private String issueDate;
        private String issuePlace;
        private String workplace;
        private String tempResidence;
        private String phone;
        private Boolean complete;

        public CccdInfoDtoBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public CccdInfoDtoBuilder cccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; return this; }
        public CccdInfoDtoBuilder dateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; return this; }
        public CccdInfoDtoBuilder gender(String gender) { this.gender = gender; return this; }
        public CccdInfoDtoBuilder permanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; return this; }
        public CccdInfoDtoBuilder issueDate(String issueDate) { this.issueDate = issueDate; return this; }
        public CccdInfoDtoBuilder issuePlace(String issuePlace) { this.issuePlace = issuePlace; return this; }
        public CccdInfoDtoBuilder workplace(String workplace) { this.workplace = workplace; return this; }
        public CccdInfoDtoBuilder tempResidence(String tempResidence) { this.tempResidence = tempResidence; return this; }
        public CccdInfoDtoBuilder phone(String phone) { this.phone = phone; return this; }
        public CccdInfoDtoBuilder complete(Boolean complete) { this.complete = complete; return this; }

        public CccdInfoDto build() {
            return new CccdInfoDto(fullName, cccdNumber, dateOfBirth, gender, permanentAddress, issueDate, issuePlace, workplace, tempResidence, phone, complete);
        }
    }
}
