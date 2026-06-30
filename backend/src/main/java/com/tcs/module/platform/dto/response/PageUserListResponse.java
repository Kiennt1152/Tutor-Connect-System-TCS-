package com.tcs.module.platform.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageUserListResponse {
    List<UserListItemResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
