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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public AdminDisputeReviewResponse() {}

    public AdminDisputeReviewResponse(Long disputeId, DisputeStatus disputeStatus, String resolution, LocalDateTime disputeCreatedAt, LocalDateTime disputeUpdatedAt, Long reportId, ReportStatus reportStatus, Long reporterId, String reporterEmail, ReportTargetType targetType, Long targetId, ReportCategory category, String description, String evidenceUrls, List<String> evidenceUrlList, LocalDateTime reportCreatedAt, LocalDateTime reportUpdatedAt, EscrowReviewInfo escrow, RefundReviewInfo latestRefundRequest, ClassReviewInfo tutoringClass, TerminationReviewInfo terminationRequest, SettlementSuggestionInfo settlementSuggestion, List<AuditReviewInfo> auditTrail) {
        this.disputeId = disputeId;
        this.disputeStatus = disputeStatus;
        this.resolution = resolution;
        this.disputeCreatedAt = disputeCreatedAt;
        this.disputeUpdatedAt = disputeUpdatedAt;
        this.reportId = reportId;
        this.reportStatus = reportStatus;
        this.reporterId = reporterId;
        this.reporterEmail = reporterEmail;
        this.targetType = targetType;
        this.targetId = targetId;
        this.category = category;
        this.description = description;
        this.evidenceUrls = evidenceUrls;
        this.evidenceUrlList = evidenceUrlList;
        this.reportCreatedAt = reportCreatedAt;
        this.reportUpdatedAt = reportUpdatedAt;
        this.escrow = escrow;
        this.latestRefundRequest = latestRefundRequest;
        this.tutoringClass = tutoringClass;
        this.terminationRequest = terminationRequest;
        this.settlementSuggestion = settlementSuggestion;
        this.auditTrail = auditTrail;
    }

    public static AdminDisputeReviewResponseBuilder builder() {
        return new AdminDisputeReviewResponseBuilder();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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

        public EscrowReviewInfo() {}

        public EscrowReviewInfo(Long escrowId, EscrowStatus status, BigDecimal amount, LocalDateTime depositedAt, LocalDateTime releasedAt, Long assignmentId, Long classStudentId, Long paymentTransactionId, PaymentTransactionType paymentType, PaymentTransactionStatus paymentStatus, String paymentReferenceCode, Long payerUserId, String payerEmail, String refundBankName, String refundAccountNoMasked, String refundAccountHolderName) {
            this.escrowId = escrowId;
            this.status = status;
            this.amount = amount;
            this.depositedAt = depositedAt;
            this.releasedAt = releasedAt;
            this.assignmentId = assignmentId;
            this.classStudentId = classStudentId;
            this.paymentTransactionId = paymentTransactionId;
            this.paymentType = paymentType;
            this.paymentStatus = paymentStatus;
            this.paymentReferenceCode = paymentReferenceCode;
            this.payerUserId = payerUserId;
            this.payerEmail = payerEmail;
            this.refundBankName = refundBankName;
            this.refundAccountNoMasked = refundAccountNoMasked;
            this.refundAccountHolderName = refundAccountHolderName;
        }

        public static EscrowReviewInfoBuilder builder() { return new EscrowReviewInfoBuilder(); }

        public static class EscrowReviewInfoBuilder {
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

            public EscrowReviewInfoBuilder escrowId(Long escrowId) { this.escrowId = escrowId; return this; }
            public EscrowReviewInfoBuilder status(EscrowStatus status) { this.status = status; return this; }
            public EscrowReviewInfoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
            public EscrowReviewInfoBuilder depositedAt(LocalDateTime depositedAt) { this.depositedAt = depositedAt; return this; }
            public EscrowReviewInfoBuilder releasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; return this; }
            public EscrowReviewInfoBuilder assignmentId(Long assignmentId) { this.assignmentId = assignmentId; return this; }
            public EscrowReviewInfoBuilder classStudentId(Long classStudentId) { this.classStudentId = classStudentId; return this; }
            public EscrowReviewInfoBuilder paymentTransactionId(Long paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; return this; }
            public EscrowReviewInfoBuilder paymentType(PaymentTransactionType paymentType) { this.paymentType = paymentType; return this; }
            public EscrowReviewInfoBuilder paymentStatus(PaymentTransactionStatus paymentStatus) { this.paymentStatus = paymentStatus; return this; }
            public EscrowReviewInfoBuilder paymentReferenceCode(String paymentReferenceCode) { this.referenceCode(paymentReferenceCode); return this; }
            public EscrowReviewInfoBuilder referenceCode(String paymentReferenceCode) { this.paymentReferenceCode = paymentReferenceCode; return this; }
            public EscrowReviewInfoBuilder payerUserId(Long payerUserId) { this.payerUserId = payerUserId; return this; }
            public EscrowReviewInfoBuilder payerEmail(String payerEmail) { this.payerEmail = payerEmail; return this; }
            public EscrowReviewInfoBuilder refundBankName(String refundBankName) { this.refundBankName = refundBankName; return this; }
            public EscrowReviewInfoBuilder refundAccountNoMasked(String refundAccountNoMasked) { this.refundAccountNoMasked = refundAccountNoMasked; return this; }
            public EscrowReviewInfoBuilder refundAccountHolderName(String refundAccountHolderName) { this.refundAccountHolderName = refundAccountHolderName; return this; }
            public EscrowReviewInfo build() {
                return new EscrowReviewInfo(escrowId, status, amount, depositedAt, releasedAt, assignmentId, classStudentId, paymentTransactionId, paymentType, paymentStatus, paymentReferenceCode, payerUserId, payerEmail, refundBankName, refundAccountNoMasked, refundAccountHolderName);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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

        public RefundReviewInfo() {}

        public RefundReviewInfo(Long refundId, RefundRequestStatus status, BigDecimal amount, String bankName, String accountNoMasked, String accountHolderName, String refundReferenceCode, String transferStatus, String reason, Long requestedByUserId, String requestedByEmail, LocalDateTime requestedAt, LocalDateTime processedAt, LocalDateTime transferProcessedAt) {
            this.refundId = refundId;
            this.status = status;
            this.amount = amount;
            this.bankName = bankName;
            this.accountNoMasked = accountNoMasked;
            this.accountHolderName = accountHolderName;
            this.refundReferenceCode = refundReferenceCode;
            this.transferStatus = transferStatus;
            this.reason = reason;
            this.requestedByUserId = requestedByUserId;
            this.requestedByEmail = requestedByEmail;
            this.requestedAt = requestedAt;
            this.processedAt = processedAt;
            this.transferProcessedAt = transferProcessedAt;
        }

        public static RefundReviewInfoBuilder builder() { return new RefundReviewInfoBuilder(); }

        public static class RefundReviewInfoBuilder {
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

            public RefundReviewInfoBuilder refundId(Long refundId) { this.refundId = refundId; return this; }
            public RefundReviewInfoBuilder status(RefundRequestStatus status) { this.status = status; return this; }
            public RefundReviewInfoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
            public RefundReviewInfoBuilder bankName(String bankName) { this.bankName = bankName; return this; }
            public RefundReviewInfoBuilder accountNoMasked(String accountNoMasked) { this.accountNoMasked = accountNoMasked; return this; }
            public RefundReviewInfoBuilder accountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; return this; }
            public RefundReviewInfoBuilder refundReferenceCode(String refundReferenceCode) { this.refundReferenceCode = refundReferenceCode; return this; }
            public RefundReviewInfoBuilder transferStatus(String transferStatus) { this.transferStatus = transferStatus; return this; }
            public RefundReviewInfoBuilder reason(String reason) { this.reason = reason; return this; }
            public RefundReviewInfoBuilder requestedByUserId(Long requestedByUserId) { this.requestedByUserId = requestedByUserId; return this; }
            public RefundReviewInfoBuilder requestedByEmail(String requestedByEmail) { this.requestedByEmail = requestedByEmail; return this; }
            public RefundReviewInfoBuilder requestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; return this; }
            public RefundReviewInfoBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
            public RefundReviewInfoBuilder transferProcessedAt(LocalDateTime transferProcessedAt) { this.transferProcessedAt = transferProcessedAt; return this; }
            public RefundReviewInfo build() {
                return new RefundReviewInfo(refundId, status, amount, bankName, accountNoMasked, accountHolderName, refundReferenceCode, transferStatus, reason, requestedByUserId, requestedByEmail, requestedAt, processedAt, transferProcessedAt);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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

        public ClassReviewInfo() {}

        public ClassReviewInfo(Long classId, String title, TutoringClassStatus status, Long creatorUserId, String creatorEmail, Long assignmentId, Long tutorUserId, String tutorEmail, String tutorName, Long classStudentId, Long enrolledByUserId, String enrolledByEmail, String studentName) {
            this.classId = classId;
            this.title = title;
            this.status = status;
            this.creatorUserId = creatorUserId;
            this.creatorEmail = creatorEmail;
            this.assignmentId = assignmentId;
            this.tutorUserId = tutorUserId;
            this.tutorEmail = tutorEmail;
            this.tutorName = tutorName;
            this.classStudentId = classStudentId;
            this.enrolledByUserId = enrolledByUserId;
            this.enrolledByEmail = enrolledByEmail;
            this.studentName = studentName;
        }

        public static ClassReviewInfoBuilder builder() { return new ClassReviewInfoBuilder(); }

        public static class ClassReviewInfoBuilder {
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

            public ClassReviewInfoBuilder classId(Long classId) { this.classId = classId; return this; }
            public ClassReviewInfoBuilder title(String title) { this.title = title; return this; }
            public ClassReviewInfoBuilder status(TutoringClassStatus status) { this.status = status; return this; }
            public ClassReviewInfoBuilder creatorUserId(Long creatorUserId) { this.creatorUserId = creatorUserId; return this; }
            public ClassReviewInfoBuilder creatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; return this; }
            public ClassReviewInfoBuilder assignmentId(Long assignmentId) { this.assignmentId = assignmentId; return this; }
            public ClassReviewInfoBuilder tutorUserId(Long tutorUserId) { this.tutorUserId = tutorUserId; return this; }
            public ClassReviewInfoBuilder tutorEmail(String tutorEmail) { this.tutorEmail = tutorEmail; return this; }
            public ClassReviewInfoBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
            public ClassReviewInfoBuilder classStudentId(Long classStudentId) { this.classStudentId = classStudentId; return this; }
            public ClassReviewInfoBuilder enrolledByUserId(Long enrolledByUserId) { this.enrolledByUserId = enrolledByUserId; return this; }
            public ClassReviewInfoBuilder enrolledByEmail(String enrolledByEmail) { this.enrolledByEmail = enrolledByEmail; return this; }
            public ClassReviewInfoBuilder studentName(String studentName) { this.studentName = studentName; return this; }
            public ClassReviewInfo build() {
                return new ClassReviewInfo(classId, title, status, creatorUserId, creatorEmail, assignmentId, tutorUserId, tutorEmail, tutorName, classStudentId, enrolledByUserId, enrolledByEmail, studentName);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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

        public TerminationReviewInfo() {}

        public TerminationReviewInfo(Long terminationId, ClassTerminationStatus status, Long requestedByUserId, String requestedByEmail, String reason, String bankName, String accountNoMasked, String accountHolderName, LocalDate effectiveDate, LocalDateTime createdAt, LocalDateTime processedAt) {
            this.terminationId = terminationId;
            this.status = status;
            this.requestedByUserId = requestedByUserId;
            this.requestedByEmail = requestedByEmail;
            this.reason = reason;
            this.bankName = bankName;
            this.accountNoMasked = accountNoMasked;
            this.accountHolderName = accountHolderName;
            this.effectiveDate = effectiveDate;
            this.createdAt = createdAt;
            this.processedAt = processedAt;
        }

        public static TerminationReviewInfoBuilder builder() { return new TerminationReviewInfoBuilder(); }

        public static class TerminationReviewInfoBuilder {
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

            public TerminationReviewInfoBuilder terminationId(Long terminationId) { this.terminationId = terminationId; return this; }
            public TerminationReviewInfoBuilder status(ClassTerminationStatus status) { this.status = status; return this; }
            public TerminationReviewInfoBuilder requestedByUserId(Long requestedByUserId) { this.requestedByUserId = requestedByUserId; return this; }
            public TerminationReviewInfoBuilder requestedByEmail(String requestedByEmail) { this.requestedByEmail = requestedByEmail; return this; }
            public TerminationReviewInfoBuilder reason(String reason) { this.reason = reason; return this; }
            public TerminationReviewInfoBuilder bankName(String bankName) { this.bankName = bankName; return this; }
            public TerminationReviewInfoBuilder accountNoMasked(String accountNoMasked) { this.accountNoMasked = accountNoMasked; return this; }
            public TerminationReviewInfoBuilder accountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; return this; }
            public TerminationReviewInfoBuilder effectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; return this; }
            public TerminationReviewInfoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public TerminationReviewInfoBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
            public TerminationReviewInfo build() {
                return new TerminationReviewInfo(terminationId, status, requestedByUserId, requestedByEmail, reason, bankName, accountNoMasked, accountHolderName, effectiveDate, createdAt, processedAt);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementSuggestionInfo {
        private Integer totalSessions;
        private Integer completedSessions;
        private BigDecimal releaseAmount;
        private BigDecimal refundAmount;
        private String reason;

        public SettlementSuggestionInfo() {}

        public SettlementSuggestionInfo(Integer totalSessions, Integer completedSessions, BigDecimal releaseAmount, BigDecimal refundAmount, String reason) {
            this.totalSessions = totalSessions;
            this.completedSessions = completedSessions;
            this.releaseAmount = releaseAmount;
            this.refundAmount = refundAmount;
            this.reason = reason;
        }

        public static SettlementSuggestionInfoBuilder builder() { return new SettlementSuggestionInfoBuilder(); }

        public static class SettlementSuggestionInfoBuilder {
            private Integer totalSessions;
            private Integer completedSessions;
            private BigDecimal releaseAmount;
            private BigDecimal refundAmount;
            private String reason;

            public SettlementSuggestionInfoBuilder totalSessions(Integer totalSessions) { this.totalSessions = totalSessions; return this; }
            public SettlementSuggestionInfoBuilder completedSessions(Integer completedSessions) { this.completedSessions = completedSessions; return this; }
            public SettlementSuggestionInfoBuilder releaseAmount(BigDecimal releaseAmount) { this.releaseAmount = releaseAmount; return this; }
            public SettlementSuggestionInfoBuilder refundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; return this; }
            public SettlementSuggestionInfoBuilder reason(String reason) { this.reason = reason; return this; }
            public SettlementSuggestionInfo build() {
                return new SettlementSuggestionInfo(totalSessions, completedSessions, releaseAmount, refundAmount, reason);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditReviewInfo {
        private Long auditId;
        private Long actorId;
        private String actorEmail;
        private String action;
        private String oldValue;
        private String newValue;
        private LocalDateTime createdAt;

        public AuditReviewInfo() {}

        public AuditReviewInfo(Long auditId, Long actorId, String actorEmail, String action, String oldValue, String newValue, LocalDateTime createdAt) {
            this.auditId = auditId;
            this.actorId = actorId;
            this.actorEmail = actorEmail;
            this.action = action;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.createdAt = createdAt;
        }

        public static AuditReviewInfoBuilder builder() { return new AuditReviewInfoBuilder(); }

        public static class AuditReviewInfoBuilder {
            private Long auditId;
            private Long actorId;
            private String actorEmail;
            private String action;
            private String oldValue;
            private String newValue;
            private LocalDateTime createdAt;

            public AuditReviewInfoBuilder auditId(Long auditId) { this.auditId = auditId; return this; }
            public AuditReviewInfoBuilder actorId(Long actorId) { this.actorId = actorId; return this; }
            public AuditReviewInfoBuilder actorEmail(String actorEmail) { this.actorEmail = actorEmail; return this; }
            public AuditReviewInfoBuilder action(String action) { this.action = action; return this; }
            public AuditReviewInfoBuilder oldValue(String oldValue) { this.oldValue = oldValue; return this; }
            public AuditReviewInfoBuilder newValue(String newValue) { this.newValue = newValue; return this; }
            public AuditReviewInfoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
            public AuditReviewInfo build() {
                return new AuditReviewInfo(auditId, actorId, actorEmail, action, oldValue, newValue, createdAt);
            }
        }
    }

    public static class AdminDisputeReviewResponseBuilder {
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

        public AdminDisputeReviewResponseBuilder disputeId(Long disputeId) { this.disputeId = disputeId; return this; }
        public AdminDisputeReviewResponseBuilder disputeStatus(DisputeStatus disputeStatus) { this.disputeStatus = disputeStatus; return this; }
        public AdminDisputeReviewResponseBuilder resolution(String resolution) { this.resolution = resolution; return this; }
        public AdminDisputeReviewResponseBuilder disputeCreatedAt(LocalDateTime disputeCreatedAt) { this.disputeCreatedAt = disputeCreatedAt; return this; }
        public AdminDisputeReviewResponseBuilder disputeUpdatedAt(LocalDateTime disputeUpdatedAt) { this.disputeUpdatedAt = disputeUpdatedAt; return this; }
        public AdminDisputeReviewResponseBuilder reportId(Long reportId) { this.reportId = reportId; return this; }
        public AdminDisputeReviewResponseBuilder reportStatus(ReportStatus reportStatus) { this.reportStatus = reportStatus; return this; }
        public AdminDisputeReviewResponseBuilder reporterId(Long reporterId) { this.reporterId = reporterId; return this; }
        public AdminDisputeReviewResponseBuilder reporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; return this; }
        public AdminDisputeReviewResponseBuilder targetType(ReportTargetType targetType) { this.targetType = targetType; return this; }
        public AdminDisputeReviewResponseBuilder targetId(Long targetId) { this.targetId = targetId; return this; }
        public AdminDisputeReviewResponseBuilder category(ReportCategory category) { this.category = category; return this; }
        public AdminDisputeReviewResponseBuilder description(String description) { this.description = description; return this; }
        public AdminDisputeReviewResponseBuilder evidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; return this; }
        public AdminDisputeReviewResponseBuilder evidenceUrlList(List<String> evidenceUrlList) { this.evidenceUrlList = evidenceUrlList; return this; }
        public AdminDisputeReviewResponseBuilder reportCreatedAt(LocalDateTime reportCreatedAt) { this.reportCreatedAt = reportCreatedAt; return this; }
        public AdminDisputeReviewResponseBuilder reportUpdatedAt(LocalDateTime reportUpdatedAt) { this.reportUpdatedAt = reportUpdatedAt; return this; }
        public AdminDisputeReviewResponseBuilder escrow(EscrowReviewInfo escrow) { this.escrow = escrow; return this; }
        public AdminDisputeReviewResponseBuilder latestRefundRequest(RefundReviewInfo latestRefundRequest) { this.latestRefundRequest = latestRefundRequest; return this; }
        public AdminDisputeReviewResponseBuilder tutoringClass(ClassReviewInfo tutoringClass) { this.tutoringClass = tutoringClass; return this; }
        public AdminDisputeReviewResponseBuilder terminationRequest(TerminationReviewInfo terminationRequest) { this.terminationRequest = terminationRequest; return this; }
        public AdminDisputeReviewResponseBuilder settlementSuggestion(SettlementSuggestionInfo settlementSuggestion) { this.settlementSuggestion = settlementSuggestion; return this; }
        public AdminDisputeReviewResponseBuilder auditTrail(List<AuditReviewInfo> auditTrail) { this.auditTrail = auditTrail; return this; }

        public AdminDisputeReviewResponse build() {
            return new AdminDisputeReviewResponse(disputeId, disputeStatus, resolution, disputeCreatedAt, disputeUpdatedAt, reportId, reportStatus, reporterId, reporterEmail, targetType, targetId, category, description, evidenceUrls, evidenceUrlList, reportCreatedAt, reportUpdatedAt, escrow, latestRefundRequest, tutoringClass, terminationRequest, settlementSuggestion, auditTrail);
        }
    }
}
