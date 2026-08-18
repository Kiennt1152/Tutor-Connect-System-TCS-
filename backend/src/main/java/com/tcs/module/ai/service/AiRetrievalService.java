package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import java.util.*;
import java.util.regex.Pattern;
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
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<RetrievalResult> results = new ArrayList<>();
        List<AiKnowledgeChunk> allChunks = chunkRepository.findByActiveTrue();

        // 1. Vector Cosine Similarity Search (Primary Dense Vector Engine)
        Optional<double[]> queryVectorOpt = Optional.empty();
        try {
            queryVectorOpt = embeddingService.getEmbedding(query);
        } catch (Exception e) {
            log.warn("Vector retrieval error, proceeding with fallback: {}", e.getMessage());
        }

        boolean hasQueryVector = queryVectorOpt.isPresent();
        double[] queryVector = hasQueryVector ? queryVectorOpt.get() : null;

        for (AiKnowledgeChunk chunk : allChunks) {
            if (!permissionFilterService.canAccess(chunk, userRole, userId)) {
                continue;
            }

            double score = 0.0;
            if (hasQueryVector && chunk.getEmbeddingJson() != null && !chunk.getEmbeddingJson().isBlank()) {
                try {
                    double[] chunkVector = objectMapper.readValue(chunk.getEmbeddingJson(), double[].class);
                    score = cosineSimilarity(queryVector, chunkVector);
                } catch (Exception ignored) {}
            } else {
                // Fallback for offline/test environments without Gemini API key
                score = calculateKeywordScore(extractKeywords(query), chunk);
            }

            // Relevance Gating: 0.50 for dense vectors, 0.25 for keyword fallback
            double threshold = (hasQueryVector && chunk.getEmbeddingJson() != null && !chunk.getEmbeddingJson().isBlank()) ? 0.50 : 0.25;
            if (score >= threshold) {
                results.add(new RetrievalResult(chunk, score));
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
            if ("gia".equals(kw) || "su".equals(kw) || "cho".equals(kw) || "toi".equals(kw) || "can".equals(kw) || "tim".equals(kw)) {
                continue;
            }
            boolean matched = false;
            if (textContainsWord(title, kw)) {
                matched = true;
                titleBonus += 2;
            }
            if (textContainsWord(content, kw)) {
                matched = true;
            }
            if (textContainsWord(metadata, kw)) {
                matched = true;
                titleBonus++;
            }
            if (matched) {
                matchCount++;
            }
        }

        double ratio = (double) matchCount / Math.max(1, keywords.size());
        double score = (ratio * 0.65) + Math.min(0.35, titleBonus * 0.08);
        return Math.min(1.0, score);
    }

    private boolean textContainsWord(String text, String kw) {
        if (text == null || kw == null || text.isBlank() || kw.isBlank()) return false;
        if (kw.length() <= 4) {
            return Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS).matcher(text).find();
        }
        return text.contains(kw);
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
