package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeminiChatClient implements AiChatProviderClient {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiChatClient(String apiKey, String baseUrl, String model, ObjectMapper objectMapper, long timeoutMs) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public String providerName() {
        return "Gemini";
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
            // Build Gemini native payload
            String fullPrompt = "System Prompt:\n" + request.systemPrompt() + "\n\nUser Prompt:\n" + request.userPrompt();
            
            String payload = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt)))),
                    "generationConfig", Map.of(
                        "temperature", request.temperature(),
                        "maxOutputTokens", request.maxOutputTokens()
                    )
            ));

            long effectiveTimeout = request.timeoutMs() > 0 ? request.timeoutMs() : 15000L;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "models/" + model + ":generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(effectiveTimeout))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                if (textNode.isMissingNode()) {
                    return new AiProviderChatResponse(providerName(), model, null, 500);
                }
                return new AiProviderChatResponse(providerName(), model, textNode.asText(), resp.statusCode());
            } else {
                log.warn("{} API error: Status {}", providerName(), resp.statusCode());
                return new AiProviderChatResponse(providerName(), model, null, resp.statusCode());
            }
        } catch (Exception e) {
            log.warn("{} API call failed: {}", providerName(), e.getMessage());
            return new AiProviderChatResponse(providerName(), model, null, 500);
        }
    }
}
