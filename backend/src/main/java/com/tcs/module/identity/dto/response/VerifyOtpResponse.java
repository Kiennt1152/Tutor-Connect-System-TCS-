package com.tcs.module.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyOtpResponse {

    private String email;
    private String message;
    /** Token chung nhan email da xac thuc, gui kem khi Submit dang ky (BR-05). */
    private String verifiedEmailToken;
    private long tokenExpiresInSeconds;
}
