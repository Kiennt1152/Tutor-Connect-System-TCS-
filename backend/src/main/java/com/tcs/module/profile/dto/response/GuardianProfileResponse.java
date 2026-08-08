package com.tcs.module.profile.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardianProfileResponse {

    private Long parentUserId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDateTime linkedAt;
}
