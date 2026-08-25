package com.tcs.module.finance.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AdminEscrowPageResponse {
    private List<AdminEscrowResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
