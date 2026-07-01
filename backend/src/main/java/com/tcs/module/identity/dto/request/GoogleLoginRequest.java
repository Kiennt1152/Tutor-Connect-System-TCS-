package com.tcs.module.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** UC: dang nhap bang Google. `credential` la Google ID token (JWT) do Google Identity Services tra ve. */
@Getter
@Setter
public class GoogleLoginRequest {

    @NotBlank(message = "Thiếu Google credential")
    private String credential;
}
