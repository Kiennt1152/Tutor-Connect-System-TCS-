package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
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
public class AdminWithdrawalResponse {

    private Long withdrawalId;
    private Long refundId;
    private String requestType;
    private Long walletId;
    private String requesterEmail;
    private BigDecimal amount;
    private WithdrawalRequestStatus status;
    private Long paymentMethodId;
    private String bankName;
    private String accountNo;
    private String accountNoMasked;
    private String accountHolderName;
    private Long transactionId;
    private PaymentTransactionStatus transactionStatus;
    private String referenceCode;
    private String externalTransactionId;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String failureReason;

    public AdminWithdrawalResponse() {}

    public AdminWithdrawalResponse(Long withdrawalId, Long refundId, String requestType, Long walletId, String requesterEmail, BigDecimal amount, WithdrawalRequestStatus status, Long paymentMethodId, String bankName, String accountNo, String accountNoMasked, String accountHolderName, Long transactionId, PaymentTransactionStatus transactionStatus, String referenceCode, String externalTransactionId, LocalDateTime requestedAt, LocalDateTime processedAt, String failureReason) {
        this.withdrawalId = withdrawalId;
        this.refundId = refundId;
        this.requestType = requestType;
        this.walletId = walletId;
        this.requesterEmail = requesterEmail;
        this.amount = amount;
        this.status = status;
        this.paymentMethodId = paymentMethodId;
        this.bankName = bankName;
        this.accountNo = accountNo;
        this.accountNoMasked = accountNoMasked;
        this.accountHolderName = accountHolderName;
        this.transactionId = transactionId;
        this.transactionStatus = transactionStatus;
        this.referenceCode = referenceCode;
        this.externalTransactionId = externalTransactionId;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.failureReason = failureReason;
    }

    public static AdminWithdrawalResponseBuilder builder() {
        return new AdminWithdrawalResponseBuilder();
    }

    public static class AdminWithdrawalResponseBuilder {
        private Long withdrawalId;
        private Long refundId;
        private String requestType;
        private Long walletId;
        private String requesterEmail;
        private BigDecimal amount;
        private WithdrawalRequestStatus status;
        private Long paymentMethodId;
        private String bankName;
        private String accountNo;
        private String accountNoMasked;
        private String accountHolderName;
        private Long transactionId;
        private PaymentTransactionStatus transactionStatus;
        private String referenceCode;
        private String externalTransactionId;
        private LocalDateTime requestedAt;
        private LocalDateTime processedAt;
        private String failureReason;

        public AdminWithdrawalResponseBuilder withdrawalId(Long withdrawalId) { this.withdrawalId = withdrawalId; return this; }
        public AdminWithdrawalResponseBuilder refundId(Long refundId) { this.refundId = refundId; return this; }
        public AdminWithdrawalResponseBuilder requestType(String requestType) { this.requestType = requestType; return this; }
        public AdminWithdrawalResponseBuilder walletId(Long walletId) { this.walletId = walletId; return this; }
        public AdminWithdrawalResponseBuilder requesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; return this; }
        public AdminWithdrawalResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public AdminWithdrawalResponseBuilder status(WithdrawalRequestStatus status) { this.status = status; return this; }
        public AdminWithdrawalResponseBuilder paymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; return this; }
        public AdminWithdrawalResponseBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public AdminWithdrawalResponseBuilder accountNo(String accountNo) { this.accountNo = accountNo; return this; }
        public AdminWithdrawalResponseBuilder accountNoMasked(String accountNoMasked) { this.accountNoMasked = accountNoMasked; return this; }
        public AdminWithdrawalResponseBuilder accountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; return this; }
        public AdminWithdrawalResponseBuilder transactionId(Long transactionId) { this.transactionId = transactionId; return this; }
        public AdminWithdrawalResponseBuilder transactionStatus(PaymentTransactionStatus transactionStatus) { this.transactionStatus = transactionStatus; return this; }
        public AdminWithdrawalResponseBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
        public AdminWithdrawalResponseBuilder externalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; return this; }
        public AdminWithdrawalResponseBuilder requestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; return this; }
        public AdminWithdrawalResponseBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }
        public AdminWithdrawalResponseBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }

        public AdminWithdrawalResponse build() {
            return new AdminWithdrawalResponse(withdrawalId, refundId, requestType, walletId, requesterEmail, amount, status, paymentMethodId, bankName, accountNo, accountNoMasked, accountHolderName, transactionId, transactionStatus, referenceCode, externalTransactionId, requestedAt, processedAt, failureReason);
        }
    }
}
