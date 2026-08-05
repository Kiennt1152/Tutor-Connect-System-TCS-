package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSessionResponse;
import java.util.List;

public interface AiService {
    AiMessageResponse chat(ChatRequest request, Long userId);
    List<AiSessionResponse> getUserSessions(Long userId);
    List<AiMessageResponse> getSessionMessages(Long sessionId, Long userId);
    void deleteSession(Long sessionId, Long userId);
}
