package com.tcs.module.platform.dto.response;

import com.tcs.module.platform.enums.AnnouncementAudience;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementResponse {

    private Long announcementId;
    private String title;
    private String content;
    private AnnouncementAudience audience;
    private Boolean published;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
