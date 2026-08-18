package com.tcs.module.platform.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedirectDisputeRequest {

    private Long targetClassId;

    private String notes;
}
