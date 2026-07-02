package com.tcs.module.identity.dto.request;

import com.tcs.module.profile.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * UC: hoan tat tao tai khoan cho nguoi dang nhap Google lan dau - chon vai tro va so dien thoai.
 * `accessToken` duoc xac thuc lai voi Google de dam bao email khong bi gia mao.
 */
@Getter
@Setter
public class GoogleCompleteRequest {

    @NotBlank(message = "Thiếu Google access token")
    private String accessToken;

    @NotNull(message = "Vui lòng chọn loại tài khoản")
    private UserRole role;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
            regexp = "^(0(3|5|7|8|9)\\d{8}|\\+84(3|5|7|8|9)\\d{8})$",
            message = "Số điện thoại không hợp lệ (VD: 0901234567 hoặc +84901234567)")
    private String phone;
}
