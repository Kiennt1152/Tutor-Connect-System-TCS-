package com.tcs.module.center.dto.response;

import com.tcs.module.center.enums.RecruitmentPostStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitmentPostResponse {

    private Long recruitmentId;
    private Long centerId;
    private String centerName;
    private String title;
    private String description;
    private Integer maxPositions;
    private RecruitmentPostStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
