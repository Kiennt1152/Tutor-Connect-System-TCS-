package com.tcs.module.messaging.repository;

import com.tcs.module.messaging.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    java.util.Optional<ConversationParticipant> findByConversation_ConversationIdAndUser_UserId(Long conversationId, Long userId);
    boolean existsByConversation_ConversationIdAndUser_UserId(Long conversationId, Long userId);
    java.util.List<ConversationParticipant> findByConversation_ConversationId(Long conversationId);
}
