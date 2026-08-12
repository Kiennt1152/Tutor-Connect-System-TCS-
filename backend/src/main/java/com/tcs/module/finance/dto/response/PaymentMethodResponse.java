package com.tcs.module.finance.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentMethodResponse {

    private Long paymentMethodId;
    private String type;
    private String provider;
    private String bankName;
    private String accountHolderName;
    private String lastFour;
    private String accountNoMasked;
    private Boolean isDefault;
    private LocalDateTime verifiedAt;
    private LocalDateTime cooldownUntil;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
