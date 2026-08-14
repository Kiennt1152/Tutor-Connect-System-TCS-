package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DeepSeekChatClient extends OpenAiCompatibleChatClient {

    public DeepSeekChatClient(String apiKey, String baseUrl, String model, ObjectMapper objectMapper, long timeoutMs) {
        super(apiKey, baseUrl, model, objectMapper, timeoutMs);
    }

    @Override
    public String providerName() {
        return "DeepSeek";
    }
}
