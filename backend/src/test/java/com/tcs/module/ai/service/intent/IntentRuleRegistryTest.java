package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
@SpringBootTest
class IntentRuleRegistryTest {

    @Autowired
    private IntentRuleRegistry registry;

    @Test
    @DisplayName("Marketplace Rule: Correctly identifies tutor search")
    void testMarketplaceTutorSearchRule() {
        ClassificationDetail result = registry.evaluate("tim gia su toan", "tìm gia sư toán");
        
        assertThat(result).isNotNull();
        assertThat(result.domain()).isEqualTo(AiDomain.MARKETPLACE);
        assertThat(result.subIntent()).isEqualTo(AiSubIntent.FIND_TUTOR);
    }

    @Test
    @DisplayName("Finance Rule: Correctly identifies wallet topup")
    void testFinanceRule() {
        ClassificationDetail result = registry.evaluate("nap tien vao vi", "nạp tiền vào ví");
        
        assertThat(result).isNotNull();
        assertThat(result.domain()).isEqualTo(AiDomain.FINANCE_WALLET);
        assertThat(result.subIntent()).isEqualTo(AiSubIntent.WALLET_TOPUP);
    }

    @Test
    @DisplayName("Conversation Safety Rule: Correctly identifies greetings with priority 0")
    void testConversationSafetyGreeting() {
        ClassificationDetail result = registry.evaluate("xin chao", "xin chào");
        
        assertThat(result).isNotNull();
        assertThat(result.domain()).isEqualTo(AiDomain.CONVERSATION_SAFETY);
        assertThat(result.subIntent()).isEqualTo(AiSubIntent.GREETING);
    }

    @Test
    @DisplayName("Fallback: Returns null when no domain rules match")
    void testFallbackNoMatch() {
        ClassificationDetail result = registry.evaluate("may bay bay cao bao nhieu", "máy bay bay cao bao nhiêu");
        
        assertThat(result).isNull();
    }
}
