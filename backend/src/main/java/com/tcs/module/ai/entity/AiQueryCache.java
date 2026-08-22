package com.tcs.module.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Semantic Query Cache for AI responses.
 * Caches responses for similar queries based on embedding similarity.
 */
@Entity
@Table(name = "ai_query_cache", indexes = {
    @Index(name = "idx_cache_hash", columnList = "query_hash"),
    @Index(name = "idx_cache_created", columnList = "created_at"),
    @Index(name = "idx_cache_hits", columnList = "hit_count")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQueryCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cache_id")
    private Long cacheId;

    @Column(name = "query_text", columnDefinition = "TEXT", nullable = false)
    private String queryText;

    @Column(name = "query_hash", length = 64, nullable = false)
    private String queryHash;

    @Column(name = "normalized_query", columnDefinition = "TEXT")
    private String normalizedQuery;

    @Column(name = "embedding_json", columnDefinition = "JSON")
    private String embeddingJson;

    @Column(name = "response_content", columnDefinition = "TEXT", nullable = false)
    private String responseContent;

    @Column(name = "intent", length = 50)
    private String intent;

    @Column(name = "domain", length = 50)
    private String domain;

    @Column(name = "sub_intent", length = 50)
    private String subIntent;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "source_count")
    private Integer sourceCount;

    @Column(name = "referenced_tutor_ids", columnDefinition = "TEXT")
    private String referencedTutorIds;

    @Column(name = "referenced_class_ids", columnDefinition = "TEXT")
    private String referencedClassIds;

    @Column(name = "referenced_faq_ids", columnDefinition = "TEXT")
    private String referencedFaqIds;

    @Column(name = "user_role", length = 30)
    private String userRole;

    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private Integer hitCount = 0;

    @Column(name = "last_hit_at")
    private LocalDateTime lastHitAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void incrementHit() {
        this.hitCount = (this.hitCount == null ? 0 : this.hitCount) + 1;
        this.lastHitAt = LocalDateTime.now();
    }
}
