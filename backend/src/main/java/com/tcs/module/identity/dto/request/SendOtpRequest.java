package com.tcs.module.identity.dto.request;

import com.tcs.module.profile.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UC-01 buoc 2: yeu cau gui ma OTP toi email. Chua tao tai khoan.
 */
@Getter
@Setter
public class SendOtpRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    /** Loai tai khoan (tham khao - khong bat buoc de gui ma). */
    private UserRole role;
}
