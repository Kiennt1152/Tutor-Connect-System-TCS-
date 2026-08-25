package com.tcs.module.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotAskResponse {

    /** true nếu tìm được FAQ khớp với câu hỏi, false nếu không tìm được (nên gợi ý tạo ticket). */
    private boolean matched;

    private Long faqId;
    private String question;
    private String answer;
    private String suggestion;

    /** true neu answer duoc sinh boi Gemini AI (khong phai FAQ da duyet). */
    private boolean aiGenerated;

    public boolean isMatched() { return matched; }
    public Long getFaqId() { return faqId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getSuggestion() { return suggestion; }
    public boolean isAiGenerated() { return aiGenerated; }

    public static ChatbotAskResponseBuilder builder() {
        return new ChatbotAskResponseBuilder();
    }

    public static class ChatbotAskResponseBuilder {
        private boolean matched;
        private Long faqId;
        private String question;
        private String answer;
        private String suggestion;
        private boolean aiGenerated;

        public ChatbotAskResponseBuilder matched(boolean matched) { this.matched = matched; return this; }
        public ChatbotAskResponseBuilder faqId(Long faqId) { this.faqId = faqId; return this; }
        public ChatbotAskResponseBuilder question(String question) { this.question = question; return this; }
        public ChatbotAskResponseBuilder answer(String answer) { this.answer = answer; return this; }
        public ChatbotAskResponseBuilder suggestion(String suggestion) { this.suggestion = suggestion; return this; }
        public ChatbotAskResponseBuilder aiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; return this; }
        public ChatbotAskResponse build() { return new ChatbotAskResponse(matched, faqId, question, answer, suggestion, aiGenerated); }
    }
}
