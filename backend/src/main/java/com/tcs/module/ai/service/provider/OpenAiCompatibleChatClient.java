package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================================
 * [UC-65] KẾT NỐI MÔ HÌNH TƯƠNG THÍCH OPENAI (OPENAI COMPATIBLE CLIENT)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Lớp kết nối dùng chung cho tất cả các nhà cung cấp hỗ trợ giao thức chuẩn OpenAI Chat Completions.
 * 
 * Chức năng chính:
 *   1. Tái sử dụng mã nguồn: Đóng gói logic HTTP RestClient, Header Bearer và JSON Mapping.
 *   2. Bắt lỗi đồng bộ: Chuẩn hóa các mã lỗi HTTP 401, 429, 500 thành ngoại lệ nghiệp vụ.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận endpoint URL, Model ID và API Key tương ứng.
 *   - Bước 2: Thực thi cuộc gọi REST chuẩn và trả về đối tượng phản hồi.
 *  * ============================================================================
 */
@Slf4j
public abstract class OpenAiCompatibleChatClient implements AiChatProviderClient {

    protected final String apiKey;
    protected final String baseUrl;
    protected final String model;
    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;

    public OpenAiCompatibleChatClient(String apiKey, String baseUrl, String model, ObjectMapper objectMapper, long timeoutMs) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AiProviderChatResponse chat(AiProviderChatRequest request) {
        if (!isConfigured()) {
            return new AiProviderChatResponse(providerName(), model, null, 401);
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
            messages.add(Map.of("role", "user", "content", request.userPrompt()));

            String payload = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", request.temperature(),
                    "max_tokens", request.maxOutputTokens()
            ));

            long effectiveTimeout = request.timeoutMs() > 0 ? request.timeoutMs() : 15000L;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(effectiveTimeout))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                String text = root.path("choices").path(0).path("message").path("content").asText();
                return new AiProviderChatResponse(providerName(), model, text, resp.statusCode());
            } else {
                log.warn("{} API error: Status {}", providerName(), resp.statusCode());
                return new AiProviderChatResponse(providerName(), model, null, resp.statusCode());
            }
        } catch (Exception e) {
            log.warn("{} API call failed: {}", providerName(), e.getMessage());
            return new AiProviderChatResponse(providerName(), model, null, 500); // 500 for generic internal/network error
        }
    }
}
