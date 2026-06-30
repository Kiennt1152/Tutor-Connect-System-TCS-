package com.tcs.module.profile.dto.request;

import com.tcs.module.profile.enums.Gender;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bio;
    private Integer experienceYears;
    private java.math.BigDecimal hourlyRate;
    private String companyName;
    private String description;
}
