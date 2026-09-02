package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
@ExtendWith(MockitoExtension.class)
class Phase2AiEnhancementsTest {

    private Bm25Scorer bm25Scorer;
    private ConfidenceCalibrator confidenceCalibrator;
    private UserPreferenceService userPreferenceService;
    private ConversationContextService conversationContextService;

    @Mock
    private AiKnowledgeChunkRepository chunkRepository;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private AiPermissionFilterService permissionFilterService;

    private AiRetrievalService retrievalService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        bm25Scorer = new Bm25Scorer();
        confidenceCalibrator = new ConfidenceCalibrator();
        userPreferenceService = new UserPreferenceService();
        conversationContextService = new ConversationContextService();
        objectMapper = new ObjectMapper();

        retrievalService = new AiRetrievalService(
            chunkRepository,
            embeddingService,
            permissionFilterService,
            objectMapper,
            bm25Scorer
        );

        lenient().when(permissionFilterService.canAccess(any(), any(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("1. Hybrid Search (Vector + BM25 Keyword Search) Tests")
    class HybridSearchTests {

        @Test
        @DisplayName("BM25 accurately matches exact terms like 'phí 10%', 'OTP', 'CCCD', 'Escrow'")
        void testExactKeywordMatching() {
            AiKnowledgeChunk chunk1 = AiKnowledgeChunk.builder()
                .chunkId(1L)
                .title("Chính sách phí nền tảng TCS")
                .content("Hệ thống TCS thu phí sàn 10% trên mỗi hợp đồng hoàn thành thông qua Escrow.")
                .metadataJson("{\"category\":\"policy\"}")
                .sourceType(KnowledgeSourceType.POLICY)
                .active(true)
                .build();

            AiKnowledgeChunk chunk2 = AiKnowledgeChunk.builder()
                .chunkId(2L)
                .title("Tìm gia sư Tiếng Anh")
                .content("Gia sư dạy kèm IELTS và giao tiếp tại nhà.")
                .metadataJson("{\"subject\":\"Tiếng Anh\"}")
                .sourceType(KnowledgeSourceType.TUTOR)
                .active(true)
                .build();

            List<AiKnowledgeChunk> chunks = List.of(chunk1, chunk2);
            Bm25Scorer.CorpusStats stats = bm25Scorer.buildStats(chunks);

            double score1 = bm25Scorer.score("Phí sàn 10% và Escrow", chunk1, stats);
            double score2 = bm25Scorer.score("Phí sàn 10% và Escrow", chunk2, stats);

            assertThat(score1).isGreaterThan(score2);
            assertThat(score1).isGreaterThan(0.40);
        }

        @Test
        @DisplayName("Hybrid Retrieval fuses Dense Vector and BM25 scores effectively")
        void testHybridRetrievalFusion() throws Exception {
            double[] queryVec = new double[]{0.5, 0.5, 0.0};
            double[] chunkVec = new double[]{0.5, 0.5, 0.0};

            AiKnowledgeChunk chunk = AiKnowledgeChunk.builder()
                .chunkId(10L)
                .title("Quy trình xác minh CCCD")
                .content("Gia sư cần tải lên ảnh CCCD 2 mặt và bằng cấp để được duyệt hồ sơ.")
                .embeddingJson(objectMapper.writeValueAsString(chunkVec))
                .sourceType(KnowledgeSourceType.SYSTEM_DOC)
                .active(true)
                .build();

            when(chunkRepository.findByActiveTrue()).thenReturn(List.of(chunk));
            when(embeddingService.getEmbedding(anyString())).thenReturn(Optional.of(queryVec));

            List<AiRetrievalService.RetrievalResult> results = retrievalService.retrieve(
                "Làm sao xác minh CCCD và bằng cấp?",
                "TUTOR",
                1L
            );

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).cosineSimilarity()).isGreaterThan(0.60);
        }
    }

    @Nested
    @DisplayName("2. Confidence Calibration Tests")
    class ConfidenceCalibrationTests {

        @Test
        @DisplayName("Discounts confidence for vague search without subject or grade entities")
        void testConfidenceDiscountForVagueSearch() {
            double raw = 0.80;
            Map<String, String> emptyEntities = Map.of();

            double calibrated = confidenceCalibrator.calibrate(
                raw,
                AiDomain.MARKETPLACE,
                AiSubIntent.FIND_TUTOR,
                emptyEntities,
                "Tôi muốn tìm gia sư"
            );

            assertThat(calibrated).isLessThan(raw);
            assertThat(calibrated).isEqualTo(raw * 0.85);
        }

        @Test
        @DisplayName("Boosts confidence when rich specific entities exist")
        void testConfidenceBoostForRichEntities() {
            double raw = 0.80;
            Map<String, String> richEntities = Map.of(
                "subject", "Toán",
                "grade", "12",
                "location", "Cầu Giấy"
            );

            double calibrated = confidenceCalibrator.calibrate(
                raw,
                AiDomain.MARKETPLACE,
                AiSubIntent.FIND_TUTOR,
                richEntities,
                "Tìm gia sư Toán 12 tại Cầu Giấy"
            );

            assertThat(calibrated).isGreaterThan(raw);
        }

        @Test
        @DisplayName("Boosts confidence to maximum for high-certainty domain terms (10%, OTP, CCCD)")
        void testHighCertaintyKeywords() {
            double calibrated = confidenceCalibrator.calibrate(
                0.70,
                AiDomain.FINANCE_WALLET,
                AiSubIntent.PLATFORM_FEE_EXPLAIN,
                Map.of(),
                "Phí nền tảng TCS là 10% đúng không?"
            );

            assertThat(calibrated).isGreaterThanOrEqualTo(0.98);
        }
    }

    @Nested
    @DisplayName("3. User Preference Learning Tests")
    class UserPreferenceLearningTests {

        @Test
        @DisplayName("Learns user preferences across multiple interactions")
        void testLearnUserPreferences() {
            Long userId = 55L;

            userPreferenceService.updateFromInteraction(
                userId,
                Map.of("subject", "Toán", "grade", "12", "location", "Cầu Giấy", "maxFee", "250000"),
                "Tìm gia sư Toán 12 ở Cầu Giấy dưới 250k"
            );

            Optional<UserPreferenceService.UserPreferences> pref = userPreferenceService.getPreferences(userId);
            assertThat(pref).isPresent();
            assertThat(pref.get().preferredSubjects()).contains("Toán");
            assertThat(pref.get().preferredGrades()).contains("12");
            assertThat(pref.get().preferredLocations()).contains("Cầu Giấy");
            assertThat(pref.get().maxBudget()).isEqualTo(250000L);
        }

        @Test
        @DisplayName("Enriches vague query entities with learned user preferences")
        void testEnrichWithPreferences() {
            Long userId = 55L;

            userPreferenceService.updateFromInteraction(
                userId,
                Map.of("subject", "Vật lý", "grade", "11"),
                "Tìm gia sư Lý 11"
            );

            Map<String, String> vagueEntities = new HashMap<>();
            Map<String, String> enriched = userPreferenceService.enrichWithPreferences(userId, vagueEntities);

            assertThat(enriched).containsKey("preferredSubject");
            assertThat(enriched.get("preferredSubject")).isEqualTo("Vật lý");
            assertThat(enriched).containsKey("preferredGrade");
            assertThat(enriched.get("preferredGrade")).isEqualTo("11");
        }
    }

    @Nested
    @DisplayName("4. Session Context Expansion Tests")
    class SessionContextExpansionTests {

        @Test
        @DisplayName("Accumulates mentioned tutors, classes, faqs, and topic frequencies")
        void testAccumulateSessionContext() {
            Long sessionId = 999L;

            conversationContextService.saveContext(
                sessionId,
                AiDomain.MARKETPLACE,
                AiSubIntent.FIND_TUTOR,
                Map.of("subject", "Toán", "grade", "12"),
                "Tìm gia sư Toán 12",
                List.of(101L, 102L),
                List.of(),
                List.of(),
                "Tìm gia sư ôn thi đại học"
            );

            ConversationContextService.ConversationContext ctx = conversationContextService.getContext(sessionId);
            assertThat(ctx).isNotNull();
            assertThat(ctx.mentionedTutorIds()).containsExactly(101L, 102L);
            assertThat(ctx.userGoal()).isEqualTo("Tìm gia sư ôn thi đại học");
            assertThat(ctx.topicFrequency()).containsKey("FIND_TUTOR");
        }

        @Test
        @DisplayName("Resolves follow-up query with referenced tutor from expanded session context")
        void testFollowUpResolutionWithReferencedTutor() {
            Long sessionId = 999L;

            conversationContextService.saveContext(
                sessionId,
                AiDomain.MARKETPLACE,
                AiSubIntent.FIND_TUTOR,
                Map.of("subject", "Toán", "location", "Hà Nội"),
                "Tìm gia sư Toán",
                List.of(101L),
                List.of(),
                List.of(),
                null
            );

            ConversationContextService.FollowUpResolution followUp = conversationContextService.resolveFollowUp(
                sessionId,
                "Gia sư đó dạy học phí bao nhiêu?",
                Map.of()
            );

            assertThat(followUp.isFollowUp()).isTrue();
            assertThat(followUp.resolvedEntities()).containsKey("lastMentionedTutorId");
            assertThat(followUp.resolvedEntities().get("lastMentionedTutorId")).isEqualTo("101");
        }
    }
}
