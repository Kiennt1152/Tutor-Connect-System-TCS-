package com.tcs.module.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.RateLimitExceededException;
import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import com.tcs.module.ai.service.impl.AiServiceImpl;
import com.tcs.module.ai.service.provider.*;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiProductionHardeningTest {

    @Nested
    @DisplayName("1. AiProviderChatRequest Boundary & Validation Tests")
    class ProviderRequestValidationTests {

        @Test
        @DisplayName("Should clamp maxOutputTokens within 1..4096")
        void shouldClampMaxOutputTokens() {
            var reqLow = new AiProviderChatRequest("sys", "user", 0, 0.5);
            assertEquals(700, reqLow.maxOutputTokens());

            var reqHigh = new AiProviderChatRequest("sys", "user", 10000, 0.5);
            assertEquals(4096, reqHigh.maxOutputTokens());

            var reqValid = new AiProviderChatRequest("sys", "user", 1500, 0.5);
            assertEquals(1500, reqValid.maxOutputTokens());
        }

        @Test
        @DisplayName("Should clamp temperature within 0.0..2.0")
        void shouldClampTemperature() {
            var reqNeg = new AiProviderChatRequest("sys", "user", 500, -1.5);
            assertEquals(0.0, reqNeg.temperature());

            var reqHigh = new AiProviderChatRequest("sys", "user", 500, 3.5);
            assertEquals(2.0, reqHigh.temperature());

            var reqNan = new AiProviderChatRequest("sys", "user", 500, Double.NaN);
            assertEquals(0.0, reqNan.temperature());
        }

        @Test
        @DisplayName("Should normalize negative timeoutMs to 0")
        void shouldNormalizeTimeoutMs() {
            var req = new AiProviderChatRequest("sys", "user", 500, 0.7, -5000L);
            assertEquals(0L, req.timeoutMs());
        }
    }

    @Nested
    @DisplayName("2. AiProviderRouter Total Generation Deadline & Health Tests")
    class ProviderRouterResilienceTests {

        private AiProviderRouter router;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
            objectMapper = new ObjectMapper();
            router = new AiProviderRouter(objectMapper);
        }

        @Test
        @DisplayName("Should stop provider failover when total deadline is exhausted")
        void shouldStopFailoverWhenDeadlineExhausted() {
            router.setTotalGenerationDeadlineMs(50L); // 50ms total budget

            AiChatProviderClient slowProvider1 = mock(AiChatProviderClient.class);
            when(slowProvider1.isConfigured()).thenReturn(true);
            when(slowProvider1.chat(any())).thenAnswer(inv -> {
                Thread.sleep(60L); // Exceeds 50ms budget
                return new AiProviderChatResponse("Slow1", "m", null, 500);
            });

            AiChatProviderClient provider2 = mock(AiChatProviderClient.class);

            router.registerProvider("slow1", slowProvider1);
            router.registerProvider("prov2", provider2);
            router.setExecutionOrder(List.of("slow1", "prov2"));

            AiProviderChatRequest request = new AiProviderChatRequest("sys", "user", 500, 0.5);
            AiProviderChatResponse response = router.chat(request);

            assertNull(response);
            verify(slowProvider1, times(1)).chat(any());
            // provider2 should NOT be called because deadline was exhausted
            verify(provider2, never()).chat(any());
        }

        @Test
        @DisplayName("Should skip provider during 429 cooldown window")
        void shouldSkipProviderInCooldown() {
            router.setTotalGenerationDeadlineMs(20000L);

            AiChatProviderClient rateLimitedProvider = mock(AiChatProviderClient.class);
            when(rateLimitedProvider.isConfigured()).thenReturn(true);
            when(rateLimitedProvider.chat(any())).thenReturn(new AiProviderChatResponse("P1", "m", null, 429));

            AiChatProviderClient backupProvider = mock(AiChatProviderClient.class);
            when(backupProvider.isConfigured()).thenReturn(true);
            when(backupProvider.chat(any())).thenReturn(new AiProviderChatResponse("P2", "m", "Backup response", 200));

            router.registerProvider("p1", rateLimitedProvider);
            router.registerProvider("p2", backupProvider);
            router.setExecutionOrder(List.of("p1", "p2"));

            // First call: P1 returns 429, router falls over to P2
            AiProviderChatResponse resp1 = router.chat(new AiProviderChatRequest("sys", "user", 500, 0.5));
            assertNotNull(resp1);
            assertEquals("Backup response", resp1.content());
            assertTrue(router.isProviderInCooldown("p1"));

            // Second call: P1 is in cooldown -> router skips P1 directly and calls P2
            AiProviderChatResponse resp2 = router.chat(new AiProviderChatRequest("sys", "user", 500, 0.5));
            assertNotNull(resp2);
            assertEquals("Backup response", resp2.content());

            // Verify P1 was only called once (in the first request)
            verify(rateLimitedProvider, times(1)).chat(any());
            verify(backupProvider, times(2)).chat(any());
        }
    }

    @Nested
    @DisplayName("3. Dynamic FAQ Count in OpenDomainHandler")
    class OpenDomainDynamicStatsTests {

        @Mock
        private FaqEntryRepository faqEntryRepository;

        @Test
        @DisplayName("Should dynamically report actual FAQ count from repository")
        void shouldReportDynamicFaqCount() {
            when(faqEntryRepository.count()).thenReturn(247L);

            OpenDomainHandler handler = new OpenDomainHandler(null, null, faqEntryRepository);
            OpenDomainHandler.OpenDomainResponse response = handler.handlePlatformStats("faq");

            assertNotNull(response);
            assertTrue(response.answer().contains("**247 câu hỏi thường gặp (FAQ)**"));
        }
    }

    @Nested
    @DisplayName("4. Rate Limiting in AiServiceImpl Pipeline")
    class AiServiceRateLimitingTests {

        @Mock private AiChatSessionRepository sessionRepository;
        @Mock private AiChatMessageRepository messageRepository;
        @Mock private UserRepository userRepository;
        @Mock private PlatformAdminRepository platformAdminRepository;
        @Mock private TutorRepository tutorRepository;
        @Mock private TutorCenterRepository tutorCenterRepository;
        @Mock private ClientRepository clientRepository;
        @Mock private FaqEntryRepository faqEntryRepository;
        @Mock private TutoringClassRepository tutoringClassRepository;

        @Mock private AiConversationContextService contextService;
        @Mock private AiQueryRewriteService rewriteService;
        @Mock private AiIntentService intentService;
        @Mock private AiRetrievalService retrievalService;
        @Mock private AiRerankService rerankService;
        @Mock private AiPromptBuilderService promptBuilderService;
        @Mock private AiAnswerEvaluatorService evaluatorService;
        @Mock private AiCapabilityRouter capabilityRouter;
        @Mock private AiFallbackService fallbackService;
        @Mock private AiHallucinationGuard hallucinationGuard;
        @Mock private OpenDomainHandler openDomainHandler;
        @Mock private ContentSafetyFilter contentSafetyFilter;
        @Mock private OpenDomainRateLimiter openDomainRateLimiter;
        @Mock private ConversationContextService conversationContextService;
        @Mock private OpenDomainAnalytics openDomainAnalytics;

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
            aiService = new AiServiceImpl(
                sessionRepository, messageRepository, userRepository, platformAdminRepository,
                tutorRepository, tutorCenterRepository, clientRepository, faqEntryRepository,
                tutoringClassRepository, contextService, rewriteService, intentService,
                retrievalService, rerankService, promptBuilderService, evaluatorService,
                capabilityRouter, fallbackService, hallucinationGuard, openDomainHandler,
                contentSafetyFilter, openDomainRateLimiter, conversationContextService,
                openDomainAnalytics, ticketContextProvider, dashboardContextProvider,
                tutorSearchContextProvider, classSearchContextProvider, platformStatsContextProvider,
                tutorFinanceContextProvider, aiProviderRouter, new ObjectMapper()
            );
        }

        @Test
        @DisplayName("Should throw RateLimitExceededException (HTTP 429) when rate limit exceeded")
        void shouldThrow429WhenRateLimitExceeded() {
            ChatRequest request = new ChatRequest();
            request.setSessionId(1L);
            request.setMessage("Tìm gia sư Toán");

            AiChatSession session = new AiChatSession();
            session.setSessionId(1L);
            session.setUserId(100L);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
            when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(contentSafetyFilter.checkQuery(any())).thenReturn(new ContentSafetyFilter.SafetyCheckResult(true, null, null, false));
            when(intentService.classifyAndExtractDetailed(any(), any(), any())).thenReturn(
                new AiIntentService.DetailedIntentResult(AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR, AiIntent.FIND_TUTOR, 0.95, Map.of(), null)
            );
            // Simulate rate limit exceeded
            when(openDomainRateLimiter.allowRequest(any(), any(), any())).thenReturn(false);

            assertThrows(RateLimitExceededException.class, () -> aiService.chat(request, 100L));

            // Verify no retrieval or LLM provider was called
            verifyNoInteractions(retrievalService);
            verifyNoInteractions(aiProviderRouter);
        }
    }

    @Nested
    @DisplayName("5. Batch Message Hydration in AiServiceImpl")
    class MessageHydrationBatchingTests {

        @Mock private AiChatSessionRepository sessionRepository;
        @Mock private AiChatMessageRepository messageRepository;
        @Mock private TutorRepository tutorRepository;
        @Mock private TutoringClassRepository tutoringClassRepository;
        @Mock private FaqEntryRepository faqEntryRepository;

        private AiServiceImpl aiService;

        @BeforeEach
        void setUp() {
            aiService = new AiServiceImpl(
                sessionRepository, messageRepository, null, null,
                tutorRepository, null, null, faqEntryRepository,
                tutoringClassRepository, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, new ObjectMapper()
            );
        }

        @Test
        @DisplayName("Should hydrate tutor, class, and FAQ references in batches using findAllById")
        void shouldHydrateReferencesInBatch() {
            Long sessionId = 10L;
            AiChatSession session = new AiChatSession();
            session.setSessionId(sessionId);
            session.setUserId(100L);

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            AiChatMessage m1 = new AiChatMessage();
            m1.setMessageId(1L);
            m1.setRole("assistant");
            m1.setContent("Gia sư phù hợp");
            m1.setReferencedTutorIds("101,102");

            AiChatMessage m2 = new AiChatMessage();
            m2.setMessageId(2L);
            m2.setRole("assistant");
            m2.setContent("Lớp học phù hợp");
            m2.setReferencedClassIds("201");
            m2.setReferencedFaqIds("301");

            when(messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId))
                .thenReturn(List.of(m1, m2));

            Tutor t1 = new Tutor(); t1.setTutorId(101L); t1.setFullName("Nguyễn Văn A");
            Tutor t2 = new Tutor(); t2.setTutorId(102L); t2.setFullName("Trần Thị B");
            when(tutorRepository.findAllById(anySet())).thenReturn(List.of(t1, t2));

            TutoringClass c1 = new TutoringClass(); c1.setClassId(201L); c1.setTitle("Toán 12");
            when(tutoringClassRepository.findAllById(anySet())).thenReturn(List.of(c1));

            FaqEntry f1 = new FaqEntry(); f1.setFaqId(301L); f1.setQuestion("Chính sách hoàn tiền?");
            when(faqEntryRepository.findAllById(anySet())).thenReturn(List.of(f1));

            List<AiMessageResponse> responses = aiService.getSessionMessages(sessionId, 100L);

            assertEquals(2, responses.size());
            assertEquals(2, responses.get(0).getReferencedTutors().size());
            assertEquals("Nguyễn Văn A", responses.get(0).getReferencedTutors().get(0).getFullName());
            assertEquals("Trần Thị B", responses.get(0).getReferencedTutors().get(1).getFullName());

            assertEquals(1, responses.get(1).getReferencedClasses().size());
            assertEquals("Toán 12", responses.get(1).getReferencedClasses().get(0).getTitle());

            assertEquals(1, responses.get(1).getReferencedFaqs().size());
            assertEquals("Chính sách hoàn tiền?", responses.get(1).getReferencedFaqs().get(0).getQuestion());

            // Verify findAllById was invoked once per repository (batch) instead of findById in a loop
            verify(tutorRepository, times(1)).findAllById(anySet());
            verify(tutoringClassRepository, times(1)).findAllById(anySet());
            verify(faqEntryRepository, times(1)).findAllById(anySet());
        }
    }
}
