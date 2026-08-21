package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiIntentServiceRolloutAndRoutingTest {

    private IntentClassifier intentClassifier;

    @Mock
    private LlmIntentClassifierService llmIntentClassifierService;

    private AiIntentService intentService;

    @BeforeEach
    void setUp() {
        intentClassifier = new IntentClassifier();
        intentService = new AiIntentService(intentClassifier, llmIntentClassifierService);
    }

    @Test
    @DisplayName("Fast-Path: Greeting (< 4 words) should resolve directly without invoking LLM")
    void testFastPathBypassesLlm() {
        intentService.setSemanticFirstEnabled(true);
        intentService.setRolloutPercent(100);

        AiIntentService.DetailedIntentResult result = intentService.classifyAndExtractDetailed("xin chào", 1L, 100L);

        assertEquals(AiDomain.CONVERSATION_SAFETY, result.domain());
        assertEquals(AiSubIntent.GREETING, result.subIntent());
        verifyNoInteractions(llmIntentClassifierService);
    }

    @Test
    @DisplayName("Semantic-First Enabled (100% Rollout): LLM is called first for complex queries")
    void testSemanticFirstRoutingSuccess() {
        intentService.setSemanticFirstEnabled(true);
        intentService.setRolloutPercent(100);

        when(llmIntentClassifierService.classifyWithLlm(anyString()))
            .thenReturn(new IntentClassifier.ClassificationDetail(
                AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.92, "/help"
            ));

        AiIntentService.DetailedIntentResult result = intentService.classifyAndExtractDetailed(
            "mô hình kết nối gia sư và bảo chứng thanh toán hoạt động ra sao?", 1L, 100L);

        assertEquals(AiDomain.CATALOG_FAQ, result.domain());
        assertEquals(AiSubIntent.FAQ_SEARCH, result.subIntent());
        assertEquals(0.92, result.confidence(), 0.001);
        verify(llmIntentClassifierService).classifyWithLlm(anyString());
    }

    @Test
    @DisplayName("Semantic-First OUT_OF_SCOPE: When LLM determines OUT_OF_SCOPE with high confidence, do NOT fallback to keyword classifier")
    void testSemanticFirstOutOfScopeHonored() {
        intentService.setSemanticFirstEnabled(true);
        intentService.setRolloutPercent(100);

        when(llmIntentClassifierService.classifyWithLlm(anyString()))
            .thenReturn(new IntentClassifier.ClassificationDetail(
                AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.85, null
            ));

        // Even if query contains words like "gia sư" in a non-marketplace sentence, LLM OUT_OF_SCOPE must be honored
        AiIntentService.DetailedIntentResult result = intentService.classifyAndExtractDetailed(
            "hồi xưa gia sư thường dạy ở hoàng gia phong kiến đúng không?", 1L, 100L);

        assertEquals(AiDomain.OUT_OF_SCOPE, result.domain());
        assertEquals(AiSubIntent.OUT_OF_SCOPE, result.subIntent());
    }

    @Test
    @DisplayName("Semantic-First Fallback: When LLM returns low confidence (< 0.70), fallback to deterministic keyword classifier")
    void testSemanticFirstLowConfidenceFallback() {
        intentService.setSemanticFirstEnabled(true);
        intentService.setRolloutPercent(100);

        when(llmIntentClassifierService.classifyWithLlm(anyString()))
            .thenReturn(new IntentClassifier.ClassificationDetail(
                AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.40, null
            ));

        AiIntentService.DetailedIntentResult result = intentService.classifyAndExtractDetailed(
            "tìm gia sư toán lớp 10", 1L, 100L);

        // Fallback to keyword classifier correctly recognizes FIND_TUTOR
        assertEquals(AiDomain.MARKETPLACE, result.domain());
        assertEquals(AiSubIntent.FIND_TUTOR, result.subIntent());
    }

    @Test
    @DisplayName("Semantic-First Fallback: When LLM throws exception/timeout, fallback to deterministic keyword classifier")
    void testSemanticFirstProviderErrorFallback() {
        intentService.setSemanticFirstEnabled(true);
        intentService.setRolloutPercent(100);

        when(llmIntentClassifierService.classifyWithLlm(anyString()))
            .thenThrow(new RuntimeException("Groq Connection Timeout"));

        AiIntentService.DetailedIntentResult result = intentService.classifyAndExtractDetailed(
            "tìm gia sư tiếng anh", 1L, 100L);

        assertEquals(AiDomain.MARKETPLACE, result.domain());
        assertEquals(AiSubIntent.FIND_TUTOR, result.subIntent());
    }

    @Test
    @DisplayName("Rollout Percentage Cohorting: Stable hash bucket assignment for same user and session")
    void testRolloutPercentCohorting() {
        intentService.setSemanticFirstEnabled(true);
        intentService.setRolloutPercent(50); // 50% rollout

        boolean isEligible1 = intentService.isSemanticFirstEligible(12345L, 100L);
        boolean isEligible2 = intentService.isSemanticFirstEligible(12345L, 100L);
        assertEquals(isEligible1, isEligible2, "Same user/session must always get identical cohort assignment");

        intentService.setRolloutPercent(0);
        assertFalse(intentService.isSemanticFirstEligible(12345L, 100L), "0% rollout must disable for all users");

        intentService.setRolloutPercent(100);
        assertTrue(intentService.isSemanticFirstEligible(12345L, 100L), "100% rollout must enable for all users");
    }

    @Test
    @DisplayName("Kill Switch Verification: When enabled=true but rollout=0%, LLM classifier is NEVER invoked")
    void testRolloutZeroKillsSemanticFirstEvenWhenEnabled() {
        intentService.setSemanticFirstEnabled(true);
        intentService.setRolloutPercent(0); // Kill switch / 0% cohort

        AiIntentService.DetailedIntentResult result = intentService.classifyAndExtractDetailed(
            "mô hình kết nối gia sư và bảo chứng thanh toán hoạt động ra sao?", 1L, 100L);

        assertNotNull(result);
        // LLM Classifier must NEVER be called when rollout = 0
        verifyNoInteractions(llmIntentClassifierService);
    }

    @Test
    @DisplayName("Legacy Mode: When semantic-first is disabled, uses keyword-first routing")
    void testLegacyModeRouting() {
        intentService.setSemanticFirstEnabled(false);

        AiIntentService.DetailedIntentResult result = intentService.classifyAndExtractDetailed(
            "tìm gia sư toán lớp 10", 1L, 100L);

        assertEquals(AiDomain.MARKETPLACE, result.domain());
        assertEquals(AiSubIntent.FIND_TUTOR, result.subIntent());
        verifyNoInteractions(llmIntentClassifierService);
    }
}
