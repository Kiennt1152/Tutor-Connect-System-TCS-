package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
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
}
