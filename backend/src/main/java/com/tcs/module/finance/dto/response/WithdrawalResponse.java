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
    private Long paymentMethodId;
    private String bankName;
    private String accountNoMasked;
    private String accountHolderName;
    private String referenceCode;
    private LocalDateTime requestedAt;
    private WalletResponse wallet;
}
