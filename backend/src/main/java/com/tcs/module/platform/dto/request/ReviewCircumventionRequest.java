package com.tcs.module.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewCircumventionRequest {
    @NotBlank @Pattern(regexp = "CONFIRMED|DISMISSED") private String status;
    @Size(max = 500) private String note;
}
