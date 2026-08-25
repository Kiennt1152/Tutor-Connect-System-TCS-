package com.tcs.module.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppealDisputeRequest {

    private String reason;
    private String evidenceUrls;
}
