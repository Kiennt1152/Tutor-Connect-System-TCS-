package com.tcs.module.ai.repository;

import com.tcs.module.ai.entity.AiChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {
    List<AiChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<AiChatSession> findTop20ByOrderByUpdatedAtDesc();
}
