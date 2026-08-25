package com.tcs.module.contract.dto.response;

import com.tcs.module.contract.enums.ContractSourceType;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {

    private Long contractId;
    private String contractNo;
    private ContractStatus status;
    private ContractSourceType sourceType;

    private Long assignmentId;
    private Long classId;
    private Long classStudentId;
    /** BF-03: != null nghĩa là hợp đồng tuyển dụng/hợp tác (không có lớp/số buổi/học phí). */
    private Long recruitmentApplicationId;

    private Long clientId;
    private String clientName;
    private String clientEmail;

    private Long tutorId;
    private String tutorName;
    private String tutorEmail;

    private Long centerId;
    private String centerName;
    private String centerEmail;

    private Long templateId;
    private String templateName;
    private String termsSummary;
    /** Văn bản hợp đồng đầy đủ (Quốc hiệu + BÊN A + BÊN B + điều khoản) để hiển thị/ký. */
    private String documentText;
    private String contractFileUrl;

    private boolean hasAllSignatures;
    private int signedCount;
    private int requiredSignatures;

    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Legacy contract pages still read these denormalized class fields.
    private String classTitle;
    private String classType;
    private BigDecimal tuitionFee;
    private BigDecimal totalTuitionAmount;
    private BigDecimal escrowAmount;
    private String lessonMode;
    private Integer numberOfSessions;

    private PartyInfo tutor;
    private PartyInfo client;
    private PartyInfo center;
    private EscrowPaymentInfo escrowPayment;
    private RefundPayoutInfoView refundPayoutInfo;
    private Integer totalSessions;
    private Integer completedSessions;
    @Builder.Default
    private boolean refundAllowed = true;
    private String refundBlockedReason;

    public static ContractResponseBuilder builder() {
        return new ContractResponseBuilder();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartyInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String phone;

        public static PartyInfoBuilder builder() { return new PartyInfoBuilder(); }

        public static class PartyInfoBuilder {
            private Long userId;
            private String fullName;
            private String email;
            private String phone;

            public PartyInfoBuilder userId(Long userId) { this.userId = userId; return this; }
            public PartyInfoBuilder fullName(String fullName) { this.fullName = fullName; return this; }
            public PartyInfoBuilder email(String email) { this.email = email; return this; }
            public PartyInfoBuilder phone(String phone) { this.phone = phone; return this; }
            public PartyInfo build() {
                return new PartyInfo(userId, fullName, email, phone);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EscrowPaymentInfo {
        private Long escrowId;
        private EscrowStatus escrowStatus;
        private Long paymentTransactionId;
        private PaymentTransactionStatus paymentStatus;
        private BigDecimal amount;
        private String referenceCode;
        private String bankName;
        private String bankBin;
        private String accountNumber;
        private String accountName;
        private String transferContent;
        private String qrUrl;
        private LocalDateTime depositedAt;
        private LocalDateTime processedAt;

        public static EscrowPaymentInfoBuilder builder() { return new EscrowPaymentInfoBuilder(); }

        public static class EscrowPaymentInfoBuilder {
            private Long escrowId;
            private EscrowStatus escrowStatus;
            private Long paymentTransactionId;
            private PaymentTransactionStatus paymentStatus;
            private BigDecimal amount;
            private String referenceCode;
            private String bankName;
            private String bankBin;
            private String accountNumber;
            private String accountName;
            private String transferContent;
            private String qrUrl;
            private LocalDateTime depositedAt;
            private LocalDateTime processedAt;

            public EscrowPaymentInfoBuilder escrowId(Long escrowId) { this.escrowId = escrowId; return this; }
            public EscrowPaymentInfoBuilder escrowStatus(EscrowStatus escrowStatus) { this.escrowStatus = escrowStatus; return this; }
            public EscrowPaymentInfoBuilder paymentTransactionId(Long paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; return this; }
            public EscrowPaymentInfoBuilder paymentStatus(PaymentTransactionStatus paymentStatus) { this.paymentStatus = paymentStatus; return this; }
            public EscrowPaymentInfoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
            public EscrowPaymentInfoBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
            public EscrowPaymentInfoBuilder bankName(String bankName) { this.bankName = bankName; return this; }
            public EscrowPaymentInfoBuilder bankBin(String bankBin) { this.bankBin = bankBin; return this; }
            public EscrowPaymentInfoBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
            public EscrowPaymentInfoBuilder accountName(String accountName) { this.accountName = accountName; return this; }
            public EscrowPaymentInfoBuilder transferContent(String transferContent) { this.transferContent = transferContent; return this; }
            public EscrowPaymentInfoBuilder qrUrl(String qrUrl) { this.qrUrl = qrUrl; return this; }
            public EscrowPaymentInfoBuilder depositedAt(LocalDateTime depositedAt) { this.depositedAt = depositedAt; return this; }
            public EscrowPaymentInfoBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
            public EscrowPaymentInfo build() {
                return new EscrowPaymentInfo(escrowId, escrowStatus, paymentTransactionId, paymentStatus, amount, referenceCode, bankName, bankBin, accountNumber, accountName, transferContent, qrUrl, depositedAt, processedAt);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundPayoutInfoView {
        private String bankName;
        private String accountNo;
        private String accountNoMasked;
        private String accountHolderName;
        private boolean suggested;

        public static RefundPayoutInfoViewBuilder builder() { return new RefundPayoutInfoViewBuilder(); }

        public static class RefundPayoutInfoViewBuilder {
            private String bankName;
            private String accountNo;
            private String accountNoMasked;
            private String accountHolderName;
            private boolean suggested;

            public RefundPayoutInfoViewBuilder bankName(String bankName) { this.bankName = bankName; return this; }
            public RefundPayoutInfoViewBuilder accountNo(String accountNo) { this.accountNo = accountNo; return this; }
            public RefundPayoutInfoViewBuilder accountNoMasked(String accountNoMasked) { this.accountNoMasked = accountNoMasked; return this; }
            public RefundPayoutInfoViewBuilder accountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; return this; }
            public RefundPayoutInfoViewBuilder suggested(boolean suggested) { this.suggested = suggested; return this; }
            public RefundPayoutInfoView build() {
                return new RefundPayoutInfoView(bankName, accountNo, accountNoMasked, accountHolderName, suggested);
            }
        }
    }

    public static class ContractResponseBuilder {
        private Long contractId;
        private String contractNo;
        private ContractStatus status;
        private ContractSourceType sourceType;
        private Long assignmentId;
        private Long classId;
        private Long classStudentId;
        private Long recruitmentApplicationId;
        private Long clientId;
        private String clientName;
        private String clientEmail;
        private Long tutorId;
        private String tutorName;
        private String tutorEmail;
        private Long centerId;
        private String centerName;
        private String centerEmail;
        private Long templateId;
        private String templateName;
        private String termsSummary;
        private String documentText;
        private String contractFileUrl;
        private boolean hasAllSignatures;
        private int signedCount;
        private int requiredSignatures;
        private LocalDateTime signedAt;
        private LocalDateTime expiresAt;
        private LocalDateTime confirmedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String classTitle;
        private String classType;
        private BigDecimal tuitionFee;
        private BigDecimal totalTuitionAmount;
        private BigDecimal escrowAmount;
        private String lessonMode;
        private Integer numberOfSessions;
        private PartyInfo tutor;
        private PartyInfo client;
        private PartyInfo center;
        private EscrowPaymentInfo escrowPayment;
        private RefundPayoutInfoView refundPayoutInfo;
        private Integer totalSessions;
        private Integer completedSessions;
        private boolean refundAllowed = true;
        private String refundBlockedReason;

        public ContractResponseBuilder contractId(Long contractId) { this.contractId = contractId; return this; }
        public ContractResponseBuilder contractNo(String contractNo) { this.contractNo = contractNo; return this; }
        public ContractResponseBuilder status(ContractStatus status) { this.status = status; return this; }
        public ContractResponseBuilder sourceType(ContractSourceType sourceType) { this.sourceType = sourceType; return this; }
        public ContractResponseBuilder assignmentId(Long assignmentId) { this.assignmentId = assignmentId; return this; }
        public ContractResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public ContractResponseBuilder classStudentId(Long classStudentId) { this.classStudentId = classStudentId; return this; }
        public ContractResponseBuilder recruitmentApplicationId(Long recruitmentApplicationId) { this.recruitmentApplicationId = recruitmentApplicationId; return this; }
        public ContractResponseBuilder clientId(Long clientId) { this.clientId = clientId; return this; }
        public ContractResponseBuilder clientName(String clientName) { this.clientName = clientName; return this; }
        public ContractResponseBuilder clientEmail(String clientEmail) { this.clientEmail = clientEmail; return this; }
        public ContractResponseBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
        public ContractResponseBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
        public ContractResponseBuilder tutorEmail(String tutorEmail) { this.tutorEmail = tutorEmail; return this; }
        public ContractResponseBuilder centerId(Long centerId) { this.centerId = centerId; return this; }
        public ContractResponseBuilder centerName(String centerName) { this.centerName = centerName; return this; }
        public ContractResponseBuilder centerEmail(String centerEmail) { this.centerEmail = centerEmail; return this; }
        public ContractResponseBuilder templateId(Long templateId) { this.templateId = templateId; return this; }
        public ContractResponseBuilder templateName(String templateName) { this.templateName = templateName; return this; }
        public ContractResponseBuilder termsSummary(String termsSummary) { this.termsSummary = termsSummary; return this; }
        public ContractResponseBuilder documentText(String documentText) { this.documentText = documentText; return this; }
        public ContractResponseBuilder contractFileUrl(String contractFileUrl) { this.contractFileUrl = contractFileUrl; return this; }
        public ContractResponseBuilder hasAllSignatures(boolean hasAllSignatures) { this.hasAllSignatures = hasAllSignatures; return this; }
        public ContractResponseBuilder signedCount(int signedCount) { this.signedCount = signedCount; return this; }
        public ContractResponseBuilder requiredSignatures(int requiredSignatures) { this.requiredSignatures = requiredSignatures; return this; }
        public ContractResponseBuilder signedAt(LocalDateTime signedAt) { this.signedAt = signedAt; return this; }
        public ContractResponseBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public ContractResponseBuilder confirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; return this; }
        public ContractResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ContractResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ContractResponseBuilder classTitle(String classTitle) { this.classTitle = classTitle; return this; }
        public ContractResponseBuilder classType(String classType) { this.classType = classType; return this; }
        public ContractResponseBuilder tuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; return this; }
        public ContractResponseBuilder totalTuitionAmount(BigDecimal totalTuitionAmount) { this.totalTuitionAmount = totalTuitionAmount; return this; }
        public ContractResponseBuilder escrowAmount(BigDecimal escrowAmount) { this.escrowAmount = escrowAmount; return this; }
        public ContractResponseBuilder lessonMode(String lessonMode) { this.lessonMode = lessonMode; return this; }
        public ContractResponseBuilder numberOfSessions(Integer numberOfSessions) { this.numberOfSessions = numberOfSessions; return this; }
        public ContractResponseBuilder tutor(PartyInfo tutor) { this.tutor = tutor; return this; }
        public ContractResponseBuilder client(PartyInfo client) { this.client = client; return this; }
        public ContractResponseBuilder center(PartyInfo center) { this.center = center; return this; }
        public ContractResponseBuilder escrowPayment(EscrowPaymentInfo escrowPayment) { this.escrowPayment = escrowPayment; return this; }
        public ContractResponseBuilder refundPayoutInfo(RefundPayoutInfoView refundPayoutInfo) { this.refundPayoutInfo = refundPayoutInfo; return this; }
        public ContractResponseBuilder totalSessions(Integer totalSessions) { this.totalSessions = totalSessions; return this; }
        public ContractResponseBuilder completedSessions(Integer completedSessions) { this.completedSessions = completedSessions; return this; }
        public ContractResponseBuilder refundAllowed(boolean refundAllowed) { this.refundAllowed = refundAllowed; return this; }
        public ContractResponseBuilder refundBlockedReason(String refundBlockedReason) { this.refundBlockedReason = refundBlockedReason; return this; }

        public ContractResponse build() {
            return new ContractResponse(contractId, contractNo, status, sourceType, assignmentId, classId, classStudentId, recruitmentApplicationId, clientId, clientName, clientEmail, tutorId, tutorName, tutorEmail, centerId, centerName, centerEmail, templateId, templateName, termsSummary, documentText, contractFileUrl, hasAllSignatures, signedCount, requiredSignatures, signedAt, expiresAt, confirmedAt, createdAt, updatedAt, classTitle, classType, tuitionFee, totalTuitionAmount, escrowAmount, lessonMode, numberOfSessions, tutor, client, center, escrowPayment, refundPayoutInfo, totalSessions, completedSessions, refundAllowed, refundBlockedReason);
        }
    }
}
