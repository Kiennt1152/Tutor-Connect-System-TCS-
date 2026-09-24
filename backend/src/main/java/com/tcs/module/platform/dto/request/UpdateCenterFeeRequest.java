package com.tcs.module.platform.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCenterFeeRequest {

    @DecimalMin(value = "0.0000", message = "Tỷ lệ phí tùy chỉnh tối thiểu là 0%")
    @DecimalMax(value = "0.5000", message = "Tỷ lệ phí tùy chỉnh tối đa là 50%")
    private BigDecimal customFeeRate;

    private String reason;
}
