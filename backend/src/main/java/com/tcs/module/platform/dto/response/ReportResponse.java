package com.tcs.module.platform.dto.response;

import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResponse {

    private Long reportId;
    private Long reporterId;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportCategory category;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
