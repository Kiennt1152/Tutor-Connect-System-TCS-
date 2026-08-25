package com.tcs.module.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopupSessionResponse {

    private String reference;
    private BigDecimal amount;
    private String status;
    private String qrUrl;
    private String bankName;
    private String bankBin;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private LocalDateTime expiresAt;
    private long expiresAtMillis;
}
