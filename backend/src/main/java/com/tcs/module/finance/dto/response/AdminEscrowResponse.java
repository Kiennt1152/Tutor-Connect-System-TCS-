package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.EscrowStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
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
}
