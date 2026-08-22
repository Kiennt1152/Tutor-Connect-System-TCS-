package com.tcs.module.ai.service;

import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import com.tcs.module.ai.enums.RagStrategy;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.repository.SupportTicketRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase3AiEnhancementsTest {

    private FewShotIntentClassifier fewShotClassifier;
    private RagExperimentService experimentService;
    private ContextualChunkRetriever contextualChunkRetriever;

    @Mock
    private SupportTicketRepository supportTicketRepository;
    @Mock
    private FaqEntryRepository faqEntryRepository;

    private DynamicFaqGenerationService dynamicFaqService;

    @BeforeEach
    void setUp() {
        fewShotClassifier = new FewShotIntentClassifier();
        experimentService = new RagExperimentService();
        contextualChunkRetriever = new ContextualChunkRetriever();
        dynamicFaqService = new DynamicFaqGenerationService(supportTicketRepository, faqEntryRepository);
    }

    @Nested
    @DisplayName("1. Few-Shot Intent Classification Tests")
    class FewShotIntentClassificationTests {

        @Test
        @DisplayName("Few-shot matches representative queries with high exemplar similarity")
        void testFewShotMatching() {
            Optional<FewShotIntentClassifier.FewShotMatch> match = fewShotClassifier.classifyWithExemplars(
                "Phí sàn 10% tính trên khoản nào",
                0.50
            );

            assertThat(match).isPresent();
            assertThat(match.get().subIntent()).isEqualTo(AiSubIntent.PLATFORM_FEE_EXPLAIN);
            assertThat(match.get().domain()).isEqualTo(AiDomain.FINANCE_WALLET);
            assertThat(match.get().similarityScore()).isGreaterThanOrEqualTo(0.50);
        }

        @Test
        @DisplayName("Few-shot matches Out-of-Scope queries reliably")
        void testFewShotOutOfScope() {
            Optional<FewShotIntentClassifier.FewShotMatch> match = fewShotClassifier.classifyWithExemplars(
                "Thời tiết Hà Nội hôm nay thế nào",
                0.60
            );

            assertThat(match).isPresent();
            assertThat(match.get().domain()).isEqualTo(AiDomain.OUT_OF_SCOPE);
        }
    }

    @Nested
    @DisplayName("2. Dynamic FAQ Generation from Tickets Tests")
    class DynamicFaqGenerationTests {

        @Test
        @DisplayName("Generates draft FAQs when recurring support tickets exceed threshold")
        void testGenerateFaqsFromTickets() {
            SupportTicket t1 = new SupportTicket();
            t1.setTicketId(1L);
            t1.setCategory(SupportTicketCategory.INQUIRY);
            t1.setSubject("Lỗi nạp tiền VietQR chưa vào ví");
            t1.setDescription("Tôi đã chuyển khoản qua QR nhưng ví chưa cập nhật số dư");
            t1.setCreatedAt(LocalDateTime.now().minusDays(2));

            SupportTicket t2 = new SupportTicket();
            t2.setTicketId(2L);
            t2.setCategory(SupportTicketCategory.INQUIRY);
            t2.setSubject("Lỗi nạp tiền VietQR chưa nhận được");
            t2.setDescription("Chuyển tiền 500k qua VietQR 15 phút chưa thấy");
            t2.setCreatedAt(LocalDateTime.now().minusDays(1));

            when(supportTicketRepository.findAll()).thenReturn(List.of(t1, t2));
            when(faqEntryRepository.findAll()).thenReturn(new ArrayList<>());
            when(faqEntryRepository.save(any(FaqEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            List<FaqEntry> drafts = dynamicFaqService.generateFaqsFromRecentTickets(7, 2);

            assertThat(drafts).isNotEmpty();
            FaqEntry draft = drafts.get(0);
            assertThat(draft.getPublished()).isFalse(); // DRAFT for admin review
            assertThat(draft.getCategory()).isEqualTo("INQUIRY");
            assertThat(draft.getAnswer()).contains("VietQR/SePay");
        }
    }

    @Nested
    @DisplayName("3. A/B Testing & RAG Experiment Strategy Tests")
    class RagExperimentTests {

        @Test
        @DisplayName("Routes user sessions deterministically across A/B strategies")
        void testStrategyBucketing() {
            RagStrategy stratUser1 = experimentService.selectStrategy(10L, 100L);
            RagStrategy stratUser2 = experimentService.selectStrategy(25L, 200L);

            assertThat(stratUser1).isNotNull();
            assertThat(stratUser2).isNotNull();
        }

        @Test
        @DisplayName("Tracks execution metrics and computes summary accurately")
        void testMetricTracking() {
            experimentService.recordExecution(RagStrategy.HYBRID_VECTOR_BM25, 0.90, 3, 120, false);
            experimentService.recordExecution(RagStrategy.HYBRID_VECTOR_BM25, 0.80, 2, 80, true);

            Map<RagStrategy, RagExperimentService.StrategyMetric> summary = experimentService.getExperimentSummary();
            RagExperimentService.StrategyMetric hybridMetric = summary.get(RagStrategy.HYBRID_VECTOR_BM25);

            assertThat(hybridMetric.totalQueries()).isEqualTo(2);
            assertThat(hybridMetric.avgConfidence()).isCloseTo(0.85, org.assertj.core.data.Offset.offset(0.001));
            assertThat(hybridMetric.avgLatencyMs()).isEqualTo(100.0);
            assertThat(hybridMetric.cacheHits()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("4. Contextual Chunk Retrieval Tests")
    class ContextualChunkRetrievalTests {

        @Test
        @DisplayName("Retrieves neighboring chunks within the same document to provide coherent context window")
        void testContextualWindowRetrieval() {
            AiKnowledgeChunk chunk1 = AiKnowledgeChunk.builder()
                .chunkId(1L)
                .sourceType(KnowledgeSourceType.POLICY)
                .sourceId("POLICY_REFUND")
                .title("Điều khoản 1: Định nghĩa")
                .content("Học viên và gia sư tham gia hợp đồng giảng dạy.")
                .build();

            AiKnowledgeChunk chunk2 = AiKnowledgeChunk.builder()
                .chunkId(2L)
                .sourceType(KnowledgeSourceType.POLICY)
                .sourceId("POLICY_REFUND")
                .title("Điều khoản 2: Quy định hoàn tiền 100%")
                .content("Học viên được hoàn 100% học phí nếu hủy trước 24 giờ.")
                .build();

            AiKnowledgeChunk chunk3 = AiKnowledgeChunk.builder()
                .chunkId(3L)
                .sourceType(KnowledgeSourceType.POLICY)
                .sourceId("POLICY_REFUND")
                .title("Điều khoản 3: Trường hợp bất khả kháng")
                .content("Các trường hợp ốm đau có giấy xác nhận y tế sẽ được xem xét hoàn tiền.")
                .build();

            List<AiKnowledgeChunk> allChunks = List.of(chunk1, chunk2, chunk3);
            AiRetrievalService.RetrievalResult match = new AiRetrievalService.RetrievalResult(chunk2, 0.90);

            List<ContextualChunkRetriever.ContextualChunk> contextualList = contextualChunkRetriever.retrieveWithContext(
                List.of(match),
                allChunks,
                1 // windowSize = 1 chunk before and 1 chunk after
            );

            assertThat(contextualList).hasSize(1);
            ContextualChunkRetriever.ContextualChunk res = contextualList.get(0);
            assertThat(res.precedingChunks()).containsExactly(chunk1);
            assertThat(res.succeedingChunks()).containsExactly(chunk3);
            assertThat(res.mergedContext()).contains("--- [Ngữ cảnh tài liệu liên quan trước] ---");
            assertThat(res.mergedContext()).contains("--- [Nội dung chính khớp] ---");
            assertThat(res.mergedContext()).contains("--- [Ngữ cảnh tài liệu liên quan sau] ---");
        }
    }
}
