package com.tcs.module.finance.dto.request;

import com.tcs.module.finance.enums.ClassIssueRequestedAction;
import com.tcs.module.finance.enums.ClassIssueType;
import com.tcs.module.platform.enums.ReportCategory;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassIssueRequest {

    private Long classId;
    private ClassIssueType issueType;
    private ReportCategory category;
    private String lessonRef;
    private LocalDate occurredAt;
    private ClassIssueRequestedAction requestedAction;
    private String description;
    private String evidenceUrls;
    private Long escrowId;
    private Long assignmentId;
    private Long classStudentId;
}
