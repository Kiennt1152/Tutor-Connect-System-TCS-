package com.tcs.module.ai.controller;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSessionResponse;
import com.tcs.module.ai.service.AiService;
import com.tcs.security.AuthHelper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AuthHelper authHelper;

    @PostMapping("/chat")
    public AiMessageResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiService.chat(request, getOptionalUserId());
    }

    @GetMapping("/sessions")
    public List<AiSessionResponse> getUserSessions() {
        return aiService.getUserSessions(getOptionalUserId());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<AiMessageResponse> getSessionMessages(@PathVariable Long sessionId) {
        return aiService.getSessionMessages(sessionId, getOptionalUserId());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable Long sessionId) {
        aiService.deleteSession(sessionId, getOptionalUserId());
    }

    private Long getOptionalUserId() {
        try {
            return authHelper.currentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
