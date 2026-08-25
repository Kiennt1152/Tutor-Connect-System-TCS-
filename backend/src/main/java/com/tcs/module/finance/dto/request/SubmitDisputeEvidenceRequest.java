package com.tcs.module.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitDisputeEvidenceRequest {

    private String evidenceUrls;
    private String note;
}
