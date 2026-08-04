package com.tcs.module.marketplace.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RescheduleDecisionRequest {

    private Boolean approve;

    private String note;
}
