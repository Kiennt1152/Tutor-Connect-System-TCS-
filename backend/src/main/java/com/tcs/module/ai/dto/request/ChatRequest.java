package com.tcs.module.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message must not be empty")
    private String message;
    
    private Long sessionId;
    
    private String userRole;
}
