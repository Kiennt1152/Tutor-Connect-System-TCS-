package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE "
         + "(:actorId IS NULL OR a.actor.userId = :actorId) "
         + "AND (:action IS NULL OR a.action = :action) "
         + "AND (:entityType IS NULL OR a.entityType = :entityType) "
         + "AND (:keyword IS NULL OR LOWER(a.actor.email) LIKE CONCAT('%', :keyword, '%')) "
         + "AND (CAST(:from AS timestamp) IS NULL OR a.createdAt >= :from) "
         + "AND (CAST(:to AS timestamp) IS NULL OR a.createdAt <= :to) "
         + "AND (:actorRole IS NULL "
         + "  OR (:actorRole = 'PLATFORM_ADMIN' AND EXISTS (SELECT 1 FROM PlatformAdmin pa WHERE pa.user.userId = a.actor.userId)) "
         + "  OR (:actorRole = 'TUTOR' AND EXISTS (SELECT 1 FROM Tutor t WHERE t.user.userId = a.actor.userId) "
         + "      AND NOT EXISTS (SELECT 1 FROM PlatformAdmin pa WHERE pa.user.userId = a.actor.userId)) "
         + "  OR (:actorRole = 'TUTOR_CENTER' AND EXISTS (SELECT 1 FROM TutorCenter tc WHERE tc.user.userId = a.actor.userId) "
         + "      AND NOT EXISTS (SELECT 1 FROM PlatformAdmin pa WHERE pa.user.userId = a.actor.userId) "
         + "      AND NOT EXISTS (SELECT 1 FROM Tutor t WHERE t.user.userId = a.actor.userId)) "
         + "  OR (:actorRole = 'CLIENT' AND EXISTS (SELECT 1 FROM Client c WHERE c.user.userId = a.actor.userId) "
         + "      AND NOT EXISTS (SELECT 1 FROM PlatformAdmin pa WHERE pa.user.userId = a.actor.userId) "
         + "      AND NOT EXISTS (SELECT 1 FROM Tutor t WHERE t.user.userId = a.actor.userId) "
         + "      AND NOT EXISTS (SELECT 1 FROM TutorCenter tc WHERE tc.user.userId = a.actor.userId))) "
         + "ORDER BY a.createdAt DESC")
    Page<AuditLog> search(@Param("actorId") Long actorId,
                          @Param("action") String action,
                          @Param("entityType") String entityType,
                          @Param("keyword") String keyword,
                          @Param("actorRole") String actorRole,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
