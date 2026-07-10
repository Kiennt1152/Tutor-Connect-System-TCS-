package com.tcs.module.contract.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateContractRequest {

    private Long assignmentId;

    private Long classStudentId;

    private Long templateId;
}
