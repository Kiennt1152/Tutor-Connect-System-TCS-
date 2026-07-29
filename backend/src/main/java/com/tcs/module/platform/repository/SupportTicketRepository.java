package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            SELECT t FROM SupportTicket t
            WHERE (:status IS NULL OR t.status = :status)
            AND (:category IS NULL OR t.category = :category)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<SupportTicket> search(
            @Param("status") SupportTicketStatus status,
            @Param("category") SupportTicketCategory category,
            @Param("priority") SupportTicketPriority priority,
            @Param("keyword") String keyword,
            Pageable pageable);

    long countByStatusIn(List<SupportTicketStatus> statuses);
    List<SupportTicket> findByStatusInOrderByCreatedAtAsc(List<SupportTicketStatus> statuses);
}
