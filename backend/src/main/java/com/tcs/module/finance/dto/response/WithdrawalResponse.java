package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawalResponse {

    private Long withdrawalId;
    private BigDecimal amount;
    private WithdrawalRequestStatus status;
    private String bankName;
    private String accountNo;
    private String accountName;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String failureReason;

    /** true = rut truc tiep (tutor/center), false = cho admin duyet (client). */
    private boolean direct;
}
