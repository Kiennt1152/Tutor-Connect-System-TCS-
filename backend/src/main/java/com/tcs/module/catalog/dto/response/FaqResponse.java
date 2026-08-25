package com.tcs.module.catalog.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
