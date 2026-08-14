package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRetrievalService {

    private final AiKnowledgeChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final AiPermissionFilterService permissionFilterService;
    private final ObjectMapper objectMapper;

    public record RetrievalResult(AiKnowledgeChunk chunk, double cosineSimilarity) {}

    public List<RetrievalResult> retrieve(String query, String userRole, Long userId) {
        List<RetrievalResult> results = new ArrayList<>();
        List<AiKnowledgeChunk> allChunks = chunkRepository.findByActiveTrue();

        // 1. Try Vector Cosine Similarity Search
        try {
            Optional<double[]> queryVectorOpt = embeddingService.getEmbedding(query);
            if (queryVectorOpt.isPresent()) {
                double[] queryVector = queryVectorOpt.get();
                for (AiKnowledgeChunk chunk : allChunks) {
                    if (!permissionFilterService.canAccess(chunk, userRole, userId)) {
                        continue;
                    }
                    
                    String embeddingJson = chunk.getEmbeddingJson();
                    if (embeddingJson == null || embeddingJson.isBlank()) continue;
                    
                    try {
                        double[] chunkVector = objectMapper.readValue(embeddingJson, double[].class);
                        double similarity = cosineSimilarity(queryVector, chunkVector);
                        if (similarity >= 0.45) { // broad threshold, reranker will refine
                            results.add(new RetrievalResult(chunk, similarity));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Vector retrieval encountered an issue, proceeding with text search fallback: {}", e.getMessage());
        }

        // 2. Text Search Fallback: If vector results are empty or < 2, perform keyword text matching
        if (results.size() < 2 && query != null && !query.isBlank()) {
            List<String> queryKeywords = extractKeywords(query);
            Set<Long> existingChunkIds = results.stream().map(r -> r.chunk().getChunkId()).collect(Collectors.toSet());

            for (AiKnowledgeChunk chunk : allChunks) {
                if (existingChunkIds.contains(chunk.getChunkId()) || !permissionFilterService.canAccess(chunk, userRole, userId)) {
                    continue;
                }

                double textMatchScore = calculateKeywordScore(queryKeywords, chunk);
                if (textMatchScore >= 0.25) {
                    results.add(new RetrievalResult(chunk, textMatchScore));
                }
            }
        }
        
        results.sort((a, b) -> Double.compare(b.cosineSimilarity(), a.cosineSimilarity()));
        return results;
    }

    private List<String> extractKeywords(String text) {
        if (text == null) return List.of();
        String cleaned = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9à-ỹ\\s]", " ")
                .trim();
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(w -> w.length() >= 2)
                .toList();
    }

    private double calculateKeywordScore(List<String> keywords, AiKnowledgeChunk chunk) {
        if (keywords.isEmpty()) return 0.0;
        
        String title = chunk.getTitle() != null ? chunk.getTitle().toLowerCase(Locale.ROOT) : "";
        String content = chunk.getContent() != null ? chunk.getContent().toLowerCase(Locale.ROOT) : "";
        String metadata = chunk.getMetadataJson() != null ? chunk.getMetadataJson().toLowerCase(Locale.ROOT) : "";
        
        int matchCount = 0;
        int titleBonus = 0;
        
        for (String kw : keywords) {
            boolean matched = false;
            if (title.contains(kw)) {
                matched = true;
                titleBonus++;
            }
            if (content.contains(kw)) {
                matched = true;
            }
            if (metadata.contains(kw)) {
                matched = true;
            }
            if (matched) {
                matchCount++;
            }
        }

        double ratio = (double) matchCount / keywords.size();
        double score = ratio * 0.70 + (titleBonus > 0 ? 0.20 : 0.0);
        return Math.min(0.90, score);
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
