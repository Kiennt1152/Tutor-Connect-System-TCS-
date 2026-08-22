package com.tcs.module.ai.service;

import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Bm25Scorer {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private static final Set<String> STOP_WORDS = Set.of(
        "cho", "toi", "can", "tim", "co", "khong", "la", "gi", "cua", "duoc", 
        "cac", "nhung", "va", "hay", "the", "nao", "o", "dau", "may", "bao", 
        "nhieu", "mot", "hai", "ban", "minh", "em", "anh", "chi", "nhe", "a", "da"
    );

    private static final List<String> EXACT_DOMAIN_TERMS = List.of(
        "10%", "otp", "cccd", "escrow", "vietqr", "sepay", "vi tien", "rut tien",
        "nap tien", "hop dong", "hoc phi", "gia su", "trung tam", "hoan tien",
        "khieu nai", "danh gia", "uy tin", "xac minh", "bang cap", "hoc ba"
    );

    public record CorpusStats(
        int totalDocs,
        double avgDocLength,
        Map<String, Integer> docFrequencies
    ) {}

    public CorpusStats buildStats(List<AiKnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new CorpusStats(0, 0, Map.of());
        }

        int totalDocs = chunks.size();
        long totalLength = 0;
        Map<String, Integer> dfMap = new HashMap<>();

        for (AiKnowledgeChunk chunk : chunks) {
            List<String> tokens = tokenizeChunk(chunk);
            totalLength += tokens.size();
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String t : uniqueTokens) {
                dfMap.put(t, dfMap.getOrDefault(t, 0) + 1);
            }
        }

        double avgDocLength = totalDocs > 0 ? (double) totalLength / totalDocs : 1.0;
        return new CorpusStats(totalDocs, avgDocLength, dfMap);
    }

    public double score(String query, AiKnowledgeChunk chunk, CorpusStats stats) {
        if (query == null || chunk == null || stats == null || stats.totalDocs() == 0) {
            return 0.0;
        }

        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return 0.0;
        }

        List<String> chunkTokens = tokenizeChunk(chunk);
        int docLength = chunkTokens.size();
        if (docLength == 0) return 0.0;

        Map<String, Integer> termFreqs = new HashMap<>();
        for (String t : chunkTokens) {
            termFreqs.put(t, termFreqs.getOrDefault(t, 0) + 1);
        }

        // Title and metadata matches have higher importance
        String titleTokensStr = chunk.getTitle() != null ? VietnameseTextNormalizer.normalize(chunk.getTitle().toLowerCase(Locale.ROOT)) : "";
        String metaTokensStr = chunk.getMetadataJson() != null ? VietnameseTextNormalizer.normalize(chunk.getMetadataJson().toLowerCase(Locale.ROOT)) : "";

        // Exact domain term matching bonus
        double exactBonus = calculateExactTermBonus(query, chunk);

        int matchedCount = 0;
        double bm25Score = 0.0;
        for (String term : queryTokens) {
            int tf = termFreqs.getOrDefault(term, 0);
            if (titleTokensStr.contains(term)) {
                tf += 2;
            }
            if (metaTokensStr.contains(term)) {
                tf += 1;
            }

            if (tf == 0) continue;
            matchedCount++;

            int df = stats.docFrequencies().getOrDefault(term, 0);
            double idf = Math.log(1.0 + (stats.totalDocs() - df + 0.5) / (df + 0.5));
            if (idf < 0.2) idf = 0.2;

            double numerator = tf * (K1 + 1.0);
            double denominator = tf + K1 * (1.0 - B + B * (docLength / Math.max(1.0, stats.avgDocLength())));
            bm25Score += idf * (numerator / denominator);
        }

        if (matchedCount == 0 && exactBonus == 0.0) {
            return 0.0;
        }

        double normalizedBm25 = Math.min(1.0, bm25Score / Math.max(1.0, Math.min(queryTokens.size(), 4) * 1.5));
        double score = (normalizedBm25 * 0.75) + exactBonus;
        if (exactBonus > 0) {
            score = Math.max(0.35, score);
        }

        return Math.min(1.0, score);
    }

    private double calculateExactTermBonus(String query, AiKnowledgeChunk chunk) {
        String queryNorm = VietnameseTextNormalizer.normalize(query.toLowerCase(Locale.ROOT));
        String contentNorm = (chunk.getTitle() != null ? chunk.getTitle() : "") + " " +
                             (chunk.getContent() != null ? chunk.getContent() : "") + " " +
                             (chunk.getMetadataJson() != null ? chunk.getMetadataJson() : "");
        contentNorm = VietnameseTextNormalizer.normalize(contentNorm.toLowerCase(Locale.ROOT));

        double bonus = 0.0;
        for (String term : EXACT_DOMAIN_TERMS) {
            if (queryNorm.contains(term) && contentNorm.contains(term)) {
                bonus += 0.15;
            }
        }
        return Math.min(0.35, bonus);
    }

    public List<String> tokenizeChunk(AiKnowledgeChunk chunk) {
        StringBuilder sb = new StringBuilder();
        if (chunk.getTitle() != null) sb.append(chunk.getTitle()).append(" ");
        if (chunk.getContent() != null) sb.append(chunk.getContent()).append(" ");
        if (chunk.getMetadataJson() != null) sb.append(chunk.getMetadataJson());
        return tokenize(sb.toString());
    }

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = VietnameseTextNormalizer.normalize(text.toLowerCase(Locale.ROOT));
        String cleaned = normalized.replaceAll("[^a-z0-9à-ỹ%\\s]", " ").trim();
        String[] rawTokens = cleaned.split("\\s+");

        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            String t = token.trim();
            if (t.length() >= 2 && !STOP_WORDS.contains(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }
}
