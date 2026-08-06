package com.tcs.module.finance.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminWithdrawalPageResponse {

    private List<AdminWithdrawalResponse> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
}
