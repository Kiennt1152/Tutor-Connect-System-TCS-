package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminWithdrawalResponse {

    private Long withdrawalId;
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
}
