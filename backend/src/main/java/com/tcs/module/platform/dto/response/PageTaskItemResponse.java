package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PageTaskItemResponse {
    List<TaskItemResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
