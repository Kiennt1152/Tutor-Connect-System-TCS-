package com.tcs.module.ai.repository;

import com.tcs.module.ai.entity.AiChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findBySession_SessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySession_SessionId(Long sessionId);
}
