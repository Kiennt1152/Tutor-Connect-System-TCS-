package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefundRequestResponse {

    private Long refundId;
    private Long escrowId;
    private EscrowStatus escrowStatus;
    private Long requesterId;
    private String requesterEmail;
    private Long classId;
    private String classTitle;
    private Long assignmentId;
    private Long classStudentId;
    private BigDecimal escrowAmount;
    private BigDecimal amount;
    private String bankName;
    private String accountNoMasked;
    private String accountHolderName;
    private String refundReferenceCode;
    private String transferStatus;
    private RefundRequestStatus status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private LocalDateTime transferProcessedAt;
}
