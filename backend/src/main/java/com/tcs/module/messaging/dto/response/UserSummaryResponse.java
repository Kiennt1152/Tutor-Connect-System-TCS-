package com.tcs.module.messaging.dto.response;

import com.tcs.module.profile.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryResponse {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    private UserRole role;
}
