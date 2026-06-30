package com.tcs.module.platform.mapper;

import com.tcs.module.identity.entity.User;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.profile.enums.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformMapper {

    public UserRole resolveRole(UserProfileBundle profiles) {
        if (profiles.platformAdmin() != null) {
            return UserRole.PLATFORM_ADMIN;
        }
        if (profiles.tutor() != null) {
            return UserRole.TUTOR;
        }
        if (profiles.tutorCenter() != null) {
            return UserRole.TUTOR_CENTER;
        }
        if (profiles.client() != null) {
            return UserRole.CLIENT;
        }
        return UserRole.UNKNOWN;
    }

    public UserListItemResponse toUserListItem(User user, UserProfileBundle profiles) {
        return UserListItemResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(resolvePhone(user, profiles))
                .status(user.getStatus())
                .role(resolveRole(profiles))
                .displayName(resolveDisplayName(user, profiles))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String resolvePhone(User user, UserProfileBundle profiles) {
        if (StringUtils.hasText(user.getPhone())) {
            return user.getPhone();
        }
        if (profiles.client() != null && StringUtils.hasText(profiles.client().getPhone())) {
            return profiles.client().getPhone();
        }
        if (profiles.tutor() != null && StringUtils.hasText(profiles.tutor().getPhone())) {
            return profiles.tutor().getPhone();
        }
        if (profiles.tutorCenter() != null && StringUtils.hasText(profiles.tutorCenter().getPhone())) {
            return profiles.tutorCenter().getPhone();
        }
        return null;
    }

    private String resolveDisplayName(User user, UserProfileBundle profiles) {
        if (profiles.platformAdmin() != null) {
            return profiles.platformAdmin().getFullName();
        }
        if (profiles.client() != null) {
            return profiles.client().getFullName();
        }
        if (profiles.tutor() != null) {
            return profiles.tutor().getFullName();
        }
        if (profiles.tutorCenter() != null) {
            return profiles.tutorCenter().getCompanyName();
        }
        return user.getEmail();
    }
}
