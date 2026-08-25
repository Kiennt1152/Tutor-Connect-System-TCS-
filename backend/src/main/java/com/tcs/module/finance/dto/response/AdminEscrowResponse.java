package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.EscrowStatus;
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
public class AdminEscrowResponse {
    private Long escrowId;
    private Long paymentId;
    private String referenceCode;
    private BigDecimal amount;
    private EscrowStatus status;
    private Long payerUserId;
    private String payerEmail;
    private Long beneficiaryUserId;
    private String beneficiaryEmail;
    private Long assignmentId;
    private Long classStudentId;
    private LocalDateTime depositedAt;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminEscrowResponse() {}

    public AdminEscrowResponse(Long escrowId, Long paymentId, String referenceCode, BigDecimal amount, EscrowStatus status, Long payerUserId, String payerEmail, Long beneficiaryUserId, String beneficiaryEmail, Long assignmentId, Long classStudentId, LocalDateTime depositedAt, LocalDateTime releasedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.escrowId = escrowId;
        this.paymentId = paymentId;
        this.referenceCode = referenceCode;
        this.amount = amount;
        this.status = status;
        this.payerUserId = payerUserId;
        this.payerEmail = payerEmail;
        this.beneficiaryUserId = beneficiaryUserId;
        this.beneficiaryEmail = beneficiaryEmail;
        this.assignmentId = assignmentId;
        this.classStudentId = classStudentId;
        this.depositedAt = depositedAt;
        this.releasedAt = releasedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AdminEscrowResponseBuilder builder() {
        return new AdminEscrowResponseBuilder();
    }

    public static class AdminEscrowResponseBuilder {
        private Long escrowId;
        private Long paymentId;
        private String referenceCode;
        private BigDecimal amount;
        private EscrowStatus status;
        private Long payerUserId;
        private String payerEmail;
        private Long beneficiaryUserId;
        private String beneficiaryEmail;
        private Long assignmentId;
        private Long classStudentId;
        private LocalDateTime depositedAt;
        private LocalDateTime releasedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AdminEscrowResponseBuilder escrowId(Long escrowId) { this.escrowId = escrowId; return this; }
        public AdminEscrowResponseBuilder paymentId(Long paymentId) { this.paymentId = paymentId; return this; }
        public AdminEscrowResponseBuilder referenceCode(String referenceCode) { this.referenceCode = referenceCode; return this; }
        public AdminEscrowResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public AdminEscrowResponseBuilder status(EscrowStatus status) { this.status = status; return this; }
        public AdminEscrowResponseBuilder payerUserId(Long payerUserId) { this.payerUserId = payerUserId; return this; }
        public AdminEscrowResponseBuilder payerEmail(String payerEmail) { this.payerEmail = payerEmail; return this; }
        public AdminEscrowResponseBuilder beneficiaryUserId(Long beneficiaryUserId) { this.beneficiaryUserId = beneficiaryUserId; return this; }
        public AdminEscrowResponseBuilder beneficiaryEmail(String beneficiaryEmail) { this.beneficiaryEmail = beneficiaryEmail; return this; }
        public AdminEscrowResponseBuilder assignmentId(Long assignmentId) { this.assignmentId = assignmentId; return this; }
        public AdminEscrowResponseBuilder classStudentId(Long classStudentId) { this.classStudentId = classStudentId; return this; }
        public AdminEscrowResponseBuilder depositedAt(LocalDateTime depositedAt) { this.depositedAt = depositedAt; return this; }
        public AdminEscrowResponseBuilder releasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; return this; }
        public AdminEscrowResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AdminEscrowResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AdminEscrowResponse build() {
            return new AdminEscrowResponse(escrowId, paymentId, referenceCode, amount, status, payerUserId, payerEmail, beneficiaryUserId, beneficiaryEmail, assignmentId, classStudentId, depositedAt, releasedAt, createdAt, updatedAt);
        }
    }
}
