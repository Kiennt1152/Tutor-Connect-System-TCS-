package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class AiProviderRouter {

    @Value("${ai.chat.provider-order:groq,cerebras,deepseek,gemini}")
    private String providerOrderConfig;
    
    @Value("${ai.provider.cooldown-seconds:60}")
    private long cooldownSeconds;
    
    @Value("${ai.provider.timeout-ms:15000}")
    private long timeoutMs;

    // Gemini
    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;
    @Value("${ai.gemini.chat-model:gemini-2.0-flash}")
    private String geminiModel;
    @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    // Cerebras
    @Value("${ai.cerebras.api-key:}")
    private String cerebrasApiKey;
    @Value("${ai.cerebras.model:llama-3.3-70b}")
    private String cerebrasModel;
    @Value("${ai.cerebras.base-url:https://api.cerebras.ai/v1}")
    private String cerebrasBaseUrl;

    // DeepSeek
    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;
    @Value("${ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;
    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    // Groq
    @Value("${ai.groq.api-key:}")
    private String groqApiKey;
    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;
    @Value("${ai.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    private final ObjectMapper objectMapper;
    private final Map<String, AiChatProviderClient> providers = new ConcurrentHashMap<>();
    private final Map<String, Long> providerCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Boolean> providerDisabled = new ConcurrentHashMap<>();
    private List<String> executionOrder = new ArrayList<>();

    public AiProviderRouter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Init clients
        providers.put("gemini", new GeminiChatClient(geminiApiKey, geminiBaseUrl, geminiModel, objectMapper, timeoutMs));
        providers.put("cerebras", new CerebrasChatClient(cerebrasApiKey, cerebrasBaseUrl, cerebrasModel, objectMapper, timeoutMs));
        providers.put("deepseek", new DeepSeekChatClient(deepseekApiKey, deepseekBaseUrl, deepseekModel, objectMapper, timeoutMs));
        providers.put("groq", new GroqChatClient(groqApiKey, groqBaseUrl, groqModel, objectMapper, timeoutMs));

        // Parse order
        if (providerOrderConfig != null && !providerOrderConfig.isBlank()) {
            for (String p : providerOrderConfig.split(",")) {
                String name = p.trim().toLowerCase();
                if (providers.containsKey(name)) {
                    executionOrder.add(name);
                }
            }
        }
        
        // Add remaining if not in config
        for (String key : providers.keySet()) {
            if (!executionOrder.contains(key)) executionOrder.add(key);
        }
        
        log.info("AI Chat Provider Route Order: {}", executionOrder);
    }

    public AiProviderChatResponse chat(AiProviderChatRequest request) {
        long now = System.currentTimeMillis();
        
        for (String providerName : executionOrder) {
            AiChatProviderClient client = providers.get(providerName);
            
            if (!client.isConfigured()) {
                log.debug("Provider {} is not configured (missing key). Skipping.", providerName);
                continue;
            }
            
            if (providerDisabled.getOrDefault(providerName, false)) {
                log.debug("Provider {} is marked as disabled (e.g. 401 Unauthorized). Skipping.", providerName);
                continue;
            }
            
            Long cooldownUntil = providerCooldowns.get(providerName);
            if (cooldownUntil != null && now < cooldownUntil) {
                log.warn("Provider {} is in cooldown for {} more seconds. Skipping.", providerName, (cooldownUntil - now) / 1000);
                continue;
            }
            
            log.info("Routing chat request to provider: {}", providerName);
            AiProviderChatResponse response = client.chat(request);
            
            if (response.statusCode() == 200 && response.content() != null && !response.content().isBlank()) {
                return response;
            }
            
            // Handle failures
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                log.error("Provider {} returned {}. Marking as disabled for runtime.", providerName, response.statusCode());
                providerDisabled.put(providerName, true);
            } else if (response.statusCode() == 429) {
                log.warn("Provider {} rate limited (429). Setting cooldown for {} seconds.", providerName, cooldownSeconds);
                providerCooldowns.put(providerName, now + (cooldownSeconds * 1000));
            } else {
                log.warn("Provider {} failed with status {}. Setting short cooldown.", providerName, response.statusCode());
                providerCooldowns.put(providerName, now + (15000)); // 15s cooldown for 5xx
            }
        }
        
        log.error("All AI providers failed or are in cooldown/disabled.");
        return null;
    }
}
