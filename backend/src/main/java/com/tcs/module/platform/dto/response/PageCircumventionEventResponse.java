package com.tcs.module.platform.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class PageCircumventionEventResponse {
    private List<CircumventionEventResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
