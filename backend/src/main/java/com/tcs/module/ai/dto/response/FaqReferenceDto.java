package com.tcs.module.ai.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FaqReferenceDto {
    Long faqId;
    String question;
    String answer;
    String category;
}
