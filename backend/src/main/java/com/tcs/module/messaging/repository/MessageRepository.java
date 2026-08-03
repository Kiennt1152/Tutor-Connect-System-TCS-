package com.tcs.module.messaging.repository;

import com.tcs.module.messaging.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation_ConversationIdOrderBySentAtAsc(Long conversationId);
    org.springframework.data.domain.Page<Message> findByConversation_ConversationIdOrderBySentAtDesc(Long conversationId, org.springframework.data.domain.Pageable pageable);
    long countByConversation_ConversationIdAndSentAtAfterAndSender_UserIdNot(Long conversationId, java.time.LocalDateTime date, Long senderId);
}
