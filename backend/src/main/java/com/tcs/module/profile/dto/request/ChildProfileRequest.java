package com.tcs.module.profile.dto.request;

import com.tcs.module.profile.enums.Gender;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChildProfileRequest {

    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private Long gradeId;
    private String schoolName;
    private String notes;
}
