package com.tcs.module.contract.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignContractRequest {

    private String otpCode;

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
