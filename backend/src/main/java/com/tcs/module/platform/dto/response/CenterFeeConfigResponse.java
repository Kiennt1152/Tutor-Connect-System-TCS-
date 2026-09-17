package com.tcs.module.platform.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterFeeConfigResponse {
    private Long centerId;
    private Long userId;
    private String companyName;
    private String licenseNo;
    private String email;
    private String phone;
    private String verificationStatus;
    private BigDecimal customFeeRate;
    private BigDecimal effectiveFeeRate;
    private boolean custom;
    private String effectiveFeeRatePercent;
    private BigDecimal defaultPlatformFeeRate;
}
