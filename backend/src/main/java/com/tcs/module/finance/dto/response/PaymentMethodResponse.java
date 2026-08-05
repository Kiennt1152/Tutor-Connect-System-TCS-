package com.tcs.module.finance.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentMethodResponse {

    private Long paymentMethodId;
    private String type;
    private String provider;
    private String bankName;
    private String lastFour;
    private String accountNoMasked;
    private Boolean isDefault;
}
