package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE "
         + "(:actorId IS NULL OR a.actor.userId = :actorId) "
         + "AND (:action IS NULL OR a.action = :action) "
         + "AND (:entityType IS NULL OR a.entityType = :entityType) "
         + "AND (CAST(:from AS timestamp) IS NULL OR a.createdAt >= :from) "
         + "AND (CAST(:to AS timestamp) IS NULL OR a.createdAt <= :to) "
         + "ORDER BY a.createdAt DESC")
    Page<AuditLog> search(@Param("actorId") Long actorId,
                          @Param("action") String action,
                          @Param("entityType") String entityType,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
