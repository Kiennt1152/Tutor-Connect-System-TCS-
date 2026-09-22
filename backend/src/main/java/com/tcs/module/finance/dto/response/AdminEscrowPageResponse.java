package com.tcs.module.finance.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEscrowPageResponse {
    private List<AdminEscrowResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
