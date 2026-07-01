package com.tcs.module.profile.dto.response;

import com.tcs.module.profile.enums.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChildProfileResponse {

    private Long childProfileId;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private Long gradeId;
    private String gradeName;
    private String schoolName;
    private String notes;
    private LocalDateTime createdAt;

    /** true nếu hồ sơ con trùng khớp tài khoản CLIENT học sinh đã đăng ký. */
    private boolean linkedToUserAccount;

    private Long childUserId;
    private String childEmail;
}
