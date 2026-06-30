package com.tcs.module.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResponse {

    private String email;
    private String message;
}
