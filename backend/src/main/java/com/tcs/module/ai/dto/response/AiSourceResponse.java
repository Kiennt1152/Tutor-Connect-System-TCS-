package com.tcs.module.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSourceResponse {
    private String sourceId;
    private String sourceType;
    private String title;
    private String snippet;
    private Double similarity;
    private Double finalScore;
    private String visibility;
}
