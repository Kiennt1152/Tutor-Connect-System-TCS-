package com.tcs.module.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMethodRequest {

    private String bankName;
    private String accountNo;
}
