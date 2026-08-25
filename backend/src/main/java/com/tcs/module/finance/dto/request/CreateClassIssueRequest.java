package com.tcs.module.finance.dto.request;

import com.tcs.module.finance.enums.ClassIssueRequestedAction;
import com.tcs.module.finance.enums.ClassIssueType;
import com.tcs.module.finance.dto.RefundPayoutInfo;
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
    private RefundPayoutInfo refundPayoutInfo;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public ClassIssueType getIssueType() { return issueType; }
    public void setIssueType(ClassIssueType issueType) { this.issueType = issueType; }
    public ReportCategory getCategory() { return category; }
    public void setCategory(ReportCategory category) { this.category = category; }
    public String getLessonRef() { return lessonRef; }
    public void setLessonRef(String lessonRef) { this.lessonRef = lessonRef; }
    public LocalDate getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDate occurredAt) { this.occurredAt = occurredAt; }
    public ClassIssueRequestedAction getRequestedAction() { return requestedAction; }
    public void setRequestedAction(ClassIssueRequestedAction requestedAction) { this.requestedAction = requestedAction; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }
    public Long getEscrowId() { return escrowId; }
    public void setEscrowId(Long escrowId) { this.escrowId = escrowId; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public Long getClassStudentId() { return classStudentId; }
    public void setClassStudentId(Long classStudentId) { this.classStudentId = classStudentId; }
    public RefundPayoutInfo getRefundPayoutInfo() { return refundPayoutInfo; }
    public void setRefundPayoutInfo(RefundPayoutInfo refundPayoutInfo) { this.refundPayoutInfo = refundPayoutInfo; }
}
