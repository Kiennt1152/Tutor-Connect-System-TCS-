package com.tcs.module.profile.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianProfileResponse {

    private Long parentUserId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDateTime linkedAt;
}
