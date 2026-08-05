package com.tcs.module.marketplace.dto.request;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyClassRequest {

    private Map<String, BigDecimal> proposedRates;

    private BigDecimal proposedRate;

    private String coverLetter;
}
