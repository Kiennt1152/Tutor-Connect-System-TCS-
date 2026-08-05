package com.tcs.module.finance.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWithdrawalRequest {

    private BigDecimal amount;
    private Long paymentMethodId;
    private String bankName;
    private String accountNo;
}
