package com.tcs.module.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.constants.AiConstants;
import com.tcs.module.ai.entity.AiQueryCache;
import com.tcs.module.ai.repository.AiQueryCacheRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =========================================================================
 * LUỒNG 2: BỘ NHỚ ĐỆM NGỮ NGHĨA SEMANTIC CACHE SERVICE (UC-65)
 * =========================================================================
 * Lưu trữ và phục vụ tức thì (<50ms) các câu hỏi có độ tương đồng ngữ nghĩa Jaccard >= 0.85
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSemanticCacheService {

    private final AiQueryCacheRepository cacheRepository;
    private final TcsSynonymService synonymService;
    private final ObjectMapper objectMapper;

    private static final double SIMILARITY_THRESHOLD = AiConstants.SEMANTIC_CACHE_THRESHOLD;
    private static final int CACHE_TTL_HOURS = AiConstants.SEMANTIC_CACHE_TTL_HOURS;
    private static final int MAX_CACHE_ENTRIES = AiConstants.MAX_CACHE_ENTRIES;

    @jakarta.annotation.PostConstruct
    public void clearStaleCacheOnStartup() {
        try {
            long count = cacheRepository.count();
            if (count > 0) {
                cacheRepository.deleteAll();
                log.info("Cleared {} legacy semantic cache entries on startup for fresh AI pipeline", count);
            }
        } catch (Exception e) {
            log.warn("Failed to clear semantic cache on startup: {}", e.getMessage());
        }
    }

    // Luồng 2 - Bước 0.5: Tra cứu cache theo hàm băm Hash Query hoặc độ tương đồng Embedding
    @Transactional
    public Optional<CachedResponse> get(String query, String userRole, double[] queryEmbedding) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        // 1. Kiểm tra khớp chính xác chuỗi sau chuẩn hóa (Exact Normalized Match)
        String normalized = synonymService != null ? synonymService.normalizeQuery(query) : query.trim().toLowerCase(Locale.ROOT);
        String queryHash = hashQuery(normalized, userRole);
        
        Optional<AiQueryCache> exactMatch = cacheRepository.findByQueryHash(queryHash);
        if (exactMatch.isPresent() && !isExpired(exactMatch.get())) {
            AiQueryCache cache = exactMatch.get();
            if ("PLATFORM_STATS".equals(cache.getSubIntent())) {
                return Optional.empty();
            }
            cache.incrementHit();
            cacheRepository.save(cache);
            log.info("Cache HIT (exact): query='{}', cacheId={}, hits={}", 
                     query, cache.getCacheId(), cache.getHitCount());
            return Optional.of(toCachedResponse(cache));
        }

        // 2. Try semantic similarity search (if embedding provided)
        if (queryEmbedding != null && queryEmbedding.length > 0) {
            List<AiQueryCache> candidates = cacheRepository.findByDomainAndActive(null, LocalDateTime.now());
            
            for (AiQueryCache candidate : candidates) {
                if ("PLATFORM_STATS".equals(candidate.getSubIntent())) {
                    continue;
                }
                if (candidate.getEmbeddingJson() == null || candidate.getEmbeddingJson().isBlank()) continue;
                
                try {
                    double[] cachedEmbedding = objectMapper.readValue(
                        candidate.getEmbeddingJson(), double[].class);
                    
                    double similarity = cosineSimilarity(queryEmbedding, cachedEmbedding);
                    
                    if (similarity >= SIMILARITY_THRESHOLD) {
                        candidate.incrementHit();
                        cacheRepository.save(candidate);
                        log.info("Cache HIT (semantic): query='{}', similarity={}, cacheId={}, hits={}", 
                                 query, similarity, candidate.getCacheId(), candidate.getHitCount());
                        return Optional.of(toCachedResponse(candidate));
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse embedding for cache {}: {}", 
                              candidate.getCacheId(), e.getMessage());
                }
            }
        }

        log.debug("Cache MISS: query='{}'", query);
        return Optional.empty();
    }

    @Transactional
    public Optional<CachedResponse> get(String query, String userRole) {
        return get(query, userRole, (double[]) null);
    }

    /**
     * Store query and response in cache with double[] embedding.
     */
    @Transactional
    public void put(String query, String normalizedQuery, String response, 
                    String intent, String domain, String subIntent, 
                    Double confidenceScore, Integer sourceCount,
                    String referencedTutorIds, String referencedClassIds, String referencedFaqIds,
                    String userRole, double[] embedding) {
        
        if (query == null || response == null) return;

        // Check cache size limit
        long cacheCount = cacheRepository.countTotalCaches();
        if (cacheCount >= MAX_CACHE_ENTRIES) {
            log.debug("Cache size limit reached ({}), skipping cache insert", MAX_CACHE_ENTRIES);
            return;
        }

        String queryHash = hashQuery(normalizedQuery != null ? normalizedQuery : query, userRole);
        
        // Don't cache if already exists
        if (cacheRepository.findByQueryHash(queryHash).isPresent()) {
            return;
        }

        String embeddingJson = null;
        if (embedding != null && embedding.length > 0) {
            try {
                embeddingJson = objectMapper.writeValueAsString(embedding);
            } catch (Exception e) {
                log.warn("Failed to serialize embedding: {}", e.getMessage());
            }
        }

        AiQueryCache cache = AiQueryCache.builder()
            .queryText(query)
            .queryHash(queryHash)
            .normalizedQuery(normalizedQuery)
            .embeddingJson(embeddingJson)
            .responseContent(response)
            .intent(intent)
            .domain(domain)
            .subIntent(subIntent)
            .confidenceScore(confidenceScore)
            .sourceCount(sourceCount)
            .referencedTutorIds(referencedTutorIds)
            .referencedClassIds(referencedClassIds)
            .referencedFaqIds(referencedFaqIds)
            .userRole(userRole)
            .hitCount(0)
            .expiresAt(LocalDateTime.now().plusHours(CACHE_TTL_HOURS))
            .build();

        cacheRepository.save(cache);
        log.debug("Cached query: '{}' with hash={}", query, queryHash);
    }

    @Transactional
    public void put(String query, String normalizedQuery, String response, 
                    String intent, String domain, String subIntent, 
                    Double confidenceScore, Integer sourceCount,
                    String referencedTutorIds, String referencedClassIds, String referencedFaqIds,
                    String userRole) {
        put(query, normalizedQuery, response, intent, domain, subIntent, confidenceScore,
            sourceCount, referencedTutorIds, referencedClassIds, referencedFaqIds, userRole, (double[]) null);
    }

    /**
     * Clear expired cache entries (scheduled distributed lock run).
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(
        name = "aiCacheCleanup",
        lockAtMostFor = "50m",
        lockAtLeastFor = "5m"
    )
    @Transactional
    public void scheduledClearExpiredCaches() {
        clearExpiredCaches();
    }

    /**
     * Clear expired cache entries programmatically.
     */
    @Transactional
    public int clearExpiredCaches() {
        int deleted = cacheRepository.deleteExpiredCaches(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleared {} expired cache entries", deleted);
        }
        return deleted;
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        long totalCaches = cacheRepository.countTotalCaches();
        long cacheHits = cacheRepository.countCacheHits();
        
        List<AiQueryCache> popular = cacheRepository.findActivePopularCaches(LocalDateTime.now());
        int avgHits = popular.isEmpty() ? 0 : 
            (int) popular.stream().mapToInt(AiQueryCache::getHitCount).average().orElse(0);
        
        return new CacheStats(totalCaches, cacheHits, avgHits, 
                              !popular.isEmpty() ? popular.get(0).getHitCount() : 0);
    }

    private boolean isExpired(AiQueryCache cache) {
        return cache.getExpiresAt() != null && cache.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private CachedResponse toCachedResponse(AiQueryCache cache) {
        return new CachedResponse(
            cache.getResponseContent(),
            cache.getIntent(),
            cache.getDomain(),
            cache.getSubIntent(),
            cache.getConfidenceScore(),
            cache.getSourceCount(),
            cache.getReferencedTutorIds(),
            cache.getReferencedClassIds(),
            cache.getReferencedFaqIds()
        );
    }

    private String hashQuery(String query, String userRole) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = (query + "|" + (userRole != null ? userRole : "GUEST")).toLowerCase();
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to hash query: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    private double cosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length == 0 || vec2.length == 0 || vec1.length != vec2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public record CachedResponse(
        String content,
        String intent,
        String domain,
        String subIntent,
        Double confidenceScore,
        Integer sourceCount,
        String referencedTutorIds,
        String referencedClassIds,
        String referencedFaqIds
    ) {}

    public record CacheStats(
        long totalCaches,
        long cacheHits,
        int avgHitsPerCache,
        int maxHits
    ) {}
}
