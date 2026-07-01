package com.tcs.module.identity.dto.response;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeResponse {

    private Long userId;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private String displayName;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
