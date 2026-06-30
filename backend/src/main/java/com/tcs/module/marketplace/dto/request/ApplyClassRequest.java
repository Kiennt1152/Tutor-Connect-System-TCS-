package com.tcs.module.marketplace.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyClassRequest {

    private BigDecimal proposedRate;
    private String coverLetter;
}
