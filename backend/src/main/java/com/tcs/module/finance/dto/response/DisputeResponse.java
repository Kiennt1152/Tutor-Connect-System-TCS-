package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {

    private Long disputeId;
    private DisputeStatus disputeStatus;
    private Boolean escalatedToDispute;
    private Long reportId;
    private ReportStatus reportStatus;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
    private Long escrowId;
    private EscrowStatus escrowStatus;
    private LocalDateTime createdAt;

    public DisputeResponse() {}

    public DisputeResponse(Long disputeId, DisputeStatus disputeStatus, Boolean escalatedToDispute, Long reportId, ReportStatus reportStatus, ReportTargetType targetType, Long targetId, ReportCategory category, String description, String evidenceUrls, Long escrowId, EscrowStatus escrowStatus, LocalDateTime createdAt) {
        this.disputeId = disputeId;
        this.disputeStatus = disputeStatus;
        this.escalatedToDispute = escalatedToDispute;
        this.reportId = reportId;
        this.reportStatus = reportStatus;
        this.targetType = targetType;
        this.targetId = targetId;
        this.category = category;
        this.description = description;
        this.evidenceUrls = evidenceUrls;
        this.escrowId = escrowId;
        this.escrowStatus = escrowStatus;
        this.createdAt = createdAt;
    }

    public static DisputeResponseBuilder builder() {
        return new DisputeResponseBuilder();
    }

    public static class DisputeResponseBuilder {
        private Long disputeId;
        private DisputeStatus disputeStatus;
        private Boolean escalatedToDispute;
        private Long reportId;
        private ReportStatus reportStatus;
        private ReportTargetType targetType;
        private Long targetId;
        private ReportCategory category;
        private String description;
        private String evidenceUrls;
        private Long escrowId;
        private EscrowStatus escrowStatus;
        private LocalDateTime createdAt;

        public DisputeResponseBuilder disputeId(Long disputeId) { this.disputeId = disputeId; return this; }
        public DisputeResponseBuilder disputeStatus(DisputeStatus disputeStatus) { this.disputeStatus = disputeStatus; return this; }
        public DisputeResponseBuilder escalatedToDispute(Boolean escalatedToDispute) { this.escalatedToDispute = escalatedToDispute; return this; }
        public DisputeResponseBuilder reportId(Long reportId) { this.reportId = reportId; return this; }
        public DisputeResponseBuilder reportStatus(ReportStatus reportStatus) { this.reportStatus = reportStatus; return this; }
        public DisputeResponseBuilder targetType(ReportTargetType targetType) { this.targetType = targetType; return this; }
        public DisputeResponseBuilder targetId(Long targetId) { this.targetId = targetId; return this; }
        public DisputeResponseBuilder category(ReportCategory category) { this.category = category; return this; }
        public DisputeResponseBuilder description(String description) { this.description = description; return this; }
        public DisputeResponseBuilder evidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; return this; }
        public DisputeResponseBuilder escrowId(Long escrowId) { this.escrowId = escrowId; return this; }
        public DisputeResponseBuilder escrowStatus(EscrowStatus escrowStatus) { this.escrowStatus = escrowStatus; return this; }
        public DisputeResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DisputeResponse build() {
            return new DisputeResponse(disputeId, disputeStatus, escalatedToDispute, reportId, reportStatus, targetType, targetId, category, description, evidenceUrls, escrowId, escrowStatus, createdAt);
        }
    }
}
