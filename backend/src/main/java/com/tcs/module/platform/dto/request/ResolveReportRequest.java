package com.tcs.module.platform.dto.request;

import com.tcs.module.platform.enums.ReportStatus;
import lombok.Data;

@Data
public class ResolveReportRequest {
    private ReportStatus status;
    private String adminNotes;
}
