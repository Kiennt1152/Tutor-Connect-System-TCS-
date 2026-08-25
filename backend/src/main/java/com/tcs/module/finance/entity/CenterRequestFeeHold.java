package com.tcs.module.finance.entity;

import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "center_request_fee_holds")
@Getter
@Setter
@NoArgsConstructor
public class CenterRequestFeeHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_hold_id")
    private Long feeHoldId;

    @Column(name = "request_id", length = 64, nullable = false, unique = true)
    private String requestId;

    @Column(name = "client_user_id", nullable = false)
    private Long clientUserId;

    @Column(name = "center_user_id", nullable = false)
    private Long centerUserId;

    @Column(name = "center_name", length = 150)
    private String centerName;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false, unique = true)
    private PaymentTransaction paymentTransaction;

    @Column(name = "projected_escrow_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal projectedEscrowAmount;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reference_code", length = 100, nullable = false, unique = true)
    private String referenceCode;

    @Column(name = "payout_bank_name", length = 100, nullable = false)
    private String payoutBankName;

    @Column(name = "payout_account_no", length = 50, nullable = false)
    private String payoutAccountNo;

    @Column(name = "payout_account_holder_name", length = 150, nullable = false)
    private String payoutAccountHolderName;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private CenterRequestFeeStatus status = CenterRequestFeeStatus.PENDING_PAYMENT;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getFeeHoldId() { return feeHoldId; }
    public void setFeeHoldId(Long feeHoldId) { this.feeHoldId = feeHoldId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getClientUserId() { return clientUserId; }
    public void setClientUserId(Long clientUserId) { this.clientUserId = clientUserId; }
    public Long getCenterUserId() { return centerUserId; }
    public void setCenterUserId(Long centerUserId) { this.centerUserId = centerUserId; }
    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }
    public PaymentTransaction getPaymentTransaction() { return paymentTransaction; }
    public void setPaymentTransaction(PaymentTransaction paymentTransaction) { this.paymentTransaction = paymentTransaction; }
    public BigDecimal getProjectedEscrowAmount() { return projectedEscrowAmount; }
    public void setProjectedEscrowAmount(BigDecimal projectedEscrowAmount) { this.projectedEscrowAmount = projectedEscrowAmount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getPayoutBankName() { return payoutBankName; }
    public void setPayoutBankName(String payoutBankName) { this.payoutBankName = payoutBankName; }
    public String getPayoutAccountNo() { return payoutAccountNo; }
    public void setPayoutAccountNo(String payoutAccountNo) { this.payoutAccountNo = payoutAccountNo; }
    public String getPayoutAccountHolderName() { return payoutAccountHolderName; }
    public void setPayoutAccountHolderName(String payoutAccountHolderName) { this.payoutAccountHolderName = payoutAccountHolderName; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public CenterRequestFeeStatus getStatus() { return status; }
    public void setStatus(CenterRequestFeeStatus status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
