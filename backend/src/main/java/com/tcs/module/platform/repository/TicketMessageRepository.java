package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.TicketMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findByTicket_TicketIdOrderByCreatedAtAsc(Long ticketId);
}
