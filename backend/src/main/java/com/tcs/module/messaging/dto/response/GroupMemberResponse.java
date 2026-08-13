package com.tcs.module.messaging.dto.response;

import com.tcs.module.profile.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupMemberResponse {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    private UserRole role;
    private boolean owner;
}
