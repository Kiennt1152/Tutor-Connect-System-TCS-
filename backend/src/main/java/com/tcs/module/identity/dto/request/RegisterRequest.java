package com.tcs.module.identity.dto.request;

import com.tcs.module.identity.enums.RegisterRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Thiếu mã xác thực email. Vui lòng xác thực email trước.")
    private String token;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotNull(message = "Vui lòng chọn loại tài khoản")
    private RegisterRole role;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,100}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số, "
                    + "chỉ dùng chữ/số/ký tự đặc biệt (không dấu, không khoảng trắng)")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Vui lòng nhập tên hiển thị")
    @Size(min = 2, max = 50, message = "Tên hiển thị phải từ 2 đến 50 ký tự")
    private String displayName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
            regexp = "^(0\\d{9}|\\+84\\d{9})$",
            message = "Số điện thoại không hợp lệ (10 số bắt đầu bằng 0, hoặc +84)")
    private String phone;
}
