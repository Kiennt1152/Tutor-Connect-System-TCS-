package com.tcs.module.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPasswordResetOtpRequest {

    @NotBlank(message = "Thiếu email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập mã OTP")
    @Pattern(regexp = "^\\d+$", message = "Mã OTP chỉ chứa chữ số")
    private String code;
}
