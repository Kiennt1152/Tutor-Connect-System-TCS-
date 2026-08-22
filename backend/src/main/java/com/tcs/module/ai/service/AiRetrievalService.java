package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiRetrievalService {

    private final AiKnowledgeChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final AiPermissionFilterService permissionFilterService;
    private final ObjectMapper objectMapper;
    private final Bm25Scorer bm25Scorer;

    public AiRetrievalService(AiKnowledgeChunkRepository chunkRepository, EmbeddingService embeddingService, AiPermissionFilterService permissionFilterService, ObjectMapper objectMapper) {
        this(chunkRepository, embeddingService, permissionFilterService, objectMapper, new Bm25Scorer());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiRetrievalService(AiKnowledgeChunkRepository chunkRepository, EmbeddingService embeddingService, AiPermissionFilterService permissionFilterService, ObjectMapper objectMapper, Bm25Scorer bm25Scorer) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.permissionFilterService = permissionFilterService;
        this.objectMapper = objectMapper;
        this.bm25Scorer = bm25Scorer != null ? bm25Scorer : new Bm25Scorer();
    }

    public record RetrievalResult(AiKnowledgeChunk chunk, double cosineSimilarity) {}

    public List<RetrievalResult> retrieve(String query, String userRole, Long userId) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<RetrievalResult> results = new ArrayList<>();
        List<AiKnowledgeChunk> allChunks = chunkRepository.findByActiveTrue();
        if (allChunks.isEmpty()) {
            return List.of();
        }

        // Precompute BM25 Corpus Statistics
        Bm25Scorer.CorpusStats corpusStats = bm25Scorer.buildStats(allChunks);

        // 1. Vector Cosine Similarity Search (Dense Vector Engine)
        Optional<double[]> queryVectorOpt = Optional.empty();
        try {
            queryVectorOpt = embeddingService.getEmbedding(query);
        } catch (Exception e) {
            log.warn("Vector retrieval error, proceeding with hybrid BM25 fallback: {}", e.getMessage());
        }

        boolean hasQueryVector = queryVectorOpt.isPresent();
        double[] queryVector = hasQueryVector ? queryVectorOpt.get() : null;

        for (AiKnowledgeChunk chunk : allChunks) {
            if (!permissionFilterService.canAccess(chunk, userRole, userId)) {
                continue;
            }

            double bm25Score = bm25Scorer.score(query, chunk, corpusStats);
            double vectorScore = 0.0;
            boolean hasVector = false;

            if (hasQueryVector && chunk.getEmbeddingJson() != null && !chunk.getEmbeddingJson().isBlank()) {
                try {
                    double[] chunkVector = objectMapper.readValue(chunk.getEmbeddingJson(), double[].class);
                    vectorScore = cosineSimilarity(queryVector, chunkVector);
                    hasVector = true;
                } catch (Exception ignored) {}
            }

            double hybridScore;
            double threshold;

            if (hasVector) {
                // Dense Vector primary semantic similarity + BM25 keyword enhancement
                hybridScore = Math.max(vectorScore, (vectorScore * 0.70) + (bm25Score * 0.30));

                // Exact keyword match boost
                if (bm25Score >= 0.50) {
                    hybridScore = Math.min(1.0, hybridScore + 0.05);
                }
                threshold = 0.48;
            } else {
                // Fallback for offline/test environments without Gemini API key: Pure BM25
                hybridScore = bm25Score;
                threshold = 0.18;
            }

            if (hybridScore >= threshold) {
                results.add(new RetrievalResult(chunk, hybridScore));
            }
        }

        results.sort((a, b) -> Double.compare(b.cosineSimilarity(), a.cosineSimilarity()));
        return results;
    }

    private double cosineSimilarity(double[] v1, double[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0.0;
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += Math.pow(v1[i], 2);
            norm2 += Math.pow(v2[i], 2);
        }
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
