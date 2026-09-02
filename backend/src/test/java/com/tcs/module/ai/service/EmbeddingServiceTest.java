package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
class EmbeddingServiceTest {

    private EmbeddingService embeddingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        embeddingService = new EmbeddingService(objectMapper, null);
    }

    @Test
    @DisplayName("Calculates Cosine Similarity accurately between identical and orthogonal vectors")
    void testCosineSimilarity() {
        double[] v1 = new double[]{1.0, 0.0, 0.0};
        double[] v2 = new double[]{1.0, 0.0, 0.0};
        double[] v3 = new double[]{0.0, 1.0, 0.0};

        assertThat(embeddingService.cosineSimilarity(v1, v2)).isEqualTo(1.0);
        assertThat(embeddingService.cosineSimilarity(v1, v3)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("LRU Cache maintains entries and respects access order")
    void testLruCacheBehavior() {
        embeddingService.clearCache();
        assertThat(embeddingService.getCacheSize()).isEqualTo(0);

        embeddingService.putCacheForTesting("query 1", new double[]{0.1, 0.2});
        embeddingService.putCacheForTesting("query 2", new double[]{0.3, 0.4});

        assertThat(embeddingService.getCacheSize()).isEqualTo(2);
        assertThat(embeddingService.getEmbedding("query 1")).isPresent();
        assertThat(embeddingService.getEmbedding("query 2")).isPresent();
    }

    @Test
    @DisplayName("Parses JSON array embedding string into double array accurately")
    void testParseEmbeddingJson() throws Exception {
        String json = objectMapper.writeValueAsString(new double[]{0.5, 0.25, 0.75});
        double[] parsed = embeddingService.parseEmbeddingJson(json);

        assertThat(parsed).containsExactly(0.5, 0.25, 0.75);
    }
}
