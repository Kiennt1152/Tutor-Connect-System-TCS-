package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PagePenaltyResponse {
    List<PenaltyResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
