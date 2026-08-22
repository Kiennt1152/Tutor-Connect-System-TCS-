package com.tcs.module.ai.repository;

import com.tcs.module.ai.entity.AiQueryCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiQueryCacheRepository extends JpaRepository<AiQueryCache, Long> {
    
    Optional<AiQueryCache> findByQueryHash(String queryHash);
    
    List<AiQueryCache> findByNormalizedQuery(String normalizedQuery);
    
    @Query("SELECT c FROM AiQueryCache c WHERE c.expiresAt > :now ORDER BY c.hitCount DESC")
    List<AiQueryCache> findActivePopularCaches(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM AiQueryCache c WHERE c.domain = :domain AND c.expiresAt > :now ORDER BY c.createdAt DESC")
    List<AiQueryCache> findByDomainAndActive(@Param("domain") String domain, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM AiQueryCache c WHERE c.expiresAt <= :now")
    int deleteExpiredCaches(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(c) FROM AiQueryCache c WHERE c.hitCount > 0")
    long countCacheHits();
    
    @Query("SELECT COUNT(c) FROM AiQueryCache c")
    long countTotalCaches();
}
