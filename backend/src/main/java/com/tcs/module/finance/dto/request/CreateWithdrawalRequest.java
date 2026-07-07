package com.tcs.module.finance.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWithdrawalRequest {

    private BigDecimal amount;

    /** Tai khoan ngan hang nhan tien (da luu trong payment_methods). */
    private Long paymentMethodId;
}
