package com.tcs.module.platform.dto.request;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserAdminRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    private String displayName;

    private String phone;

    @NotNull(message = "Vai trò không được để trống")
    private UserRole role;

    private UserStatus status;
}
