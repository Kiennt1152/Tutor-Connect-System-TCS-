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
    private long cooldownSeconds = 60L;
    
    @Value("${ai.provider.timeout-ms:15000}")
    private long timeoutMs = 15000L;

    @Value("${ai.provider.total-generation-deadline-ms:20000}")
    private long totalGenerationDeadlineMs = 20000L;

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
                if (providers.containsKey(name) && !executionOrder.contains(name)) {
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

    public void registerProvider(String name, AiChatProviderClient client) {
        providers.put(name.toLowerCase(), client);
        if (!executionOrder.contains(name.toLowerCase())) {
            executionOrder.add(name.toLowerCase());
        }
    }

    public void setExecutionOrder(List<String> order) {
        this.executionOrder = new ArrayList<>(order);
    }

    public void setTotalGenerationDeadlineMs(long deadlineMs) {
        this.totalGenerationDeadlineMs = deadlineMs;
    }

    public long getTotalGenerationDeadlineMs() {
        return this.totalGenerationDeadlineMs;
    }

    public void resetHealthState() {
        providerCooldowns.clear();
        providerDisabled.clear();
    }

    public boolean isProviderInCooldown(String providerName) {
        Long until = providerCooldowns.get(providerName.toLowerCase());
        return until != null && System.currentTimeMillis() < until;
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        long now = System.currentTimeMillis();

        for (String provider : executionOrder) {
            AiChatProviderClient client = providers.get(provider);
            boolean configured = client != null && client.isConfigured();
            boolean disabled = providerDisabled.getOrDefault(provider, false);
            Long cooldownUntil = providerCooldowns.get(provider);
            boolean onCooldown = cooldownUntil != null && now < cooldownUntil;
            boolean available = configured && !disabled && !onCooldown;

            Map<String, Object> pInfo = new java.util.LinkedHashMap<>();
            pInfo.put("available", available);
            pInfo.put("configured", configured);
            pInfo.put("disabled", disabled);
            pInfo.put("onCooldown", onCooldown);
            pInfo.put("cooldownRemainingSeconds", onCooldown ? Math.max(0, (cooldownUntil - now) / 1000) : 0);
            pInfo.put("priority", executionOrder.indexOf(provider) + 1);

            status.put(provider, pInfo);
        }

        Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("providers", status);
        root.put("defaultTimeoutMs", timeoutMs);
        root.put("totalGenerationDeadlineMs", totalGenerationDeadlineMs);
        return root;
    }

    public AiProviderChatResponse chat(AiProviderChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        for (String providerName : executionOrder) {
            long now = System.currentTimeMillis();
            long elapsed = now - startTime;
            long remainingBudget = totalGenerationDeadlineMs - elapsed;

            if (remainingBudget <= 0) {
                log.warn("Total generation deadline reached ({}ms). Stopping provider failover after {}ms.", totalGenerationDeadlineMs, elapsed);
                break;
            }

            AiChatProviderClient client = providers.get(providerName);
            if (client == null) continue;
            
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
            
            long clientBaseTimeout = request.timeoutMs() > 0 ? request.timeoutMs() : timeoutMs;
            long effectiveTimeout = Math.min(clientBaseTimeout, remainingBudget);

            AiProviderChatRequest boundedRequest = new AiProviderChatRequest(
                request.systemPrompt(),
                request.userPrompt(),
                request.maxOutputTokens(),
                request.temperature(),
                effectiveTimeout
            );

            log.info("Routing chat request to provider: {} with timeout {}ms (remaining budget: {}ms)", providerName, effectiveTimeout, remainingBudget);
            AiProviderChatResponse response = client.chat(boundedRequest);
            
            if (response.statusCode() == 200 && response.content() != null && !response.content().isBlank()) {
                return response;
            }
            
            // Handle failures
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                log.error("Provider {} returned {}. Marking as disabled for runtime.", providerName, response.statusCode());
                providerDisabled.put(providerName, true);
            } else if (response.statusCode() == 429) {
                log.warn("Provider {} rate limited (429). Setting cooldown for {} seconds.", providerName, cooldownSeconds);
                providerCooldowns.put(providerName, System.currentTimeMillis() + (cooldownSeconds * 1000));
            } else {
                log.warn("Provider {} failed with status {}. Setting short cooldown.", providerName, response.statusCode());
                providerCooldowns.put(providerName, System.currentTimeMillis() + 15000L); // 15s cooldown for 5xx/timeout
            }
        }
        
        log.error("All AI providers failed, expired deadline, or are in cooldown/disabled.");
        return null;
    }
}
