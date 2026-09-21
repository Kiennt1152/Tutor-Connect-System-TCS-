package com.tcs.module.ai.service;

import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.enums.AiIntent;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiQueryRewriteService {

    private final IntentClassifier intentClassifier;

    public record RewriteResult(String rewrittenQuery, boolean isFollowUp, AiIntent inferredIntent) {}

    public RewriteResult rewriteQuery(List<AiChatMessage> history, String currentMessage, AiIntent currentIntent) {
        if (history == null || history.isEmpty()) {
            return new RewriteResult(currentMessage, false, currentIntent);
        }

        String lower = currentMessage.toLowerCase(Locale.ROOT);
        boolean isFollowUp = lower.contains("rẻ hơn") || lower.contains("gần hơn") || 
                             lower.contains("online") || lower.contains("nữ") || 
                             lower.contains("nam") || lower.contains("khác") || 
                             lower.contains("thêm") || lower.contains("cao hơn") || 
                             lower.contains("thấp hơn") ||
                             lower.contains("học thử") || lower.contains("buổi đầu") ||
                             lower.contains("vậy có") || lower.contains("thế có") ||
                             lower.contains("được không") || lower.contains("đổi gia sư");
        
        if (isFollowUp) {
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 6); i--) {
                AiChatMessage msg = history.get(i);
                if ("user".equals(msg.getRole())) {
                    IntentClassifier.IntentResult prevIntent = intentClassifier.classify(msg.getContent());
                    return new RewriteResult(msg.getContent() + " và câu hỏi tiếp nối: " + currentMessage, true, currentIntent);
                }
            }
        }
        
        return new RewriteResult(currentMessage, false, currentIntent);
    }
}
