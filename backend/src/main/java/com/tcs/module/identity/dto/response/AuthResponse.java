package com.tcs.module.identity.dto.response;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private Long userId;
    private String email;
    private UserRole role;
    private String displayName;
    private UserStatus status;
}
