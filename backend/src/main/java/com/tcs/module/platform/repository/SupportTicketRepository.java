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

    // =========================================================================
    // LUỒNG 3: NGƯỜI DÙNG TẠO & XEM TICKET CÁ NHÂN (UC-65)
    // =========================================================================
    List<SupportTicket> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // =========================================================================
    // LUỒNG 4: ADMIN LỌC & TÌM KIẾM TICKET ĐA TIÊU CHÍ (UC-66)
    // =========================================================================
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

    // =========================================================================
    // LUỒNG 7: QUÉT ĐỊNH KỲ TICKET QUÁ HẠN CAM KẾT SLA (JOB-11)
    // =========================================================================
    // Lọc các ticket chưa đóng có hạn chót dueAt < now và chưa bị đánh dấu slaBreached
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE t.status NOT IN (:excludedStatuses)
            AND t.dueAt IS NOT NULL
            AND t.dueAt < :now
            AND (t.slaBreached IS NULL OR t.slaBreached = false)
            """)
    List<SupportTicket> findBreachedCandidateTickets(
            @Param("excludedStatuses") List<SupportTicketStatus> excludedStatuses,
            @Param("now") java.time.LocalDateTime now);

    long countByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    // =========================================================================
    // LUỒNG 6: GOM CỤM TICKET SINH BẢN NHÁP FAQ (UC-67)
    // =========================================================================
    List<SupportTicket> findByCreatedAtAfter(java.time.LocalDateTime since);
}
