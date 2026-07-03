package com.tcs.module.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** UC: dang nhap bang Google. `accessToken` la OAuth2 access token do Google Identity Services tra ve. */
@Getter
@Setter
public class GoogleLoginRequest {

    @NotBlank(message = "Thiếu Google access token")
    private String accessToken;
}
