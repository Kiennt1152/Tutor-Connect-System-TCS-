package com.tcs.module.platform.dto.response;

import com.tcs.module.profile.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementResponse {

    private Long announcementId;
    private String title;
    private String content;
    private UserRole targetRole;
    private boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
