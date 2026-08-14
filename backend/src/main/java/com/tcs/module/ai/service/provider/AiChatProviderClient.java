package com.tcs.module.ai.service.provider;

public interface AiChatProviderClient {
    String providerName();
    boolean isConfigured();
    AiProviderChatResponse chat(AiProviderChatRequest request);
}
