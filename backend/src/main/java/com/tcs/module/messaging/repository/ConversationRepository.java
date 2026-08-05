package com.tcs.module.messaging.repository;

import com.tcs.module.messaging.entity.Conversation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByContextTypeAndContextId(String contextType, Long contextId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Conversation c JOIN ConversationParticipant cp ON c = cp.conversation WHERE cp.user.userId = :userId ORDER BY c.lastMessageAt DESC")
    java.util.List<Conversation> findConversationsByParticipantUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Conversation c " +
            "JOIN ConversationParticipant cp1 ON c = cp1.conversation AND cp1.user.userId = :userId1 " +
            "JOIN ConversationParticipant cp2 ON c = cp2.conversation AND cp2.user.userId = :userId2 " +
            "WHERE c.type = 'DIRECT'")
    Optional<Conversation> findDirectConversationBetween(
            @org.springframework.data.repository.query.Param("userId1") Long userId1,
            @org.springframework.data.repository.query.Param("userId2") Long userId2);
}
