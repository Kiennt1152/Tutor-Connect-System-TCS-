package com.tcs.module.marketplace.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassTerminationRequest {

    private Long assignmentId;

    private String reason;

    private LocalDate effectiveDate;
}
