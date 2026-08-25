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

    /** true khi đã đủ các trường bắt buộc để ký hợp đồng (chỉ dùng ở response). */
    private Boolean complete;
}
