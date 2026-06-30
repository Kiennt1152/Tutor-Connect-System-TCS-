package com.tcs.module.profile.dto.response;

import com.tcs.module.profile.enums.Gender;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {

    private Long userId;
    private UserRole role;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bio;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private String companyName;
    private String licenseNo;
    private String description;
    private ProfileVerificationStatus verificationStatus;
}
