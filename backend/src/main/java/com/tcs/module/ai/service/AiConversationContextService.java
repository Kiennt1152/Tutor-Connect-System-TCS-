package com.tcs.module.ai.service;

import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiConversationContextService {

    private final AiChatMessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<AiChatMessage> getHistory(Long sessionId) {
        return messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);
    }
}
