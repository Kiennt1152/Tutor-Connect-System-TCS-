package com.tcs.module.platform.dto.request;

import com.tcs.module.identity.enums.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserAdminRequest {

    private String displayName;

    private String phone;

    private UserStatus status;
}