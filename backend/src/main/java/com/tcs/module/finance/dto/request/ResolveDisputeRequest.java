package com.tcs.module.finance.dto.request;

import com.tcs.module.finance.enums.DisputeStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveDisputeRequest {

    private DisputeStatus status;
    private String resolution;
}
