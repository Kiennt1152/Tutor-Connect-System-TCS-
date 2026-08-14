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
        boolean isFollowUp = lower.contains("r\u1ebb h\u01a1n") || lower.contains("g\u1ea7n h\u01a1n") || 
                             lower.contains("online") || lower.contains("n\u1eef") || 
                             lower.contains("nam") || lower.contains("kh\u00e1c") || 
                             lower.contains("th\u00eam") || lower.contains("cao h\u01a1n") || 
                             lower.contains("th\u1ea5p h\u01a1n");
        
        if (isFollowUp) {
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 6); i--) {
                AiChatMessage msg = history.get(i);
                if ("user".equals(msg.getRole())) {
                    IntentClassifier.IntentResult prevIntent = intentClassifier.classify(msg.getContent());
                    if (prevIntent.intent() == AiIntent.FIND_TUTOR || prevIntent.intent() == AiIntent.FIND_CLASS) {
                        return new RewriteResult(msg.getContent() + " v\u00e0 c\u00f3 t\u00ednh ch\u1ea5t: " + currentMessage, true, prevIntent.intent());
                    }
                }
            }
        }
        
        return new RewriteResult(currentMessage, false, currentIntent);
    }
}
