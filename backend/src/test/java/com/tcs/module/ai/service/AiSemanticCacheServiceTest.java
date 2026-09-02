package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiQueryCache;
import com.tcs.module.ai.repository.AiQueryCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
@SpringBootTest
@Transactional
class AiSemanticCacheServiceTest {

    @Autowired
    private AiSemanticCacheService cacheService;

    @Autowired
    private AiQueryCacheRepository cacheRepository;

    @Autowired
    private TcsSynonymService synonymService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        cacheRepository.deleteAll();
    }

    @Test
    void testCachePutAndGet_ExactMatch() {
        String query = "Phí sàn TCS là bao nhiêu?";
        String response = "Phí nền tảng TCS là 10% học phí.";
        
        cacheService.put(query, synonymService.normalizeQuery(query), response, 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.95, 2, null, null, null, "GUEST");

        Optional<AiSemanticCacheService.CachedResponse> cached = 
            cacheService.get(query, "GUEST");

        assertThat(cached).isPresent();
        assertThat(cached.get().content()).isEqualTo(response);
        assertThat(cached.get().domain()).isEqualTo("CATALOG_FAQ");
    }

    @Test
    void testCacheGet_SimilarQueries() {
        String query1 = "Phí sàn TCS là bao nhiêu?";
        String query2 = "Phí nền tảng TCS là bao nhiêu?";
        String response = "Phí nền tảng TCS là 10% học phí.";
        
        cacheService.put(query1, synonymService.normalizeQuery(query1), response, 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.95, 2, null, null, null, "GUEST");

        Optional<AiSemanticCacheService.CachedResponse> cached = 
            cacheService.get(query2, "GUEST");

        assertThat(cached).isPresent();
        assertThat(cached.get().content()).isEqualTo(response);
    }

    @Test
    void testCacheGet_MissForDifferentQuery() {
        String query1 = "Phí sàn TCS là bao nhiêu?";
        String query2 = "Tìm gia sư Toán";
        String response = "Phí nền tảng TCS là 10% học phí.";
        
        cacheService.put(query1, synonymService.normalizeQuery(query1), response, 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.95, 2, null, null, null, "GUEST");

        Optional<AiSemanticCacheService.CachedResponse> cached = 
            cacheService.get(query2, "GUEST");

        assertThat(cached).isEmpty();
    }

    @Test
    void testCacheHitIncrement() {
        String query = "Phí sàn TCS là bao nhiêu?";
        String response = "Phí nền tảng TCS là 10% học phí.";
        
        cacheService.put(query, synonymService.normalizeQuery(query), response, 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.95, 2, null, null, null, "GUEST");

        cacheService.get(query, "GUEST");
        cacheService.get(query, "GUEST");
        cacheService.get(query, "GUEST");

        List<AiQueryCache> caches = cacheRepository.findAll();
        assertThat(caches).hasSize(1);
        assertThat(caches.get(0).getHitCount()).isEqualTo(3);
    }

    @Test
    void testCacheExpiry() {
        AiQueryCache cache = AiQueryCache.builder()
            .queryText("Test query")
            .queryHash("test-hash-123")
            .normalizedQuery("test query")
            .responseContent("Test response")
            .intent("FAQ")
            .domain("CATALOG_FAQ")
            .subIntent("FAQ_SEARCH")
            .confidenceScore(0.95)
            .sourceCount(2)
            .userRole("GUEST")
            .hitCount(0)
            .expiresAt(LocalDateTime.now().minusHours(1))
            .build();
        
        cacheRepository.save(cache);

        Optional<AiSemanticCacheService.CachedResponse> cached = 
            cacheService.get("Test query", "GUEST");

        assertThat(cached).isEmpty();
    }

    @Test
    void testCacheStats() {
        cacheService.put("Query 1", "query 1", "Response 1", 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.95, 2, null, null, null, "GUEST");
        cacheService.put("Query 2", "query 2", "Response 2", 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.90, 3, null, null, null, "GUEST");

        cacheService.get("Query 1", "GUEST");
        cacheService.get("Query 1", "GUEST");

        AiSemanticCacheService.CacheStats stats = cacheService.getStats();

        assertThat(stats.totalCaches()).isEqualTo(2);
        assertThat(stats.cacheHits()).isGreaterThan(0);
        assertThat(stats.maxHits()).isEqualTo(2);
    }

    @Test
    void testCacheClearExpired() {
        AiQueryCache expired = AiQueryCache.builder()
            .queryText("Expired query")
            .queryHash("expired-hash")
            .normalizedQuery("expired query")
            .responseContent("Expired response")
            .intent("FAQ")
            .domain("CATALOG_FAQ")
            .subIntent("FAQ_SEARCH")
            .confidenceScore(0.95)
            .sourceCount(2)
            .userRole("GUEST")
            .hitCount(0)
            .expiresAt(LocalDateTime.now().minusHours(2))
            .build();
        
        AiQueryCache active = AiQueryCache.builder()
            .queryText("Active query")
            .queryHash("active-hash")
            .normalizedQuery("active query")
            .responseContent("Active response")
            .intent("FAQ")
            .domain("CATALOG_FAQ")
            .subIntent("FAQ_SEARCH")
            .confidenceScore(0.95)
            .sourceCount(2)
            .userRole("GUEST")
            .hitCount(0)
            .expiresAt(LocalDateTime.now().plusHours(2))
            .build();

        cacheRepository.save(expired);
        cacheRepository.save(active);

        cacheService.clearExpiredCaches();

        List<AiQueryCache> remaining = cacheRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getQueryText()).isEqualTo("Active query");
    }

    @Test
    void testCachePut_DuplicateQuery_ShouldNotDuplicate() {
        String query = "Phí sàn TCS";
        String response = "10%";
        
        cacheService.put(query, synonymService.normalizeQuery(query), response, 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.95, 2, null, null, null, "GUEST");
        
        cacheService.put(query, synonymService.normalizeQuery(query), "10% học phí", 
                        "FAQ", "CATALOG_FAQ", "FAQ_SEARCH", 
                        0.95, 2, null, null, null, "GUEST");

        List<AiQueryCache> caches = cacheRepository.findAll();
        assertThat(caches).hasSize(1);
    }

    @Test
    void testCacheGet_WithReferencedEntities() {
        String query = "Tìm gia sư Toán";
        String response = "Đây là danh sách gia sư Toán...";
        
        cacheService.put(query, synonymService.normalizeQuery(query), response, 
                        "FIND_TUTOR", "MARKETPLACE_TUTOR", "FIND_TUTOR", 
                        0.88, 3, "1,2,3", null, null, "CLIENT");

        Optional<AiSemanticCacheService.CachedResponse> cached = 
            cacheService.get(query, "CLIENT");

        assertThat(cached).isPresent();
        assertThat(cached.get().referencedTutorIds()).isEqualTo("1,2,3");
    }
}
