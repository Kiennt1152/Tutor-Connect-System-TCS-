package com.tcs.module.ai.service.provider;

public record AiProviderChatResponse(
    String provider,
    String model,
    String content,
    int statusCode
) {}
