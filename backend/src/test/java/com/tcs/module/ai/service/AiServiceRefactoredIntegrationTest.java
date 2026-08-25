package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.FaqReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AiServiceRefactoredIntegrationTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiFinanceGuardService financeGuardService;

    @Autowired
    private AiReferenceCardService referenceCardService;

    @Autowired
    private AiHallucinationGuardService hallucinationGuardService;

    @Autowired
    private AiResponseBuilderService responseBuilderService;

    @Test
    @DisplayName("End-to-End Chat Flow with Extracted Services")
    void testChatFlowWithExtractedServices() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Tìm gia sư Toán lớp 12 tại Hà Nội");

        AiMessageResponse response = aiService.chat(request, null);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getDomain()).isEqualTo(AiDomain.MARKETPLACE.name());
    }

    @Test
    @DisplayName("Finance Guard Service: Blocks unauthorized users for personal finance queries")
    void testFinanceGuardWithUnauthorizedUser() {
        String blockedMsg = financeGuardService.checkFinanceAccess(
            AiDomain.FINANCE_WALLET,
            "Lương của tôi tháng này là bao nhiêu?",
            "GUEST",
            99999L
        );

        assertThat(blockedMsg).isNotNull();
        assertThat(blockedMsg).contains("Gia sư hoặc Trung tâm gia sư");
    }

    @Test
    @DisplayName("Finance Guard Service: Allows authorized TUTOR users")
    void testFinanceGuardWithTutorUser() {
        String allowedMsg = financeGuardService.checkFinanceAccess(
            AiDomain.FINANCE_WALLET,
            "Lương của tôi tháng này là bao nhiêu?",
            "TUTOR",
            10L
        );

        assertThat(allowedMsg).isNull();
    }

    @Test
    @DisplayName("Reference Card Service: Hydrates empty collections gracefully on null IDs")
    void testReferenceCardHydration() {
        List<TutorReferenceDto> tutors = referenceCardService.hydrateTutorsByIds(List.of());
        List<ClassReferenceDto> classes = referenceCardService.hydrateClassesByIds(List.of());
        List<FaqReferenceDto> faqs = referenceCardService.hydrateFaqsByIds(List.of());

        assertThat(tutors).isEmpty();
        assertThat(classes).isEmpty();
        assertThat(faqs).isEmpty();
    }

    @Test
    @DisplayName("Hallucination Guard Service: Guards responses safely")
    void testHallucinationGuardSafeExecution() {
        String guarded = hallucinationGuardService.applyGuards(
            "Đây là câu trả lời thử nghiệm.",
            AiDomain.MARKETPLACE,
            AiSubIntent.FIND_TUTOR,
            Map.of("subject", "Toán"),
            List.of(),
            List.of(),
            List.of(),
            "Tìm gia sư",
            "GUEST",
            null
        );

        assertThat(guarded).isNotNull();
    }

    @Test
    @DisplayName("Response Builder Service: Correctly constructs DTOs with defaults")
    void testResponseBuilderService() {
        AiMessageResponse resp = responseBuilderService.build(
            1L, 2L, "Xin chào", "OUT_OF_SCOPE", "CONVERSATION_SAFETY", "GREETING",
            null, null, "DIRECT", 1.0, "HIGH", 0, "GROUNDED",
            null, null, null, null, null, false, null, null
        );

        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).isEqualTo("Xin chào");
        assertThat(resp.getClarificationOptions()).isEmpty();
        assertThat(resp.getSources()).isEmpty();
    }
}
