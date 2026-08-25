package com.tcs.module.center.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Thông tin trung tâm hiển thị ở khối BÊN A của hợp đồng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterContractInfoResponse {
    // Lấy sẵn từ hồ sơ trung tâm (chỉ hiển thị).
    private String companyName;
    private String address;
    private String phone;
    private String email;
    // Trung tâm tự nhập thêm (lưu qua system_parameters).
    private String website;
    /** Người đại diện pháp luật — lấy từ CCCD đã xác minh (không nhập tay). */
    private String representativeName;
    private String representativePosition;
    /** Trạng thái xác minh của trung tâm (để chặn tạo mẫu khi chưa xác minh). */
    private String verificationStatus;

    public CenterContractInfoResponse() {}

    public CenterContractInfoResponse(String companyName, String address, String phone, String email, String website, String representativeName, String representativePosition, String verificationStatus) {
        this.companyName = companyName;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.representativeName = representativeName;
        this.representativePosition = representativePosition;
        this.verificationStatus = verificationStatus;
    }

    public String getCompanyName() { return companyName; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
    public String getRepresentativeName() { return representativeName; }
    public String getRepresentativePosition() { return representativePosition; }
    public String getVerificationStatus() { return verificationStatus; }

    public static CenterContractInfoResponseBuilder builder() {
        return new CenterContractInfoResponseBuilder();
    }

    public static class CenterContractInfoResponseBuilder {
        private String companyName;
        private String address;
        private String phone;
        private String email;
        private String website;
        private String representativeName;
        private String representativePosition;
        private String verificationStatus;

        public CenterContractInfoResponseBuilder companyName(String companyName) { this.companyName = companyName; return this; }
        public CenterContractInfoResponseBuilder address(String address) { this.address = address; return this; }
        public CenterContractInfoResponseBuilder phone(String phone) { this.phone = phone; return this; }
        public CenterContractInfoResponseBuilder email(String email) { this.email = email; return this; }
        public CenterContractInfoResponseBuilder website(String website) { this.website = website; return this; }
        public CenterContractInfoResponseBuilder representativeName(String representativeName) { this.representativeName = representativeName; return this; }
        public CenterContractInfoResponseBuilder representativePosition(String representativePosition) { this.representativePosition = representativePosition; return this; }
        public CenterContractInfoResponseBuilder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }

        public CenterContractInfoResponse build() {
            return new CenterContractInfoResponse(companyName, address, phone, email, website, representativeName, representativePosition, verificationStatus);
        }
    }
}
