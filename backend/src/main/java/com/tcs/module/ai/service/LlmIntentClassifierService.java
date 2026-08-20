package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.provider.AiProviderChatRequest;
import com.tcs.module.ai.service.provider.AiProviderChatResponse;
import com.tcs.module.ai.service.provider.AiProviderRouter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * LLM-based Intent Classifier (Hybrid Tier 2.5).
 *
 * When the keyword-based IntentClassifier returns low confidence (< 0.85),
 * this service calls the LLM with a lightweight structured prompt to classify
 * the user's intent by MEANING rather than keyword matching.
 *
 * This solves the "ép chữ" (forced keyword) problem where users express
 * the same intent using creative, informal, or unexpected phrasing.
 *
 * Cost: ~50-200 tokens per classification call (very lightweight).
 * Latency: 200-500ms via Groq/Cerebras (fast inference providers).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmIntentClassifierService {

    private final AiProviderRouter aiProviderRouter;
    private final ObjectMapper objectMapper;

    private static final String CLASSIFICATION_SYSTEM_PROMPT =
        "Bạn là bộ phân loại ý định (Intent Classifier) cho hệ thống gia sư TCS. " +
        "Nhiệm vụ: Phân loại câu hỏi của người dùng vào ĐÚNG 1 nhóm. " +
        "Trả lời CHỈ bằng JSON, KHÔNG giải thích.";

    private static final String CLASSIFICATION_USER_PROMPT_TEMPLATE =
        "Phân loại câu hỏi sau vào ĐÚNG 1 nhóm:\n\n" +
        "Các nhóm:\n" +
        "- FIND_TUTOR: Tìm, thuê, hỏi về gia sư, người dạy, người kèm, cô/thầy dạy kèm\n" +
        "- FIND_CLASS: Tìm lớp học, lớp đang tuyển, lớp cần gia sư\n" +
        "- FAQ_SUPPORT: Hỏi chính sách, quy trình, hướng dẫn sử dụng hệ thống TCS, FAQ\n" +
        "- FINANCE: Nạp tiền, rút tiền, học phí, ví tiền, thanh toán, phí sàn, escrow\n" +
        "- CONTRACT: Hợp đồng, ký hợp đồng, OTP ký kết\n" +
        "- TICKET: Khiếu nại, tranh chấp, báo cáo vi phạm, hỗ trợ kỹ thuật\n" +
        "- IDENTITY: Đăng ký, đăng nhập, mật khẩu, tài khoản, OTP xác thực\n" +
        "- VERIFICATION: Xác minh hồ sơ, bằng cấp, CCCD, duyệt hồ sơ\n" +
        "- PROFILE: Cập nhật hồ sơ, thông tin cá nhân, avatar\n" +
        "- TUTOR_OPS: Lịch dạy, điểm danh, quản lý lớp (dành cho gia sư)\n" +
        "- CENTER_OPS: Quản lý trung tâm, tuyển dụng gia sư (dành cho trung tâm)\n" +
        "- ADMIN: Quản trị hệ thống, thống kê, dashboard (dành cho admin)\n" +
        "- AI_TUTORING: Giải bài tập, hỏi kiến thức học tập (Toán, Lý, Hóa, Anh...)\n" +
        "- CHITCHAT: Tán gẫu, giải trí, hỏi vui, khen, chê, không liên quan giáo dục\n" +
        "- MATH: Tính toán số học đơn giản (1+1, 5*3...)\n" +
        "- WEATHER: Hỏi thời tiết\n" +
        "- TIME: Hỏi ngày giờ\n" +
        "- OUT_OF_SCOPE: Hoàn toàn không liên quan đến bất kỳ nhóm nào ở trên\n\n" +
        "Câu hỏi: \"%s\"\n\n" +
        "Trả lời JSON (KHÔNG markdown, KHÔNG giải thích):\n" +
        "{\"intent\": \"<TÊN_NHÓM>\", \"confidence\": <0.0-1.0>}";

    /**
     * Classify user intent using LLM.
     * Returns null if LLM is unavailable or returns unparseable result.
     */
    public IntentClassifier.ClassificationDetail classifyWithLlm(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }

        try {
            String userPrompt = String.format(CLASSIFICATION_USER_PROMPT_TEMPLATE, userMessage.replace("\"", "'"));

            AiProviderChatRequest request = new AiProviderChatRequest(
                CLASSIFICATION_SYSTEM_PROMPT,
                userPrompt,
                100,   // Very small token limit — we only need a JSON snippet
                0.0    // Temperature 0 for deterministic classification
            );

            AiProviderChatResponse response = aiProviderRouter.chat(request);
            if (response == null || response.content() == null || response.content().isBlank()) {
                log.debug("LLM Intent Classifier: No response from providers.");
                return null;
            }

            return parseLlmClassification(response.content());
        } catch (Exception e) {
            log.warn("LLM Intent Classifier failed: {}", e.getMessage());
            return null;
        }
    }

    private IntentClassifier.ClassificationDetail parseLlmClassification(String rawResponse) {
        try {
            // Strip markdown code fences if present
            String cleaned = rawResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "").trim();
            }

            JsonNode json = objectMapper.readTree(cleaned);
            String intentStr = json.has("intent") ? json.get("intent").asText("OUT_OF_SCOPE") : "OUT_OF_SCOPE";
            double confidence = json.has("confidence") ? json.get("confidence").asDouble(0.5) : 0.5;

            return mapToClassificationDetail(intentStr.toUpperCase(), confidence);
        } catch (Exception e) {
            log.debug("LLM Intent Classifier: Failed to parse JSON response: {}", rawResponse);
            return null;
        }
    }

    private IntentClassifier.ClassificationDetail mapToClassificationDetail(String intentStr, double confidence) {
        return switch (intentStr) {
            case "FIND_TUTOR" -> new IntentClassifier.ClassificationDetail(
                AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR, AiIntent.FIND_TUTOR, confidence, "/tim-gia-su");
            case "FIND_CLASS" -> new IntentClassifier.ClassificationDetail(
                AiDomain.MARKETPLACE, AiSubIntent.FIND_CLASS, AiIntent.FIND_CLASS, confidence, "/lop-hoc");
            case "FAQ_SUPPORT" -> new IntentClassifier.ClassificationDetail(
                AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, confidence, "/help");
            case "FINANCE" -> new IntentClassifier.ClassificationDetail(
                AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_VIEW, AiIntent.PAYMENT_SUPPORT, confidence, "/finance");
            case "CONTRACT" -> new IntentClassifier.ClassificationDetail(
                AiDomain.CONTRACT_REVIEW, AiSubIntent.CONTRACT_SIGN_OTP, AiIntent.FAQ_SUPPORT, confidence, "/contracts");
            case "TICKET" -> new IntentClassifier.ClassificationDetail(
                AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_STATUS, AiIntent.TICKET_SUPPORT, confidence, "/support/tickets");
            case "IDENTITY" -> new IntentClassifier.ClassificationDetail(
                AiDomain.IDENTITY_AUTH, AiSubIntent.LOGIN_HELP, AiIntent.FAQ_SUPPORT, confidence, "/login");
            case "VERIFICATION" -> new IntentClassifier.ClassificationDetail(
                AiDomain.VERIFICATION, AiSubIntent.TUTOR_VERIFICATION_HELP, AiIntent.TUTOR_VERIFICATION, confidence, "/profile");
            case "PROFILE" -> new IntentClassifier.ClassificationDetail(
                AiDomain.PROFILE_GUARDIAN, AiSubIntent.PROFILE_UPDATE_HELP, AiIntent.FAQ_SUPPORT, confidence, "/profile");
            case "TUTOR_OPS" -> new IntentClassifier.ClassificationDetail(
                AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SCHEDULE_VIEW, AiIntent.FAQ_SUPPORT, confidence, "/tutor/schedule");
            case "CENTER_OPS" -> new IntentClassifier.ClassificationDetail(
                AiDomain.CENTER_OPS, AiSubIntent.CENTER_TUTOR_MANAGEMENT, AiIntent.CENTER_MANAGEMENT, confidence, "/center");
            case "ADMIN" -> new IntentClassifier.ClassificationDetail(
                AiDomain.PLATFORM_ADMIN, AiSubIntent.PLATFORM_STATS, AiIntent.PLATFORM_STATS, confidence, "/platform");
            case "AI_TUTORING" -> new IntentClassifier.ClassificationDetail(
                AiDomain.OPEN_DOMAIN, AiSubIntent.GENERAL_KNOWLEDGE, AiIntent.AI_TUTORING, confidence, null);
            case "CHITCHAT", "ENTERTAINMENT" -> new IntentClassifier.ClassificationDetail(
                AiDomain.OPEN_DOMAIN, AiSubIntent.ENTERTAINMENT, AiIntent.OUT_OF_SCOPE, confidence, null);
            case "MATH" -> new IntentClassifier.ClassificationDetail(
                AiDomain.OPEN_DOMAIN, AiSubIntent.MATH_CALCULATION, AiIntent.OUT_OF_SCOPE, confidence, null);
            case "WEATHER" -> new IntentClassifier.ClassificationDetail(
                AiDomain.OPEN_DOMAIN, AiSubIntent.WEATHER_QUERY, AiIntent.OUT_OF_SCOPE, confidence, null);
            case "TIME" -> new IntentClassifier.ClassificationDetail(
                AiDomain.OPEN_DOMAIN, AiSubIntent.TIME_DATE_QUERY, AiIntent.OUT_OF_SCOPE, confidence, null);
            default -> new IntentClassifier.ClassificationDetail(
                AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, confidence, null);
        };
    }
}
