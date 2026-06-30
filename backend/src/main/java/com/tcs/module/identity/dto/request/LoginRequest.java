package com.tcs.module.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^[\\x21-\\x7E]+$",
            message = "Mật khẩu chỉ gồm chữ, số và ký tự đặc biệt (không dấu, không khoảng trắng)")
    private String password;
}