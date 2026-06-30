package com.tcs.module.identity.dto.request;

import com.tcs.module.profile.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UC-01 Register Account - chi thu thap cac truong baseline (BR-UC01-09).
 * Cac chi tiet ho so theo vai tro duoc bo sung sau khi dang nhap.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotNull(message = "Vui lòng chọn loại tài khoản")
    private UserRole role;

    @NotBlank(message = "Vui lòng nhập tên hiển thị")
    @Size(min = 2, max = 50, message = "Tên hiển thị phải từ 2 đến 50 ký tự")
    private String displayName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
            regexp = "^(0(3|5|7|8|9)\\d{8}|\\+84(3|5|7|8|9)\\d{8})$",
            message = "Số điện thoại không hợp lệ (VD: 0901234567 hoặc +84901234567)")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Thiếu mã xác thực email. Vui lòng xác thực email trước.")
    private String verifiedEmailToken;
}
