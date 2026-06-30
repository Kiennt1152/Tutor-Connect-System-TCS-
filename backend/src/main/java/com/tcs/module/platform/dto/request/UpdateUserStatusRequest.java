package com.tcs.module.platform.dto.request;

import com.tcs.module.identity.enums.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserStatusRequest {
    private UserStatus status;
}
