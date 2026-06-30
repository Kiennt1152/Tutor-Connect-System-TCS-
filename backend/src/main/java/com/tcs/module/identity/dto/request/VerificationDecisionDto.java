package com.tcs.module.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerificationDecisionDto {

    @NotBlank(message = "Decision is required")
    private String decision;

    @NotBlank(message = "Note is required")
    @Size(max = 1000, message = "Note must be at most 1000 characters")
    private String note;
}
