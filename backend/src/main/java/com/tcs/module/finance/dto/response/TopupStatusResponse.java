package com.tcs.module.finance.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopupStatusResponse {

    private String reference;
    private String status;
    private String message;
    private WalletResponse wallet;
}
