package com.tcs.module.identity.dto.request;

import com.tcs.module.profile.enums.Gender;
import com.tcs.module.profile.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    private String phone;

    @NotNull
    private UserRole role;

    @NotBlank
    private String fullName;

    private Gender gender;

    private String address;

    private String licenseNo;

    private String companyName;

    private Integer experienceYears;

    private BigDecimal hourlyRate;
}
