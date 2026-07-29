package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.service.GeminiService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private static final String SYSTEM_PROMPT =
            "Bạn là trợ lý hỗ trợ của hệ thống Tutor Connect System (TCS) – nền tảng kết nối học viên và gia sư trực tuyến. "
            + "Hãy trả lời ngắn gọn, thân thiện, chính xác bằng tiếng Việt.\n\n"
            + "=== THÔNG TIN HỆ THỐNG ===\n"
            + "Tutor Connect System có các vai trò người dùng:\n"
            + "- Học viên (Student): tìm kiếm gia sư, đăng ký lớp học, quản lý hợp đồng, thanh toán, đánh giá gia sư.\n"
            + "- Gia sư (Tutor): tạo hồ sơ giảng dạy, đăng lớp dạy, quản lý lịch dạy, nhận thanh toán.\n"
            + "- Trung tâm (Center): quản lý gia sư thuộc trung tâm, đăng lớp học.\n"
            + "- Quản trị viên (Admin): quản lý toàn bộ hệ thống, duyệt hồ sơ, xử lý khiếu nại.\n\n"
            + "Các tính năng chính:\n"
            + "1. Marketplace: tìm kiếm gia sư/lớp học theo môn, khu vực, cấp học, giá.\n"
            + "2. Hồ sơ gia sư: thông tin cá nhân, bằng cấp, kinh nghiệm, môn dạy, đánh giá từ học viên.\n"
            + "3. Hợp đồng: tạo, ký kết hợp đồng giữa học viên và gia sư.\n"
            + "4. Thanh toán: quản lý giao dịch, nạp tiền, rút tiền.\n"
            + "5. Nhắn tin: giao tiếp giữa học viên và gia sư.\n"
            + "6. Hỗ trợ: tạo yêu cầu hỗ trợ (support ticket) khi gặp vấn đề.\n"
            + "7. Đánh giá: học viên đánh giá gia sư sau khóa học.\n"
            + "8. FAQ: câu hỏi thường gặp về hệ thống.\n\n"
            + "Quy trình cơ bản:\n"
            + "- Học viên đăng ký → tìm gia sư → liên hệ → tạo hợp đồng → học → thanh toán → đánh giá.\n"
            + "- Gia sư đăng ký → tạo hồ sơ → chờ duyệt → đăng lớp → nhận học viên → dạy → nhận thanh toán.\n\n"
            + "Nếu câu hỏi không liên quan đến học tập, gia sư, hoặc hệ thống TCS, "
            + "hãy trả lời lịch sự rằng bạn chỉ hỗ trợ các vấn đề liên quan đến Tutor Connect System. "
            + "Nếu không chắc chắn, hãy gợi ý người dùng tạo yêu cầu hỗ trợ để được đội ngũ giải đáp trực tiếp.";

    // ===== Gemini config =====
    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    // ===== Groq config (fallback) =====
    @Value("${app.groq.api-key:}")
    private String groqApiKey;

    @Value("${app.groq.model:llama-3.1-8b-instant}")
    private String groqModel;

    @Value("${app.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    // ===== Shared config =====
    @Value("${app.gemini.timeout-ms:8000}")
    private int timeoutMs;

    private RestClient geminiClient;
    private RestClient groqClient;

    private RestClient geminiClient() {
        if (geminiClient == null) {
            geminiClient = buildClient(geminiBaseUrl);
        }
        return geminiClient;
    }

    private RestClient groqClient() {
        if (groqClient == null) {
            groqClient = buildClient(groqBaseUrl);
        }
        return groqClient;
    }

    private RestClient buildClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<String> askQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            return Optional.empty();
        }

        // 1) Thu Gemini truoc
        if (StringUtils.hasText(geminiApiKey)) {
            Optional<String> geminiAnswer = askGemini(question);
            if (geminiAnswer.isPresent()) {
                return geminiAnswer;
            }
            log.info("Gemini API khong tra loi duoc, thu Groq fallback...");
        }

        // 2) Fallback sang Groq
        if (StringUtils.hasText(groqApiKey)) {
            Optional<String> groqAnswer = askGroq(question);
            if (groqAnswer.isPresent()) {
                return groqAnswer;
            }
        }

        log.debug("Khong co AI provider nao kha dung.");
        return Optional.empty();
    }

    // ==================== Gemini ====================

    private Optional<String> askGemini(String question) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                    ),
                    "contents", List.of(
                            Map.of("role", "user", "parts", List.of(Map.of("text", question)))
                    )
            );

            GeminiResponse response = geminiClient().post()
                    .uri("/models/{model}:generateContent", geminiModel)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", geminiApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            return extractGeminiAnswer(response);
        } catch (RestClientException ex) {
            log.warn("Goi Gemini API loi: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractGeminiAnswer(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }
        GeminiCandidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return Optional.empty();
        }
        String text = candidate.content().parts().get(0).text();
        return StringUtils.hasText(text) ? Optional.of(text.trim()) : Optional.empty();
    }

    // ==================== Groq (OpenAI-compatible) ====================

    private Optional<String> askGroq(String question) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", groqModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", question)
                    ),
                    "max_tokens", 1024,
                    "temperature", 0.7
            );

            GroqResponse response = groqClient().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GroqResponse.class);

            return extractGroqAnswer(response);
        } catch (RestClientException ex) {
            log.warn("Goi Groq API loi: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractGroqAnswer(GroqResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return Optional.empty();
        }
        GroqMessage message = response.choices().get(0).message();
        if (message == null) {
            return Optional.empty();
        }
        String text = message.content();
        return StringUtils.hasText(text) ? Optional.of(text.trim()) : Optional.empty();
    }

    // ==================== DTOs ====================

    // Gemini
    private record GeminiResponse(List<GeminiCandidate> candidates) {}
    private record GeminiCandidate(GeminiContent content) {}
    private record GeminiContent(List<GeminiPart> parts) {}
    private record GeminiPart(String text) {}

    // Groq (OpenAI-compatible)
    private record GroqResponse(List<GroqChoice> choices) {}
    private record GroqChoice(GroqMessage message) {}
    private record GroqMessage(String role, String content) {}
}
