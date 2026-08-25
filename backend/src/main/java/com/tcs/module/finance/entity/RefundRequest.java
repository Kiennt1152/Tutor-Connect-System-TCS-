package com.tcs.module.finance.entity;

import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@NoArgsConstructor
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrow_id")
    private EscrowTransaction escrowTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_request_fee_hold_id")
    private CenterRequestFeeHold centerRequestFeeHold;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "refund_reference_code", length = 100)
    private String refundReferenceCode;

    @Column(name = "transfer_status", length = 20)
    private String transferStatus;

    @Column(name = "transfer_processed_at")
    private LocalDateTime transferProcessedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RefundRequestStatus status = RefundRequestStatus.PENDING;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Long getRefundId() { return refundId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }
    public EscrowTransaction getEscrowTransaction() { return escrowTransaction; }
    public void setEscrowTransaction(EscrowTransaction escrowTransaction) { this.escrowTransaction = escrowTransaction; }
    public CenterRequestFeeHold getCenterRequestFeeHold() { return centerRequestFeeHold; }
    public void setCenterRequestFeeHold(CenterRequestFeeHold centerRequestFeeHold) { this.centerRequestFeeHold = centerRequestFeeHold; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getRefundReferenceCode() { return refundReferenceCode; }
    public void setRefundReferenceCode(String refundReferenceCode) { this.refundReferenceCode = refundReferenceCode; }
    public String getTransferStatus() { return transferStatus; }
    public void setTransferStatus(String transferStatus) { this.transferStatus = transferStatus; }
    public LocalDateTime getTransferProcessedAt() { return transferProcessedAt; }
    public void setTransferProcessedAt(LocalDateTime transferProcessedAt) { this.transferProcessedAt = transferProcessedAt; }
    public RefundRequestStatus getStatus() { return status; }
    public void setStatus(RefundRequestStatus status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
