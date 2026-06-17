package com.tcs.support.repository;

import com.tcs.support.entity.SupportTicket;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
}
