package com.tcs.module.ai.service.provider;

public record AiProviderChatRequest(
    String systemPrompt,
    String userPrompt,
    int maxOutputTokens,
    double temperature
) {}
