package com.tcs.module.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private Long userId;
    private String email;
    private String status;
    /** CLIENT / TUTOR / TUTOR_CENTER; null neu tai khoan chua co ho so. */
    private String role;
    /** Ten hien thi lay tu ho so (fullName / companyName). */
    private String displayName;
    private String message;
}
