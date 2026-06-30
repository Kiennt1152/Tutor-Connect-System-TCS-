package com.tcs.module.platform.dto.response;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserListItemResponse {
    Long userId;
    String email;
    String phone;
    UserStatus status;
    UserRole role;
    String displayName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
