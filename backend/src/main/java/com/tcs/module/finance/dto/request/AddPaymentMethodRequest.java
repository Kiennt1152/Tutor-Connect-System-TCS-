package com.tcs.module.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPaymentMethodRequest {

    /** Ten ngan hang, vd "MB Bank", "Vietcombank". */
    private String bankName;

    /** So tai khoan ngan hang. */
    private String accountNo;

    /** Ten chu tai khoan. */
    private String accountName;
}
