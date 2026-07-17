package com.tcs.module.messaging.repository;

import com.tcs.module.messaging.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation_ConversationIdOrderBySentAtAsc(Long conversationId);
}
