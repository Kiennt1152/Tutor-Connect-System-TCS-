package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRagRetrievalTest {

    @Mock
    private AiKnowledgeChunkRepository chunkRepository;

    @Mock
    private FaqEntryRepository faqRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private TutoringClassRepository classRepository;

    @Mock
    private TutorCertificateRepository certificateRepository;

    @Mock
    private TutorEducationRepository educationRepository;

    @Mock
    private TutorExperienceRepository experienceRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private AiPermissionFilterService permissionFilterService;

    private ObjectMapper objectMapper;
    private AiRetrievalService retrievalService;
    private KnowledgeIndexerService indexerService;
    private AiPromptBuilderService promptBuilderService;
    private IntentClassifier intentClassifier;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        retrievalService = new AiRetrievalService(chunkRepository, embeddingService, permissionFilterService, objectMapper);
        indexerService = new KnowledgeIndexerService(chunkRepository, faqRepository, tutorRepository, classRepository, certificateRepository, educationRepository, experienceRepository, embeddingService, objectMapper);
        promptBuilderService = new AiPromptBuilderService();
        intentClassifier = new IntentClassifier();
    }

    @Test
    @DisplayName("Hybrid Retrieval: Matches Escrow policy chunk via keyword when embeddings unavailable")
    void testHybridRetrievalKeywordFallback() {
        AiKnowledgeChunk escrowChunk = AiKnowledgeChunk.builder()
                .chunkId(1L)
                .sourceType(KnowledgeSourceType.POLICY)
                .sourceId("POLICY_ESCROW_AND_FEES")
                .title("Chính sách Ký quỹ Escrow và Phí sàn 10%")
                .content("Khi phụ huynh đồng ý thuê gia sư, số tiền học phí sẽ được nạp và tạm khóa trong tài khoản ký quỹ Escrow.")
                .metadataJson("{\"tags\":\"escrow,phi_san,thanh_toan\"}")
                .active(true)
                .build();

        when(chunkRepository.findByActiveTrue()).thenReturn(List.of(escrowChunk));
        when(permissionFilterService.canAccess(any(), any(), any())).thenReturn(true);
        when(embeddingService.getEmbedding(anyString())).thenReturn(Optional.empty());

        List<AiRetrievalService.RetrievalResult> results = retrievalService.retrieve("Làm sao để chắc chắn không bị bùng tiền cọc escrow?", "GUEST", null);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunk().getSourceId()).isEqualTo("POLICY_ESCROW_AND_FEES");
        assertThat(results.get(0).cosineSimilarity()).isGreaterThan(0.25);
    }

    @Test
    @DisplayName("Hybrid Retrieval: Calculates dense vector similarity when embedding is present")
    void testDenseVectorRetrieval() throws Exception {
        double[] queryVector = new double[]{0.5, 0.5, 0.5, 0.5};
        double[] chunkVector = new double[]{0.5, 0.5, 0.5, 0.5};

        AiKnowledgeChunk chunk = AiKnowledgeChunk.builder()
                .chunkId(2L)
                .sourceType(KnowledgeSourceType.POLICY)
                .sourceId("POLICY_REFUND_AND_DISPUTE")
                .title("Chính sách Hoàn tiền và Tranh chấp")
                .content("Học viên được hoàn 100% nếu hủy trước 24 giờ.")
                .embeddingJson(objectMapper.writeValueAsString(chunkVector))
                .active(true)
                .build();

        when(chunkRepository.findByActiveTrue()).thenReturn(List.of(chunk));
        when(permissionFilterService.canAccess(any(), any(), any())).thenReturn(true);
        when(embeddingService.getEmbedding("Tôi muốn hoàn tiền học phí")).thenReturn(Optional.of(queryVector));

        List<AiRetrievalService.RetrievalResult> results = retrievalService.retrieve("Tôi muốn hoàn tiền học phí", "GUEST", null);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).cosineSimilarity()).isGreaterThan(0.80);
    }

    @Test
    @DisplayName("Vector Search: 'gia sư văn' retrieves Literature tutor and discriminates Math tutor")
    void testVectorSearchSubjectDiscrimination() throws Exception {
        double[] literatureVector = new double[]{0.9, 0.9, 0.1, 0.1};
        double[] mathVector = new double[]{0.1, 0.1, 0.9, 0.9};
        double[] queryVector = new double[]{0.92, 0.88, 0.1, 0.1};

        AiKnowledgeChunk litTutorChunk = AiKnowledgeChunk.builder()
                .chunkId(10L)
                .sourceType(KnowledgeSourceType.TUTOR)
                .sourceId("10")
                .title("Hoàng Thu Trang")
                .content("Hồ sơ Gia sư: Hoàng Thu Trang\nChuyên môn & Giới thiệu: Chuyên dạy Ngữ Văn cấp 2 và cấp 3, ôn thi vào lớp 10 môn Văn")
                .embeddingJson(objectMapper.writeValueAsString(literatureVector))
                .active(true)
                .build();

        AiKnowledgeChunk mathTutorChunk = AiKnowledgeChunk.builder()
                .chunkId(11L)
                .sourceType(KnowledgeSourceType.TUTOR)
                .sourceId("11")
                .title("Nguyễn Văn Toán")
                .content("Hồ sơ Gia sư: Nguyễn Văn Toán\nChuyên môn & Giới thiệu: Gia sư Toán luyện thi học sinh giỏi cấp Quốc gia")
                .embeddingJson(objectMapper.writeValueAsString(mathVector))
                .active(true)
                .build();

        when(chunkRepository.findByActiveTrue()).thenReturn(List.of(litTutorChunk, mathTutorChunk));
        when(permissionFilterService.canAccess(any(), any(), any())).thenReturn(true);
        when(embeddingService.getEmbedding("gia sư văn")).thenReturn(Optional.of(queryVector));

        List<AiRetrievalService.RetrievalResult> results = retrievalService.retrieve("gia sư văn", "GUEST", null);

        assertThat(results).isNotEmpty();
        // Top match MUST be Literature tutor
        assertThat(results.get(0).chunk().getTitle()).isEqualTo("Hoàng Thu Trang");
        assertThat(results.get(0).cosineSimilarity()).isGreaterThan(0.80);
    }

    @Test
    @DisplayName("Intent Classification: Salary payout timeline query correctly classifies as FINANCE_WALLET, NOT FIND_TUTOR")
    void testTutorSalaryTimelineClassification() {
        IntentClassifier.ClassificationDetail detail = intentClassifier.classifyDetailed("lương của gia sư trả trong bao lâu");
        assertThat(detail.domain()).isEqualTo(AiDomain.FINANCE_WALLET);
        assertThat(detail.legacyIntent()).isEqualTo(AiIntent.PAYMENT_SUPPORT);
    }

    @Test
    @DisplayName("Prompt Builder: Injects Grounded Context, Zero-Hallucination rules, and Few-Shot Examples")
    void testPromptBuilderWithContext() {
        AiSourceResponse source = AiSourceResponse.builder()
                .sourceType("POLICY")
                .sourceId("POLICY_ESCROW_AND_FEES")
                .title("Chính sách Ký quỹ Escrow")
                .snippet("Học phí được tạm khóa trong Escrow và giải ngân sau khi hoàn tất lớp học.")
                .build();

        String prompt = promptBuilderService.buildPrompt("Quy trình thanh toán như thế nào?", AiIntent.PAYMENT_SUPPORT, "CLIENT", List.of(source));

        assertThat(prompt).contains("--- DỮ LIỆU THỰC TẾ HỆ THỐNG (CONTEXT) ---");
        assertThat(prompt).contains("[POLICY] Chính sách Ký quỹ Escrow");
        assertThat(prompt).contains("Học phí được tạm khóa trong Escrow");
        assertThat(prompt).contains("ZERO HALLUCINATION");
        assertThat(prompt).contains("FEW-SHOT EXAMPLES");
    }

    @Test
    @DisplayName("Knowledge Indexer: Reindexes FAQs and System Policies accurately")
    void testKnowledgeIndexer() {
        FaqEntry faq = new FaqEntry();
        faq.setFaqId(10L);
        faq.setQuestion("TCS là gì?");
        faq.setAnswer("TCS là sàn kết nối gia sư thông minh.");
        faq.setCategory("GENERAL");

        when(faqRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc()).thenReturn(List.of(faq));
        when(tutorRepository.findAll()).thenReturn(List.of());
        when(classRepository.findByStatus(any())).thenReturn(List.of());
        when(chunkRepository.findBySourceTypeAndSourceId(any(), anyString())).thenReturn(Optional.empty());

        Map<String, Integer> stats = indexerService.reindexAll();

        assertThat(stats).containsKeys("indexed", "skipped", "updated", "unchanged", "failed");
        assertThat(stats.get("indexed") + stats.get("skipped")).isGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("Rerank Service: Enforces 0.55 Relevance Gate strictly")
    void testRerankRelevanceGate055() {
        AiRerankService rerankService = new AiRerankService(objectMapper);
        
        AiKnowledgeChunk chunk = AiKnowledgeChunk.builder()
                .chunkId(99L)
                .sourceType(KnowledgeSourceType.TUTOR)
                .sourceId("99")
                .title("Gia sư Toán")
                .content("Dạy toán")
                .qualityScore(0.8)
                .build();
        
        // 0.52 is < 0.55 gate -> must be dropped
        var result52 = rerankService.rerank(List.of(new AiRetrievalService.RetrievalResult(chunk, 0.52)), null);
        assertThat(result52).isEmpty();

        // 0.58 is >= 0.55 gate -> must be kept
        var result58 = rerankService.rerank(List.of(new AiRetrievalService.RetrievalResult(chunk, 0.58)), null);
        assertThat(result58).hasSize(1);
    }

    @Test
    @DisplayName("Rerank Service: Penalizes Semantic Conflict between Preschool query and University Exam Tutor")
    void testSemanticConflictDetection() {
        AiRerankService rerankService = new AiRerankService(objectMapper);

        AiKnowledgeChunk universityTutor = AiKnowledgeChunk.builder()
                .chunkId(101L)
                .sourceType(KnowledgeSourceType.TUTOR)
                .sourceId("101")
                .title("Lê Văn Chuyên Toán")
                .content("Chuyên luyện thi đại học, ôn thi THPTQG lớp 12 điểm 9+")
                .qualityScore(0.9)
                .build();

        // Query for preschool / 5 years old
        AiIntentService.IntentResultWithEntities preschoolIntent = new AiIntentService.IntentResultWithEntities(
                AiIntent.FIND_TUTOR,
                0.95,
                Map.of("subject", "Toán", "grade", "mầm non", "level", "5 tuổi")
        );

        double penalty = rerankService.detectSemanticConflict(universityTutor, preschoolIntent.entities());
        assertThat(penalty).isGreaterThanOrEqualTo(0.20);

        // Verify final score is reduced by penalty
        var reranked = rerankService.rerank(
                List.of(new AiRetrievalService.RetrievalResult(universityTutor, 0.70)),
                preschoolIntent
        );

        assertThat(reranked).isNotEmpty();
        // Base: 0.70 * 0.80 * 0.90 = 0.504 - 0.20 (penalty) + 0.10 (intent) = ~0.404
        assertThat(reranked.get(0).getFinalScore()).isLessThan(0.50);
    }

    @Test
    @DisplayName("Knowledge Indexer: Computes Quality Score with Verified and Super Tutor Bonus accurately")
    void testQualityScoreCalculation() {
        AiKnowledgeChunk verifiedSuperTutor = AiKnowledgeChunk.builder()
                .title("Chuyên gia IELTS 8.5 Đặng Tuấn Anh")
                .content("Hơn 8 năm kinh nghiệm giảng dạy IELTS cho học sinh sinh viên chuẩn bị du học học bổng.")
                .metadataJson("{\"verified\":true,\"ratingAvg\":4.9,\"reviewsCount\":120}")
                .sourceUpdatedAt(java.time.LocalDateTime.now())
                .build();

        double score = indexerService.calculateQualityScore(verifiedSuperTutor);
        // Base (0.50) + Structured Meta (0.12) + Verified (0.08) + SuperTutor (0.05) + Length (0.15) + Title (0.05) + Recent (0.05) = 1.0
        assertThat(score).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    @DisplayName("Fallback Service: Enhanced No-Data Fallback provides clear guidance to /tao-lop")
    void testEnhancedNoDataFallback() {
        AiFallbackService fallbackService = new AiFallbackService();
        var fallback = fallbackService.getLevel3EnhancedNoData(
                AiSubIntent.FIND_TUTOR, 
                Map.of("subject", "Anh", "certLevel", "IELTS 7.5", "location", "Cầu Giấy")
        );

        assertThat(fallback.message()).contains("chưa tìm thấy gia sư phù hợp");
        assertThat(fallback.message()).contains("môn Anh");
        assertThat(fallback.message()).contains("IELTS 7.5");
        assertThat(fallback.message()).contains("Cầu Giấy");
        assertThat(fallback.message()).contains("/tao-lop");
        assertThat(fallback.suggestedRoute()).isEqualTo("/tao-lop");
    }
}
