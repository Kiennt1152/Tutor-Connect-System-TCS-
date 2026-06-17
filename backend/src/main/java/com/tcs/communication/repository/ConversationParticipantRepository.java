package com.tcs.communication.repository;

import com.tcs.communication.entity.ConversationParticipant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
}
