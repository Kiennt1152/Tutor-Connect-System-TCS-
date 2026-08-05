package com.tcs.module.finance.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentWebhookResponse {

    private String status;
    private String message;
    private String reference;
}
