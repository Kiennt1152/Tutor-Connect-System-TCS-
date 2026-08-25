package com.tcs.module.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message must not be empty")
    private String message;
    
    private Long sessionId;
    
    private String userRole;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
}
