package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CenterRequestFeePaymentResponse {

    private String requestId;
    private Long feeHoldId;
    private CenterRequestFeeStatus status;
    private BigDecimal amount;
    private String referenceCode;
    private String bankName;
    private String bankBin;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrUrl;
    private Long classId;
    private Long assignmentId;
    private String payoutBankName;
    private String payoutAccountNoMasked;
    private String payoutAccountHolderName;
    private LocalDateTime paidAt;
    private LocalDateTime releasedAt;
    private LocalDateTime refundedAt;

    public String getRequestId() { return requestId; }
    public Long getFeeHoldId() { return feeHoldId; }
    public CenterRequestFeeStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getReferenceCode() { return referenceCode; }
    public String getBankName() { return bankName; }
    public String getBankBin() { return bankBin; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountName() { return accountName; }
    public String getTransferContent() { return transferContent; }
    public String getQrUrl() { return qrUrl; }
    public Long getClassId() { return classId; }
    public Long getAssignmentId() { return assignmentId; }
    public String getPayoutBankName() { return payoutBankName; }
    public String getPayoutAccountNoMasked() { return payoutAccountNoMasked; }
    public String getPayoutAccountHolderName() { return payoutAccountHolderName; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }

    public static CenterRequestFeePaymentResponseBuilder builder() {
        return new CenterRequestFeePaymentResponseBuilder();
    }

    public static class CenterRequestFeePaymentResponseBuilder {
        private String requestId;
        private Long feeHoldId;
        private CenterRequestFeeStatus status;
        private BigDecimal amount;
        private String referenceCode;
        private String bankName;
        private String bankBin;
        private String accountNumber;
        private String accountName;
        private String transferContent;
        private String qrUrl;
        private Long classId;
        private Long assignmentId;
        private String payoutBankName;
        private String payoutAccountNoMasked;
        private String payoutAccountHolderName;
        private LocalDateTime paidAt;
        private LocalDateTime releasedAt;
        private LocalDateTime refundedAt;

        public CenterRequestFeePaymentResponseBuilder requestId(String requestId) { this.requestId = requestId; return this; }
        public CenterRequestFeePaymentResponseBuilder feeHoldId(Long feeHoldId) { this.feeHoldId = feeHoldId; return this; }
        public CenterRequestFeePaymentResponseBuilder status(CenterRequestFeeStatus status) { this.status = status; return this; }
        public CenterRequestFeePaymentResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public CenterRequestFeePaymentResponseBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
        public CenterRequestFeePaymentResponseBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public CenterRequestFeePaymentResponseBuilder bankBin(String bankBin) { this.bankBin = bankBin; return this; }
        public CenterRequestFeePaymentResponseBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public CenterRequestFeePaymentResponseBuilder accountName(String accountName) { this.accountName = accountName; return this; }
        public CenterRequestFeePaymentResponseBuilder transferContent(String transferContent) { this.transferContent = transferContent; return this; }
        public CenterRequestFeePaymentResponseBuilder qrUrl(String qrUrl) { this.qrUrl = qrUrl; return this; }
        public CenterRequestFeePaymentResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public CenterRequestFeePaymentResponseBuilder assignmentId(Long assignmentId) { this.assignmentId = assignmentId; return this; }
        public CenterRequestFeePaymentResponseBuilder payoutBankName(String payoutBankName) { this.payoutBankName = payoutBankName; return this; }
        public CenterRequestFeePaymentResponseBuilder payoutAccountNoMasked(String payoutAccountNoMasked) { this.payoutAccountNoMasked = payoutAccountNoMasked; return this; }
        public CenterRequestFeePaymentResponseBuilder payoutAccountHolderName(String payoutAccountHolderName) { this.payoutAccountHolderName = payoutAccountHolderName; return this; }
        public CenterRequestFeePaymentResponseBuilder paidAt(LocalDateTime paidAt) { this.paidAt = paidAt; return this; }
        public CenterRequestFeePaymentResponseBuilder releasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; return this; }
        public CenterRequestFeePaymentResponseBuilder refundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; return this; }

        public CenterRequestFeePaymentResponse build() {
            return new CenterRequestFeePaymentResponse(requestId, feeHoldId, status, amount, referenceCode, bankName, bankBin, accountNumber, accountName, transferContent, qrUrl, classId, assignmentId, payoutBankName, payoutAccountNoMasked, payoutAccountHolderName, paidAt, releasedAt, refundedAt);
        }
    }
}
