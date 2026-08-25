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
public class AdminEscrowPageResponse {
    private List<AdminEscrowResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public AdminEscrowPageResponse() {}

    public AdminEscrowPageResponse(List<AdminEscrowResponse> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static AdminEscrowPageResponseBuilder builder() {
        return new AdminEscrowPageResponseBuilder();
    }

    public static class AdminEscrowPageResponseBuilder {
        private List<AdminEscrowResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public AdminEscrowPageResponseBuilder content(List<AdminEscrowResponse> content) { this.content = content; return this; }
        public AdminEscrowPageResponseBuilder page(int page) { this.page = page; return this; }
        public AdminEscrowPageResponseBuilder size(int size) { this.size = size; return this; }
        public AdminEscrowPageResponseBuilder totalElements(long totalElements) { this.totalElements = totalElements; return this; }
        public AdminEscrowPageResponseBuilder totalPages(int totalPages) { this.totalPages = totalPages; return this; }

        public AdminEscrowPageResponse build() {
            return new AdminEscrowPageResponse(content, page, size, totalElements, totalPages);
        }
    }
}
