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

public abstract class OpenAiCompatibleChatClient implements AiChatProviderClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OpenAiCompatibleChatClient.class);

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
