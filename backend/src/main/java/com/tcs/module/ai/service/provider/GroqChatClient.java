package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GroqChatClient extends OpenAiCompatibleChatClient {

    public GroqChatClient(String apiKey, String baseUrl, String model, ObjectMapper objectMapper, long timeoutMs) {
        super(apiKey, baseUrl, model, objectMapper, timeoutMs);
    }

    @Override
    public String providerName() {
        return "Groq";
    }
}
