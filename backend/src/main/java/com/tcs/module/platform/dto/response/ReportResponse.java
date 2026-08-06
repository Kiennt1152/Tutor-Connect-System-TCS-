package com.tcs.module.platform.dto.response;

import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResponse {

    private Long reportId;
    private Long reporterId;
    private String reporterEmail;
    private ReportTargetType targetType;
    private Long targetId;
    private String classTitle;
    private String classStatus;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
    private List<String> evidenceUrlList;
    private ReportStatus status;
    private String issueType;
    private String issueTypeLabel;
    private String lessonRef;
    private LocalDate occurredAt;
    private String requestedAction;
    private String requestedActionLabel;
    private Long linkedDisputeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
