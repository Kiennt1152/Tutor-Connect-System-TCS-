package com.tcs.module.catalog.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaqResponse {

    private Long faqId;
    private String question;
    private String answer;
    private String category;
    private Integer sortOrder;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
