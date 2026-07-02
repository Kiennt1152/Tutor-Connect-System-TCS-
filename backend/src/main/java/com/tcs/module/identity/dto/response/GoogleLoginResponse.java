package com.tcs.module.identity.dto.response;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

/**
 * Ket qua dang nhap Google. Neu {@code newUser} = true, tai khoan chua ton tai va chua duoc tao -
 * frontend phai goi {@code POST /api/identity/google/complete} voi vai tro + so dien thoai de
 * hoan tat dang ky. Neu false, cac truong con lai duoc dien nhu mot AuthResponse binh thuong.
 */
@Getter
@Builder
public class GoogleLoginResponse {

    private boolean newUser;
    private String email;
    private String suggestedDisplayName;

    private String accessToken;
    private Long userId;
    private UserRole role;
    private String displayName;
    private UserStatus status;
}
