package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.Announcement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("""
            SELECT a FROM Announcement a
            WHERE a.published = TRUE
            AND (a.audience = 'ALL' OR a.audience = :audience)
            AND (a.startsAt IS NULL OR a.startsAt <= CURRENT_TIMESTAMP)
            AND (a.endsAt IS NULL OR a.endsAt >= CURRENT_TIMESTAMP)
            ORDER BY a.createdAt DESC
            """)
    List<Announcement> findActiveForAudience(@Param("audience") String audience);

    List<Announcement> findAllByOrderByCreatedAtDesc();
}
