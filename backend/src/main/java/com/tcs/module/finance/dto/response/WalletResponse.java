package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.WalletStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletResponse {

    private Long walletId;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private WalletStatus status;
    private LocalDateTime updatedAt;
}
