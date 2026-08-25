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
public class AdminWithdrawalPageResponse {

    private List<AdminWithdrawalResponse> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public AdminWithdrawalPageResponse() {}

    public AdminWithdrawalPageResponse(List<AdminWithdrawalResponse> content, int page, int size, int totalPages, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public static AdminWithdrawalPageResponseBuilder builder() {
        return new AdminWithdrawalPageResponseBuilder();
    }

    public static class AdminWithdrawalPageResponseBuilder {
        private List<AdminWithdrawalResponse> content;
        private int page;
        private int size;
        private int totalPages;
        private long totalElements;

        public AdminWithdrawalPageResponseBuilder content(List<AdminWithdrawalResponse> content) { this.content = content; return this; }
        public AdminWithdrawalPageResponseBuilder page(int page) { this.page = page; return this; }
        public AdminWithdrawalPageResponseBuilder size(int size) { this.size = size; return this; }
        public AdminWithdrawalPageResponseBuilder totalPages(int totalPages) { this.totalPages = totalPages; return this; }
        public AdminWithdrawalPageResponseBuilder totalElements(long totalElements) { this.totalElements = totalElements; return this; }

        public AdminWithdrawalPageResponse build() {
            return new AdminWithdrawalPageResponse(content, page, size, totalPages, totalElements);
        }
    }
}
