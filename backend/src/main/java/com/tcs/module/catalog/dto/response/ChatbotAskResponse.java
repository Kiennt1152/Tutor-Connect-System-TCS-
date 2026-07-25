package com.tcs.module.catalog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatbotAskResponse {

    /** true nếu tìm được FAQ khớp với câu hỏi, false nếu không tìm được (nên gợi ý tạo ticket). */
    private boolean matched;

    private Long faqId;
    private String question;
    private String answer;
    private String suggestion;

    /** true neu answer duoc sinh boi Gemini AI (khong phai FAQ da duyet). */
    private boolean aiGenerated;
}
