package com.tcs.module.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordResetOtpResponse {

    private String email;
    private String message;
    private long otpExpiresInSeconds;
    private long resendCooldownSeconds;
    private String resetToken;
    private long resetTokenExpiresInSeconds;
}
