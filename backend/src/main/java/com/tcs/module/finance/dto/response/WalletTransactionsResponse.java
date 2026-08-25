package com.tcs.module.finance.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionsResponse {

    private List<TransactionResponse> transactions;
    private int page;
    private int totalPages;
    private long totalElements;

    public WalletTransactionsResponse() {}

    public WalletTransactionsResponse(List<TransactionResponse> transactions, int page, int totalPages, long totalElements) {
        this.transactions = transactions;
        this.page = page;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public static WalletTransactionsResponseBuilder builder() {
        return new WalletTransactionsResponseBuilder();
    }

    public static class WalletTransactionsResponseBuilder {
        private List<TransactionResponse> transactions;
        private int page;
        private int totalPages;
        private long totalElements;

        public WalletTransactionsResponseBuilder transactions(List<TransactionResponse> transactions) { this.transactions = transactions; return this; }
        public WalletTransactionsResponseBuilder page(int page) { this.page = page; return this; }
        public WalletTransactionsResponseBuilder totalPages(int totalPages) { this.totalPages = totalPages; return this; }
        public WalletTransactionsResponseBuilder totalElements(long totalElements) { this.totalElements = totalElements; return this; }

        public WalletTransactionsResponse build() {
            return new WalletTransactionsResponse(transactions, page, totalPages, totalElements);
        }
    }
}
