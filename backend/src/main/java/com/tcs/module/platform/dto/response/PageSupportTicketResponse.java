package com.tcs.module.platform.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageSupportTicketResponse {
    List<SupportTicketListItemResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
