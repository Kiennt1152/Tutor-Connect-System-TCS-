package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiRerankService {

    private final ObjectMapper objectMapper;

    public List<AiSourceResponse> rerank(List<AiRetrievalService.RetrievalResult> retrievalResults, AiIntentService.IntentResultWithEntities intentData) {
        return rerank(retrievalResults, intentData, null);
    }

    public List<AiSourceResponse> rerank(List<AiRetrievalService.RetrievalResult> retrievalResults, AiIntentService.IntentResultWithEntities intentData, String query) {
        List<AiSourceResponse> reranked = new ArrayList<>();

        for (AiRetrievalService.RetrievalResult r : retrievalResults) {
            double cosineSimilarity = r.cosineSimilarity();
            double intentMatch = calculateIntentMatch(r.chunk().getSourceType(), r.chunk().getTitle(), r.chunk().getMetadataJson(), intentData, query);
            double businessMatch = calculateBusinessMatch(r.chunk().getMetadataJson(), intentData != null ? intentData.entities() : Map.of());
            double qualityScore = r.chunk().getQualityScore() != null ? r.chunk().getQualityScore() : 0.5;
            double keywordBonus = calculateKeywordBonus(r.chunk().getTitle(), r.chunk().getContent(), query);

            // Re-weighting: higher weight on intent & query domain relevance
            double finalScore = (cosineSimilarity * 0.40) + (intentMatch * 0.25) + (businessMatch * 0.15) + (qualityScore * 0.05) + (keywordBonus * 0.15);

            reranked.add(AiSourceResponse.builder()
                .sourceId(r.chunk().getSourceId())
                .sourceType(r.chunk().getSourceType().name())
                .title(r.chunk().getTitle())
                .snippet(r.chunk().getContent())
                .similarity(cosineSimilarity)
                .finalScore(finalScore)
                .visibility(r.chunk().getVisibility())
                .build());
        }

        reranked.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
        return reranked;
    }

    private double calculateIntentMatch(KnowledgeSourceType sourceType, String title, String metadataJson, AiIntentService.IntentResultWithEntities intentData, String query) {
        if (intentData == null || intentData.intent() == null) return 0.5;
        
        String lowerTitle = title != null ? title.toLowerCase(Locale.ROOT) : "";
        String lowerQuery = query != null ? query.toLowerCase(Locale.ROOT) : "";
        
        switch (intentData.intent()) {
            case FIND_TUTOR:
                if (sourceType == KnowledgeSourceType.TUTOR) return 1.0;
                if (sourceType == KnowledgeSourceType.FAQ && (lowerTitle.contains("tìm gia sư") || lowerTitle.contains("lọc gia sư"))) return 0.7;
                break;
            case FIND_CLASS:
            case CREATE_CLASS:
                if (sourceType == KnowledgeSourceType.CLASS) return 1.0;
                if (sourceType == KnowledgeSourceType.FAQ && (lowerTitle.contains("lớp") || lowerTitle.contains("đăng bài"))) return 0.8;
                break;
            case TICKET_SUPPORT:
                if (sourceType == KnowledgeSourceType.FAQ || sourceType == KnowledgeSourceType.POLICY || sourceType == KnowledgeSourceType.SYSTEM_DOC) {
                    if (lowerTitle.contains("ticket") || lowerTitle.contains("khiếu nại") || lowerTitle.contains("tranh chấp") || 
                        lowerTitle.contains("báo cáo") || lowerTitle.contains("vi phạm") || lowerTitle.contains("lách sàn") || lowerTitle.contains("sla")) {
                        return 1.0;
                    }
                    // Penalize generic tutor search FAQ when intent is ticket/dispute/report
                    if (lowerTitle.contains("tìm gia sư phù hợp") && !lowerQuery.contains("tìm gia sư")) {
                        return 0.1;
                    }
                    return 0.7;
                }
                break;
            case PAYMENT_SUPPORT:
                if (sourceType == KnowledgeSourceType.FAQ || sourceType == KnowledgeSourceType.POLICY || sourceType == KnowledgeSourceType.SYSTEM_DOC) {
                    if (lowerTitle.contains("tiền") || lowerTitle.contains("escrow") || lowerTitle.contains("hoàn tiền") || 
                        lowerTitle.contains("thu nhập") || lowerTitle.contains("rút tiền") || lowerTitle.contains("lương") || 
                        lowerTitle.contains("phí") || lowerTitle.contains("ví")) {
                        return 1.0;
                    }
                    if (lowerTitle.contains("tìm gia sư phù hợp") && !lowerQuery.contains("tìm gia sư")) {
                        return 0.1;
                    }
                    return 0.7;
                }
                break;
            case FAQ_SUPPORT:
                if (sourceType == KnowledgeSourceType.FAQ || sourceType == KnowledgeSourceType.POLICY || sourceType == KnowledgeSourceType.SYSTEM_DOC) {
                    return 0.9;
                }
                break;
            case TUTOR_VERIFICATION:
                if (sourceType == KnowledgeSourceType.FAQ && lowerTitle.contains("xác minh")) return 1.0;
                break;
            default:
                if (sourceType == KnowledgeSourceType.SYSTEM_DOC || sourceType == KnowledgeSourceType.POLICY || sourceType == KnowledgeSourceType.FAQ) return 0.8;
                return 0.5;
        }
        return 0.3;
    }

    private double calculateKeywordBonus(String title, String content, String query) {
        if (query == null || query.isBlank()) return 0.5;
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        String lowerTitle = title != null ? title.toLowerCase(Locale.ROOT) : "";
        String lowerContent = content != null ? content.toLowerCase(Locale.ROOT) : "";
        
        double bonus = 0.5;
        String[] keywords = {"ticket", "khiếu nại", "tranh chấp", "báo cáo", "lách sàn", "hoàn tiền", "escrow", "rút tiền", "thu nhập", "doanh thu", "dashboard", "xác minh"};
        for (String kw : keywords) {
            if (lowerQuery.contains(kw)) {
                if (lowerTitle.contains(kw)) {
                    bonus += 0.3;
                } else if (lowerContent.contains(kw)) {
                    bonus += 0.15;
                }
            }
        }
        return Math.min(1.0, bonus);
    }

    private double calculateBusinessMatch(String metadataJson, Map<String, String> entities) {
        if (metadataJson == null || metadataJson.isBlank() || entities == null || entities.isEmpty()) return 0.5;
        
        try {
            JsonNode metadata = objectMapper.readTree(metadataJson);
            double score = 0.5;

            // Check Subject
            if (entities.containsKey("subject") && metadata.has("subjects")) {
                if (metadata.get("subjects").asText("").toLowerCase().contains(entities.get("subject").toLowerCase())) {
                    score += 0.2;
                }
            }
            
            // Check Location
            if (entities.containsKey("location") && metadata.has("area")) {
                if (metadata.get("area").asText("").toLowerCase().contains(entities.get("location").toLowerCase())) {
                    score += 0.1;
                }
            }

            // Check Fee
            if (entities.containsKey("maxFee") && metadata.has("fee")) {
                try {
                    long maxFee = Long.parseLong(entities.get("maxFee"));
                    long fee = metadata.get("fee").asLong();
                    if (fee <= maxFee) score += 0.2;
                } catch (Exception ignored) {}
            }

            return Math.min(1.0, score);
        } catch (Exception e) {
            return 0.5;
        }
    }
}
