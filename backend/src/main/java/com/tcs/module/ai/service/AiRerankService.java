package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiRerankService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiRerankService.class);

    private final ObjectMapper objectMapper;

    public List<AiSourceResponse> rerank(List<AiRetrievalService.RetrievalResult> retrievalResults, AiIntentService.IntentResultWithEntities intentData) {
        return rerank(retrievalResults, intentData, null);
    }

    public List<AiSourceResponse> rerank(List<AiRetrievalService.RetrievalResult> retrievalResults, AiIntentService.IntentResultWithEntities intentData, String query) {
        List<AiSourceResponse> reranked = new ArrayList<>();

        if (retrievalResults != null) {
            Map<String, String> entities = intentData != null ? intentData.entities() : Map.of();

            for (AiRetrievalService.RetrievalResult r : retrievalResults) {
                double cosineSimilarity = r.cosineSimilarity();
                
                // Relevance Gating: If vector cosine distance is far (< 0.55), drop it
                if (cosineSimilarity < 0.55) {
                    continue;
                }

                // Domain Intent Alignment (0.0 to 0.15, scaled down by 0.67 -> max 0.10)
                double intentBonus = calculateIntentAlignment(r.chunk().getSourceType(), intentData != null ? intentData.intent() : null);
                
                // Business Metadata Alignment (0.0 to 0.10)
                double businessBonus = calculateBusinessAlignment(r.chunk().getMetadataJson(), entities);

                // Quality Factor from chunk (defaults to 1.0)
                double qualityFactor = r.chunk().getQualityScore() != null ? r.chunk().getQualityScore() : 1.0;

                // Semantic Conflict Detection (Penalty: -0.15 to -0.50)
                double semanticPenalty = detectSemanticConflict(r.chunk(), entities);
                if (semanticPenalty >= 0.30) {
                    // Hard mismatch on subject or location for specific tutor/class searches
                    continue;
                }

                // Final Composite Score anchored by Dense Vector Cosine Similarity (80%) + Intent (10%) + Business (10%) - Semantic Penalty
                double rawScore = (cosineSimilarity * 0.80 * qualityFactor - semanticPenalty) + (intentBonus * 0.67) + businessBonus;
                double finalScore = Math.min(1.0, Math.max(0.0, rawScore));

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
        }

        reranked.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));
        return reranked;
    }

    public double detectSemanticConflict(AiKnowledgeChunk chunk, Map<String, String> entities) {
        if (chunk == null || entities == null || entities.isEmpty()) return 0.0;

        String contentLower = chunk.getContent() != null ? chunk.getContent() : "";
        String titleLower = chunk.getTitle() != null ? chunk.getTitle() : "";
        String rawText = (titleLower + " " + contentLower).toLowerCase(Locale.ROOT);
        String normText = VietnameseTextNormalizer.removeDiacritics(rawText);

        double penalty = 0.0;

        // 1. Grade / Level Conflict (e.g. Preschool/Primary vs University/High School Exam prep)
        String rawGrade = entities.getOrDefault("grade", "").toLowerCase(Locale.ROOT);
        String rawLevel = entities.getOrDefault("level", "").toLowerCase(Locale.ROOT);
        String rawGoal = entities.getOrDefault("learningGoal", "").toLowerCase(Locale.ROOT);

        String normGrade = VietnameseTextNormalizer.removeDiacritics(rawGrade);
        String normLevel = VietnameseTextNormalizer.removeDiacritics(rawLevel);
        String normGoal = VietnameseTextNormalizer.removeDiacritics(rawGoal);

        boolean isPreschoolOrPrimary = (normGrade.contains("mam non") || normGrade.contains("tieu hoc") || normGrade.contains("mau giao")
                || normGrade.equals("1") || normGrade.equals("2") || normGrade.equals("3") || normGrade.equals("4") || normGrade.equals("5")
                || normLevel.contains("tieu hoc") || normLevel.contains("mam non") || normLevel.contains("tre em") || normLevel.contains("5 tuoi") || normLevel.contains("6 tuoi"))
                && !normGrade.contains("12");

        boolean isAdvancedOrHighSchool = normText.contains("luyen thi dai hoc") || normText.contains("thptqg") || normText.contains("lop 12") || normText.contains("lop 11")
                || normText.contains("luyen thi thpt") || normText.contains("cao hoc") || normText.contains("thac si");

        boolean mentionsPrimary = normText.contains("tieu hoc") || normText.contains("mam non") || normText.contains("mau giao")
                || normText.contains("lop 1 ") || normText.contains("lop 2 ") || normText.contains("lop 3 ") || normText.contains("lop 4 ") || normText.contains("lop 5 ")
                || normText.contains("lop 1,") || normText.contains("lop 2,");

        if (isPreschoolOrPrimary && isAdvancedOrHighSchool && !mentionsPrimary) {
            penalty += 0.20;
        }

        boolean isHighSchoolExamSeeker = normGrade.equals("12") || normGoal.contains("dai hoc") || normGoal.contains("thptqg");
        boolean isOnlyPrimaryTutor = (normText.contains("chuyen kem tieu hoc") || normText.contains("chuyen day lop 1 2 3") || normText.contains("ren chu dep"))
                && !normText.contains("lop 12") && !normText.contains("dai hoc");

        if (isHighSchoolExamSeeker && isOnlyPrimaryTutor) {
            penalty += 0.20;
        }

        // 2. Cross-Language / Subject Mismatch
        String rawSubject = entities.getOrDefault("subject", "").toLowerCase(Locale.ROOT);
        String rawCert = entities.getOrDefault("certLevel", "").toLowerCase(Locale.ROOT);
        String normSubject = VietnameseTextNormalizer.removeDiacritics(rawSubject);
        String normCert = VietnameseTextNormalizer.removeDiacritics(rawCert);

        if (normSubject.contains("tieng trung") || normCert.contains("hsk") || normCert.contains("tocfl")) {
            if ((normText.contains("tieng anh") || normText.contains("ielts") || normText.contains("toan"))
                    && !normText.contains("trung") && !normText.contains("hsk") && !normText.contains("hoa ngu") && !normText.contains("chinese")) {
                penalty += 0.30;
            }
        }

        if (normSubject.contains("tieng han") || normCert.contains("topik")) {
            if ((normText.contains("tieng anh") || normText.contains("ielts") || normText.contains("toan"))
                    && !normText.contains("han") && !normText.contains("topik") && !normText.contains("korean")) {
                penalty += 0.30;
            }
        }

        if (normSubject.equals("toan") || normSubject.contains("toan")) {
            if ((normText.contains("ngu van") || normText.contains("tieng anh"))
                    && !normText.contains("toan") && !normText.contains("math")) {
                penalty += 0.35;
            }
        }

        if (normSubject.equals("anh") || normSubject.contains("tieng anh") || normCert.contains("ielts") || normCert.contains("toeic")) {
            if ((normText.contains("mon toan") || normText.contains("ngu van") || normText.contains("vat ly"))
                    && !normText.contains("tieng anh") && !normText.contains("anh") && !normText.contains("ielts") && !normText.contains("toeic") && !normText.contains("english")) {
                penalty += 0.35;
            }
        }

        if (normSubject.contains("python") || normSubject.contains("lap trinh") || normSubject.contains("tin")) {
            if (!normText.contains("python") && !normText.contains("lap trinh") && !normText.contains("coding") && !normText.contains("tin hoc")) {
                penalty += 0.40;
            }
        }

        if (normSubject.equals("hoa") || normSubject.contains("hoa hoc")) {
            if (!normText.contains("hoa") && !normText.contains("chemistry")) {
                penalty += 0.40;
            }
        }

        if (normSubject.equals("van") || normSubject.contains("ngu van")) {
            if (!normText.contains("van") && !normText.contains("ngu van") && !normText.contains("van hoc")) {
                penalty += 0.40;
            }
        }

        // 3. Inter-Provincial Offline Location Conflict
        String rawLocation = entities.getOrDefault("location", "").toLowerCase(Locale.ROOT);
        String normLocation = VietnameseTextNormalizer.removeDiacritics(rawLocation);
        String mode = entities.getOrDefault("mode", "").toUpperCase(Locale.ROOT);

        if (!"ONLINE".equalsIgnoreCase(mode) && !normLocation.isBlank()) {
            boolean isHanoiQuery = normLocation.contains("ha noi") || normLocation.contains("hanoi") || normLocation.contains("cau giay") || normLocation.contains("ba dinh") || normLocation.contains("dong da") || normLocation.contains("thanh xuan") || normLocation.contains("hoan kiem") || normLocation.contains("hai ba trung") || normLocation.contains("long bien") || normLocation.contains("nam tu liem") || normLocation.contains("bac tu liem") || normLocation.contains("tay ho") || normLocation.contains("ha dong");
            boolean isHcmQuery = normLocation.contains("ho chi minh") || normLocation.contains("hcm") || normLocation.contains("sai gon") || normLocation.contains("quan 1") || normLocation.contains("quan 3") || normLocation.contains("quan 7") || normLocation.contains("binh thanh") || normLocation.contains("go vap") || normLocation.contains("thu duc") || normLocation.contains("tan binh") || normLocation.contains("phu nhuan");

            boolean isHanoiChunk = normText.contains("ha noi") || normText.contains("cau giay") || normText.contains("ba dinh") || normText.contains("dong da") || normText.contains("thanh xuan");
            boolean isHcmChunk = normText.contains("ho chi minh") || normText.contains("tp.hcm") || normText.contains("sai gon") || normText.contains("quan 1") || normText.contains("quan 3") || normText.contains("quan 7") || normText.contains("thu duc");

            if (isHanoiQuery && isHcmChunk && !isHanoiChunk) {
                penalty += 0.15;
            } else if (isHcmQuery && isHanoiChunk && !isHcmChunk) {
                penalty += 0.15;
            }
        }

        return Math.min(0.50, penalty);
    }

    private double calculateIntentAlignment(KnowledgeSourceType sourceType, AiIntent intent) {
        if (intent == null) return 0.05;
        
        switch (intent) {
            case FIND_TUTOR:
                if (sourceType == KnowledgeSourceType.TUTOR) return 0.15;
                if (sourceType == KnowledgeSourceType.FAQ) return 0.05;
                break;
            case FIND_CLASS:
            case CREATE_CLASS:
                if (sourceType == KnowledgeSourceType.CLASS) return 0.15;
                if (sourceType == KnowledgeSourceType.FAQ) return 0.05;
                break;
            case TICKET_SUPPORT:
            case PAYMENT_SUPPORT:
            case FAQ_SUPPORT:
            case TUTOR_VERIFICATION:
                if (sourceType == KnowledgeSourceType.FAQ || sourceType == KnowledgeSourceType.POLICY || sourceType == KnowledgeSourceType.SYSTEM_DOC) {
                    return 0.15;
                }
                break;
            default:
                if (sourceType == KnowledgeSourceType.SYSTEM_DOC || sourceType == KnowledgeSourceType.POLICY || sourceType == KnowledgeSourceType.FAQ) {
                    return 0.10;
                }
        }
        return 0.0;
    }

    private double calculateBusinessAlignment(String metadataJson, Map<String, String> entities) {
        if (metadataJson == null || metadataJson.isBlank() || entities == null || entities.isEmpty()) return 0.0;
        
        try {
            JsonNode metadata = objectMapper.readTree(metadataJson);
            double bonus = 0.0;

            // Subject alignment
            if (entities.containsKey("subject")) {
                String sub = entities.get("subject").toLowerCase(Locale.ROOT);
                String bio = metadata.has("bio") ? metadata.get("bio").asText("").toLowerCase(Locale.ROOT) : "";
                String subjects = metadata.has("subjects") ? metadata.get("subjects").asText("").toLowerCase(Locale.ROOT) : "";
                if (bio.contains(sub) || subjects.contains(sub)) {
                    bonus += 0.04;
                }
            }
            
            // Location alignment
            if (entities.containsKey("location") && metadata.has("area")) {
                String loc = entities.get("location").toLowerCase(Locale.ROOT);
                String area = metadata.get("area").asText("").toLowerCase(Locale.ROOT);
                if (area.contains(loc)) {
                    bonus += 0.02;
                }
            }

            // Fee ceiling alignment
            if (entities.containsKey("maxFee") && metadata.has("fee")) {
                try {
                    long maxFee = Long.parseLong(entities.get("maxFee"));
                    long fee = metadata.get("fee").asLong();
                    if (fee <= maxFee) bonus += 0.02;
                } catch (Exception ignored) {}
            }

            // Verified tutor bonus
            if (metadata.has("verified") && metadata.get("verified").asBoolean(false)) {
                bonus += 0.01;
            }

            // High rating bonus
            if (metadata.has("ratingAvg")) {
                double rating = metadata.get("ratingAvg").asDouble(0.0);
                if (rating >= 4.8) bonus += 0.01;
            }

            return Math.min(0.10, bonus);
        } catch (Exception e) {
            log.warn("Failed to parse metadata JSON in rerank alignment: {}", e.getMessage());
            return 0.0;
        }
    }
}
