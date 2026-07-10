package com.tcs.module.contract.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OtpSentResponse {

    private String maskedEmail;
    private String message;
}
