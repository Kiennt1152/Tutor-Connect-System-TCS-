package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import com.tcs.module.ai.service.impl.AiServiceImpl;
import com.tcs.module.ai.service.provider.*;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplChatPipelineTest {

    @Mock private AiChatSessionRepository sessionRepository;
    @Mock private AiChatMessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private FaqEntryRepository faqEntryRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private com.tcs.module.ai.repository.AiKnowledgeChunkRepository chunkRepository;
    @Mock private AiConversationContextService contextService;
    @Mock private AiQueryRewriteService rewriteService;
    @Mock private AiIntentService intentService;
    @Mock private AiRetrievalService retrievalService;
    @Mock private AiRerankService rerankService;
    @Mock private ContextualChunkRetriever contextualChunkRetriever;
    @Mock private AiPromptBuilderService promptBuilderService;
    @Mock private AiAnswerEvaluatorService evaluatorService;
    @Mock private AiCapabilityRouter capabilityRouter;
    @Mock private AiFallbackService fallbackService;
    @Mock private AiHallucinationGuard hallucinationGuard;
    @Mock private ContentSafetyFilter contentSafetyFilter;
    @Mock private ConversationContextService conversationContextService;
    @Mock private AiSemanticCacheService semanticCacheService;
    @Mock private TcsSynonymService synonymService;
    @Mock private UserPreferenceService userPreferenceService;
    @Mock private AiTicketContextProvider ticketContextProvider;
    @Mock private AiAdminDashboardContextProvider dashboardContextProvider;
    @Mock private AiTutorSearchContextProvider tutorSearchContextProvider;
    @Mock private AiClassSearchContextProvider classSearchContextProvider;
    @Mock private AiPublicPlatformStatsContextProvider platformStatsContextProvider;
    @Mock private AiTutorFinanceContextProvider tutorFinanceContextProvider;
    @Mock private AiProviderRouter aiProviderRouter;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        AiFinanceGuardService financeGuardService = new AiFinanceGuardService(fallbackService);
        AiReferenceCardService referenceCardService = new AiReferenceCardService(tutorRepository, tutoringClassRepository, faqEntryRepository);
        AiHallucinationGuardService hallucinationGuardService = new AiHallucinationGuardService(hallucinationGuard, fallbackService);
        AiResponseBuilderService responseBuilderService = new AiResponseBuilderService();

        aiService = new AiServiceImpl(
            sessionRepository, messageRepository,
            platformAdminRepository, tutorRepository, tutorCenterRepository,
            clientRepository, chunkRepository,
            contextService, rewriteService, intentService, retrievalService, rerankService,
            contextualChunkRetriever, promptBuilderService, evaluatorService, capabilityRouter, fallbackService,
            financeGuardService, referenceCardService, hallucinationGuardService, responseBuilderService,
            contentSafetyFilter, conversationContextService,
            semanticCacheService, synonymService, userPreferenceService,
            ticketContextProvider, dashboardContextProvider, tutorSearchContextProvider,
            classSearchContextProvider, platformStatsContextProvider, tutorFinanceContextProvider,
            aiProviderRouter
        );

        lenient().when(synonymService.expandQuery(anyString())).thenAnswer(i -> i.getArgument(0));
        lenient().when(synonymService.normalizeQuery(anyString())).thenAnswer(i -> i.getArgument(0));
        lenient().when(conversationContextService.resolveFollowUpQuery(any(), anyString())).thenAnswer(i -> i.getArgument(1));
        lenient().when(rewriteService.rewriteQuery(any(), anyString(), any())).thenAnswer(i -> new AiQueryRewriteService.RewriteResult(i.getArgument(1), false, null));
    }

    @Test
    @DisplayName("Safety Block Path: Returns filtered response immediately when query violates safety rules")
    void testSafetyBlockPath() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Số điện thoại của em là 0912345678, liên hệ ngoài nhé");

        when(contentSafetyFilter.checkQuery(anyString())).thenReturn(
            new ContentSafetyFilter.SafetyCheckResult(false, "PRIVACY",
                "Để đảm bảo an toàn, vui lòng không trao đổi thông tin liên lạc cá nhân.", false)
        );
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(i -> i.getArgument(0));

        AiMessageResponse response = aiService.chat(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("không trao đổi thông tin liên lạc");
        assertThat(response.getAnswerMode()).isEqualTo("SAFETY_FILTER");
        assertThat(response.getGroundingStatus()).isEqualTo("SAFETY_FILTERED");
    }

    @Test
    @DisplayName("Semantic Cache Hit Path: Returns cached answer directly without calling LLM or RAG")
    void testSemanticCacheHitPath() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Phí sàn TCS là bao nhiêu");

        when(contentSafetyFilter.checkQuery(anyString())).thenReturn(
            new ContentSafetyFilter.SafetyCheckResult(true, null, null, false)
        );
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(i -> i.getArgument(0));

        AiSemanticCacheService.CachedResponse cached = new AiSemanticCacheService.CachedResponse(
            "Phí nền tảng TCS là 10% học phí.",
            "PAYMENT_SUPPORT", "FINANCE_WALLET", "PLATFORM_FEE_EXPLAIN",
            0.98, 1, null, null, null
        );
        when(semanticCacheService.get(anyString(), anyString())).thenReturn(Optional.of(cached));

        AiMessageResponse response = aiService.chat(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Phí nền tảng TCS là 10% học phí.");
        assertThat(response.getAnswerMode()).isEqualTo("CACHED");
        verify(aiProviderRouter, never()).chat(any());
    }

    @Test
    @DisplayName("Out of Scope Path: Returns polite guidance for off-topic requests")
    void testOutOfScopePath() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Thời tiết hôm nay thế nào");

        when(contentSafetyFilter.checkQuery(anyString())).thenReturn(
            new ContentSafetyFilter.SafetyCheckResult(true, null, null, false)
        );
        when(semanticCacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(i -> i.getArgument(0));

        when(intentService.classifyAndExtractDetailed(anyString(), any(), any())).thenReturn(
            new AiIntentService.DetailedIntentResult(
                AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.9, Map.of(), null
            )
        );

        AiMessageResponse response = aiService.chat(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).contains("nằm ngoài phạm vi hỗ trợ");
        assertThat(response.getAnswerMode()).isEqualTo("FALLBACK_OUT_OF_SCOPE");
        verify(aiProviderRouter, never()).chat(any());
    }

    @Test
    @DisplayName("Contextual Enrichment: Enriches matching chunks with surrounding context before prompt builder")
    void testContextualWindowEnrichment() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Tìm gia sư Toán 12");

        when(contentSafetyFilter.checkQuery(anyString())).thenReturn(
            new ContentSafetyFilter.SafetyCheckResult(true, null, null, false)
        );
        when(semanticCacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(i -> i.getArgument(0));

        when(intentService.classifyAndExtractDetailed(anyString(), any(), any())).thenReturn(
            new AiIntentService.DetailedIntentResult(
                AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR, AiIntent.FIND_TUTOR, 0.95, Map.of("subject", "Toán"), "/tim-gia-su"
            )
        );

        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setChunkId(10L);
        chunk.setContent("Gia sư Toán luyện thi ĐH");

        when(chunkRepository.findById(10L)).thenReturn(Optional.of(chunk));
        when(chunkRepository.findByActiveTrue()).thenReturn(List.of(chunk));

        AiSourceResponse source = new AiSourceResponse();
        source.setSourceId("10");
        source.setSourceType("TUTOR");
        source.setSnippet("Gia sư Toán luyện thi ĐH");
        source.setFinalScore(0.85);

        AiKnowledgeChunk chunkPre = new AiKnowledgeChunk();
        chunkPre.setChunkId(9L);
        chunkPre.setContent("Kinh nghiệm 5 năm sư phạm");

        AiKnowledgeChunk chunkPost = new AiKnowledgeChunk();
        chunkPost.setChunkId(11L);
        chunkPost.setContent("Học phí 200k/giờ");

        when(retrievalService.retrieve(anyString(), any(), any())).thenReturn(
            List.of(new AiRetrievalService.RetrievalResult(chunk, 0.85))
        );
        when(rerankService.rerank(any(), any(), anyString())).thenReturn(new ArrayList<>(List.of(source)));
        when(contextualChunkRetriever.retrieveWithContext(any(), any(), anyInt())).thenReturn(
            List.of(new ContextualChunkRetriever.ContextualChunk(
                chunk,
                List.of(chunkPre),
                List.of(chunkPost),
                "--- [Ngữ cảnh tài liệu liên quan trước] ---\nKinh nghiệm 5 năm sư phạm\n\n--- [Nội dung chính khớp] ---\nGia sư Toán luyện thi ĐH\n\n--- [Ngữ cảnh tài liệu liên quan sau] ---\nHọc phí 200k/giờ"
            ))
        );

        when(evaluatorService.evaluate(any(), any())).thenReturn(
            new AiAnswerEvaluatorService.EvaluatedAnswer("GROUNDED", 0.90, "HIGH", 1, "DIRECT_ANSWER", null, "Valid")
        );
        when(promptBuilderService.buildPrompt(anyString(), anyString(), any(), any(), any(), anyBoolean()))
            .thenReturn("Prompt with context");

        when(fallbackService.getLevel3NoData(any(), any())).thenReturn(
            new AiFallbackService.FallbackResult(3, "No data", null, List.of())
        );
        when(hallucinationGuard.guardTutorResponse(anyString(), any(), anyString())).thenAnswer(i -> i.getArgument(0));

        when(aiProviderRouter.chat(any())).thenReturn(
            new com.tcs.module.ai.service.provider.AiProviderChatResponse("Groq", "m", "Đã tìm thấy gia sư Toán phù hợp.", 200)
        );

        AiMessageResponse response = aiService.chat(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Đã tìm thấy gia sư Toán phù hợp.");
        assertThat(source.getSnippet()).contains("Kinh nghiệm 5 năm sư phạm");
        assertThat(source.getSnippet()).contains("Gia sư Toán luyện thi ĐH");
        assertThat(source.getSnippet()).contains("Học phí 200k/giờ");
    }
}
