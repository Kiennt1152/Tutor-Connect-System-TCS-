package com.tcs.module.center.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Thông tin trung tâm hiển thị ở khối BÊN A của hợp đồng. */
@Getter
@Builder
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
}
