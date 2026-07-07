package com.tcs.module.finance.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletTransactionsResponse {

    private List<TransactionResponse> transactions;
    private int page;
    private int totalPages;
    private long totalElements;
}
