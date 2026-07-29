package com.tcs.module.ai.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiSessionResponse {
    Long sessionId;
    String title;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
