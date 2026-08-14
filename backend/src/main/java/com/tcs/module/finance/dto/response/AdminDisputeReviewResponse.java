package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDisputeReviewResponse {

    private Long disputeId;
    private DisputeStatus disputeStatus;
    private String resolution;
    private LocalDateTime disputeCreatedAt;
    private LocalDateTime disputeUpdatedAt;

    private Long reportId;
    private ReportStatus reportStatus;
    private Long reporterId;
    private String reporterEmail;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
    private List<String> evidenceUrlList;
    private LocalDateTime reportCreatedAt;
    private LocalDateTime reportUpdatedAt;

    private EscrowReviewInfo escrow;
    private RefundReviewInfo latestRefundRequest;
    private ClassReviewInfo tutoringClass;
    private TerminationReviewInfo terminationRequest;
    private SettlementSuggestionInfo settlementSuggestion;
    private List<AuditReviewInfo> auditTrail;

    @Getter
    @Builder
    public static class EscrowReviewInfo {

        private Long escrowId;
        private EscrowStatus status;
        private BigDecimal amount;
        private LocalDateTime depositedAt;
        private LocalDateTime releasedAt;
        private Long assignmentId;
        private Long classStudentId;
        private Long paymentTransactionId;
        private PaymentTransactionType paymentType;
        private PaymentTransactionStatus paymentStatus;
        private String paymentReferenceCode;
        private Long payerUserId;
        private String payerEmail;
        private String refundBankName;
        private String refundAccountNoMasked;
        private String refundAccountHolderName;
    }

    @Getter
    @Builder
    public static class RefundReviewInfo {

        private Long refundId;
        private RefundRequestStatus status;
        private BigDecimal amount;
        private String bankName;
        private String accountNoMasked;
        private String accountHolderName;
        private String refundReferenceCode;
        private String transferStatus;
        private String reason;
        private Long requestedByUserId;
        private String requestedByEmail;
        private LocalDateTime requestedAt;
        private LocalDateTime processedAt;
        private LocalDateTime transferProcessedAt;
    }

    @Getter
    @Builder
    public static class ClassReviewInfo {

        private Long classId;
        private String title;
        private TutoringClassStatus status;
        private Long creatorUserId;
        private String creatorEmail;
        private Long assignmentId;
        private Long tutorUserId;
        private String tutorEmail;
        private String tutorName;
        private Long classStudentId;
        private Long enrolledByUserId;
        private String enrolledByEmail;
        private String studentName;
    }

    @Getter
    @Builder
    public static class TerminationReviewInfo {

        private Long terminationId;
        private ClassTerminationStatus status;
        private Long requestedByUserId;
        private String requestedByEmail;
        private String reason;
        private String bankName;
        private String accountNoMasked;
        private String accountHolderName;
        private LocalDate effectiveDate;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
    }

    @Getter
    @Builder
    public static class SettlementSuggestionInfo {

        private Integer totalSessions;
        private Integer completedSessions;
        private BigDecimal releaseAmount;
        private BigDecimal refundAmount;
        private String reason;
    }

    @Getter
    @Builder
    public static class AuditReviewInfo {

        private Long auditId;
        private Long actorId;
        private String actorEmail;
        private String action;
        private String oldValue;
        private String newValue;
        private LocalDateTime createdAt;
    }
}
