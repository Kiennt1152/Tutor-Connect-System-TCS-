package com.tcs.module.finance.dto.request;

import com.tcs.module.platform.enums.ReportCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassIssueRequest {

    private Long classId;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
    private Long escrowId;
    private Long assignmentId;
    private Long classStudentId;
}
