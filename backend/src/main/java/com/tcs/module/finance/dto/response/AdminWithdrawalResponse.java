package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Ban ghi rut tien cho man hinh duyet cua PLATFORM_ADMIN. */
@Getter
@Builder
public class AdminWithdrawalResponse {

    private Long withdrawalId;
    private Long userId;
    private String userEmail;
    private BigDecimal amount;
    private WithdrawalRequestStatus status;
    private String bankName;
    private String accountNo;
    private String accountName;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String failureReason;
}
