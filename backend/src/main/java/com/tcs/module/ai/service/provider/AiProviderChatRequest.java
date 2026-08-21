package com.tcs.module.ai.service.provider;

public record AiProviderChatRequest(
    String systemPrompt,
    String userPrompt,
    int maxOutputTokens,
    double temperature,
    long timeoutMs
) {
    public AiProviderChatRequest {
        if (systemPrompt == null) systemPrompt = "";
        if (userPrompt == null) userPrompt = "";
        if (maxOutputTokens <= 0) {
            maxOutputTokens = 700;
        } else if (maxOutputTokens > 4096) {
            maxOutputTokens = 4096;
        }
        if (Double.isNaN(temperature) || temperature < 0.0) {
            temperature = 0.0;
        } else if (temperature > 2.0) {
            temperature = 2.0;
        }
        if (timeoutMs < 0L) {
            timeoutMs = 0L;
        }
    }

    public AiProviderChatRequest(String systemPrompt, String userPrompt, int maxOutputTokens, double temperature) {
        this(systemPrompt, userPrompt, maxOutputTokens, temperature, 0L);
    }
}
