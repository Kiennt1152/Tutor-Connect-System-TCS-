package com.tcs.module.finance.dto.request;

import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.platform.enums.ReportTargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDisputeRequest {

    private ReportTargetType targetType;
    private Long targetId;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
    private Long escrowId;
    private Long assignmentId;
    private Long classStudentId;
    private RefundPayoutInfo refundPayoutInfo;

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public ReportCategory getCategory() { return category; }
    public void setCategory(ReportCategory category) { this.category = category; }
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
