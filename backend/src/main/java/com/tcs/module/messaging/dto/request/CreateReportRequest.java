package com.tcs.module.messaging.dto.request;

import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportTargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequest {

    private ReportTargetType targetType;
    private Long targetId;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
}
