package com.tcs.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import com.tcs.module.ai.service.AiAnswerEvaluatorService;
import com.tcs.module.ai.service.AiCapabilityRouter;
import com.tcs.module.ai.service.AiConversationContextService;
import com.tcs.module.ai.service.AiFallbackService;
import com.tcs.module.ai.service.AiFinanceGuardService;
import com.tcs.module.ai.service.AiHallucinationGuardService;
import com.tcs.module.ai.service.AiIntentService;
import com.tcs.module.ai.service.AiPromptBuilderService;
import com.tcs.module.ai.service.AiQueryRewriteService;
import com.tcs.module.ai.service.AiReferenceCardService;
import com.tcs.module.ai.service.AiRerankService;
import com.tcs.module.ai.service.AiResponseBuilderService;
import com.tcs.module.ai.service.AiRetrievalService;
import com.tcs.module.ai.service.AiSemanticCacheService;
import com.tcs.module.ai.service.ContentSafetyFilter;
import com.tcs.module.ai.service.ConversationContextService;
import com.tcs.module.ai.service.TcsSynonymService;
import com.tcs.module.ai.service.UserPreferenceService;
import com.tcs.module.ai.service.provider.AiProviderChatResponse;
import com.tcs.module.ai.service.provider.AiProviderRouter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module AI - luong hoi dap tro ly ao.
 * Bam bo test case trong Report_5.1_UnitTest: sheet chat.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiServiceImplChatTest {

    private static final Long SESSION_ID = 700L;
    private static final Long USER_ID = 42L;
    private static final String QUESTION = "TCS hiện có bao nhiêu gia sư đang hoạt động?";

    @Mock private AiChatSessionRepository sessionRepository;
    @Mock private AiChatMessageRepository messageRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AiKnowledgeChunkRepository chunkRepository;
    @Mock private AiConversationContextService contextService;
    @Mock private AiQueryRewriteService rewriteService;
    @Mock private AiIntentService intentService;
    @Mock private AiRetrievalService retrievalService;
    @Mock private AiRerankService rerankService;
    @Mock private com.tcs.module.ai.service.ContextualChunkRetriever contextualChunkRetriever;
    @Mock private AiPromptBuilderService promptBuilderService;
    @Mock private AiAnswerEvaluatorService evaluatorService;
    @Mock private AiCapabilityRouter capabilityRouter;
    @Mock private AiFallbackService fallbackService;
    @Mock private AiFinanceGuardService financeGuardService;
    @Mock private AiReferenceCardService referenceCardService;
    @Mock private AiHallucinationGuardService hallucinationGuardService;
    @Mock private AiResponseBuilderService responseBuilderService;
    @Mock private ContentSafetyFilter contentSafetyFilter;
    @Mock private ConversationContextService conversationContextService;
    @Mock private AiSemanticCacheService semanticCacheService;
    @Mock private TcsSynonymService synonymService;
    @Mock private UserPreferenceService userPreferenceService;
    @Mock private com.tcs.module.ai.service.AiTicketContextProvider ticketContextProvider;
    @Mock private com.tcs.module.ai.service.AiAdminDashboardContextProvider dashboardContextProvider;
    @Mock private com.tcs.module.ai.service.provider.AiTutorSearchContextProvider tutorSearchContextProvider;
    @Mock private com.tcs.module.ai.service.provider.AiClassSearchContextProvider classSearchContextProvider;
    @Mock private com.tcs.module.ai.service.provider.AiPublicPlatformStatsContextProvider
            platformStatsContextProvider;
    @Mock private com.tcs.module.ai.service.provider.AiTutorFinanceContextProvider tutorFinanceContextProvider;
    @Mock private AiProviderRouter aiProviderRouter;

    @InjectMocks private AiServiceImpl aiService;

    private AiChatSession existingSession;

    @BeforeEach
    void setUp() {
        existingSession = new AiChatSession();
        existingSession.setSessionId(SESSION_ID);
        existingSession.setUserId(USER_ID);
        existingSession.setTitle("Phien cu");

        // Vai tro nguoi dung: khong khop ho so nao -> "USER".
        when(platformAdminRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());
        when(tutorRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());
        when(tutorCenterRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());
        when(clientRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());

        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(existingSession));
        when(sessionRepository.save(any(AiChatSession.class))).thenAnswer(inv -> {
            AiChatSession saved = inv.getArgument(0);
            if (saved.getSessionId() == null) {
                saved.setSessionId(SESSION_ID + 1);
            }
            return saved;
        });
        when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(inv -> {
            AiChatMessage saved = inv.getArgument(0);
            if (saved.getMessageId() == null) {
                saved.setMessageId(9000L);
            }
            return saved;
        });

        // Bo loc an toan cho qua, khong co cache ngu nghia.
        when(contentSafetyFilter.checkQuery(anyString()))
                .thenReturn(new ContentSafetyFilter.SafetyCheckResult(true, null, null, false));
        when(semanticCacheService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(contextService.getHistory(anyLong())).thenReturn(List.of());
        when(conversationContextService.resolveFollowUpQuery(anyLong(), anyString())).thenReturn(QUESTION);
        when(synonymService.expandQuery(anyString())).thenReturn(QUESTION);
        when(synonymService.normalizeQuery(anyString())).thenReturn(QUESTION);
        when(userPreferenceService.enrichWithPreferences(anyLong(), any())).thenReturn(Map.of());

        // Phan loai y dinh: hoi so lieu nen tang, khong bi chan boi fast-path hay policy.
        when(intentService.classifyAndExtractDetailed(anyString(), any(), any()))
                .thenReturn(new AiIntentService.DetailedIntentResult(
                        AiDomain.CATALOG_FAQ, AiSubIntent.PLATFORM_STATS, AiIntent.PLATFORM_STATS,
                        0.9, Map.of(), "/help"));
        when(fallbackService.checkLevel0Safety(any())).thenReturn(null);
        when(capabilityRouter.getPolicy(any(), any())).thenReturn(null);
        when(rewriteService.rewriteQuery(anyList(), anyString(), any()))
                .thenReturn(new AiQueryRewriteService.RewriteResult(QUESTION, false, AiIntent.PLATFORM_STATS));

        when(evaluatorService.evaluate(any(), anyList()))
                .thenReturn(new AiAnswerEvaluatorService.EvaluatedAnswer(
                        "GROUNDED", 0.9, "HIGH", 1, "GROUNDED", null, "ok"));
        when(referenceCardService.hydrateCards(any(), any(), anyList(), any()))
                .thenReturn(new AiReferenceCardService.ReferenceCards(List.of(), List.of(), List.of()));
        when(financeGuardService.checkFinanceAccess(any(), anyString(), anyString(), any()))
                .thenReturn(null);
        when(promptBuilderService.buildPrompt(
                anyString(), anyString(), any(), anyString(), anyList(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn("prompt");
        when(hallucinationGuardService.applyGuards(
                anyString(), any(), any(), any(), anyList(), anyList(), anyList(),
                anyString(), anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(responseBuilderService.build(
                any(), any(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyList(), anyString(), org.mockito.ArgumentMatchers.anyDouble(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), anyString(), anyList(), anyList(), anyList(),
                anyList(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any()))
                .thenReturn(AiMessageResponse.builder().messageId(9000L).build());
    }

    /** Nguon RAG dat nguong diem (>= 0.60) va mang san cau tra lo so lieu nen tang. */
    private AiSourceResponse statsSource() {
        return AiSourceResponse.builder()
                .sourceId("STATS")
                .sourceType("SYSTEM")
                .title("Thong ke nen tang")
                .snippet("TCS hiện có 128 gia sư đang hoạt động.")
                .similarity(0.95)
                .finalScore(0.95)
                .visibility("PUBLIC")
                .build();
    }

    private void givenRagSources(List<AiSourceResponse> sources) {
        when(retrievalService.retrieve(anyString(), anyString(), any())).thenReturn(List.of());
        when(rerankService.rerank(anyList(), any(), anyString())).thenReturn(sources);
    }

    private void givenLlmAnswer(String content) {
        when(aiProviderRouter.chat(any()))
                .thenReturn(new AiProviderChatResponse("gemini", "gemini-2.0", content, 200));
    }

    private void givenLlmFailure() {
        when(aiProviderRouter.chat(any())).thenThrow(new IllegalStateException("All AI providers failed"));
    }

    /** Noi dung cau tra loi cuoi cung ma service dua vao responseBuilderService. */
    private String capturedAnswer() {
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(responseBuilderService).build(
                any(), any(), content.capture(), anyString(), anyString(), anyString(), anyString(),
                anyList(), anyString(), org.mockito.ArgumentMatchers.anyDouble(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), anyString(), anyList(), anyList(), anyList(),
                anyList(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any());
        return content.getValue();
    }

    private ChatRequest request(Long sessionId) {
        ChatRequest request = new ChatRequest();
        request.setSessionId(sessionId);
        request.setMessage(QUESTION);
        return request;
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("UTCID01 (N) - sessionId = null, LLM tra loi thanh cong, co du lieu RAG khop -> tra ve cau tra loi cua LLM tren phien moi")
        void utcid01_newSessionWithLlmAnswer() {
            givenRagSources(List.of(statsSource()));
            givenLlmAnswer("Hiện có 128 gia sư đang hoạt động trên TCS.");

            AiMessageResponse response = aiService.chat(request(null), USER_ID);

            assertNotNull(response);
            assertEquals("Hiện có 128 gia sư đang hoạt động trên TCS.", capturedAnswer());
            // Phien moi duoc tao vi khong truyen sessionId (khong doc lai phien cu).
            verify(sessionRepository, never()).findById(any());
            verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(any(AiChatSession.class));
        }

        @Test
        @DisplayName("UTCID02 (N) - sessionId da ton tai, LLM tra loi thanh cong -> dung lai phien cu")
        void utcid02_existingSessionWithLlmAnswer() {
            givenRagSources(List.of(statsSource()));
            givenLlmAnswer("Hiện có 128 gia sư đang hoạt động trên TCS.");

            aiService.chat(request(SESSION_ID), USER_ID);

            assertEquals("Hiện có 128 gia sư đang hoạt động trên TCS.", capturedAnswer());
            verify(sessionRepository).findById(SESSION_ID);
            ArgumentCaptor<AiChatMessage> saved = ArgumentCaptor.forClass(AiChatMessage.class);
            verify(messageRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
            assertEquals(SESSION_ID, saved.getAllValues().get(0).getSession().getSessionId());
        }

        @Test
        @DisplayName("UTCID03 (A) - LLM loi/timeout nhung co du lieu RAG khop -> tra ve cau tra loi RAG fallback")
        void utcid03_llmFailsButRagMatches() {
            givenRagSources(List.of(statsSource()));
            givenLlmFailure();

            aiService.chat(request(null), USER_ID);

            assertEquals("TCS hiện có 128 gia sư đang hoạt động.", capturedAnswer(),
                    "LLM loi thi phai lay noi dung tu nguon RAG da truy hoi");
        }

        @Test
        @DisplayName("UTCID04 (A) - LLM loi/timeout va khong co du lieu RAG khop -> tra ve thong bao khong tim thay du lieu phu hop")
        void utcid04_llmFailsAndNoRagMatch() {
            givenRagSources(List.of());
            givenLlmFailure();

            aiService.chat(request(null), USER_ID);

            String answer = capturedAnswer();
            assertNotNull(answer);
            assertTrue(answer.contains("Trợ lý AI của Tutor Connect System"),
                    "Khong co nguon RAG thi phai roi ve cau tra loi mac dinh cua he thong");
            verify(aiProviderRouter).chat(any());
        }
    }
}
