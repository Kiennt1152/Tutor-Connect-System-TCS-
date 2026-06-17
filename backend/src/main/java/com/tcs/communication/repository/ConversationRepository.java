package com.tcs.communication.repository;

import com.tcs.communication.entity.Conversation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
}
