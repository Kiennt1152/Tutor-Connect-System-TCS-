package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ConversationContextService {

    public record ConversationContext(
        AiDomain lastDomain,
        AiSubIntent lastSubIntent,
        Map<String, String> lastEntities,
        String lastQuery,
        Instant lastUpdated
    ) {}

    public record FollowUpResolution(
        boolean isFollowUp,
        AiDomain domain,
        AiSubIntent subIntent,
        Map<String, String> resolvedEntities
    ) {}

    private final Map<Long, ConversationContext> sessionContexts = new ConcurrentHashMap<>();
    private final Map<Long, Integer> fallbackCountMap = new ConcurrentHashMap<>();
    private static final Duration CONTEXT_TTL = Duration.ofMinutes(15);

    public void saveContext(Long sessionId, AiDomain domain, AiSubIntent subIntent, Map<String, String> entities, String query) {
        if (sessionId == null) return;
        Map<String, String> safeEntities = entities != null ? new HashMap<>(entities) : new HashMap<>();
        sessionContexts.put(sessionId, new ConversationContext(domain, subIntent, safeEntities, query, Instant.now()));
    }

    public ConversationContext getContext(Long sessionId) {
        if (sessionId == null) return null;
        ConversationContext ctx = sessionContexts.get(sessionId);
        if (ctx != null && Duration.between(ctx.lastUpdated(), Instant.now()).compareTo(CONTEXT_TTL) <= 0) {
            return ctx;
        }
        if (ctx != null) {
            sessionContexts.remove(sessionId);
        }
        return null;
    }

    public FollowUpResolution resolveFollowUp(Long sessionId, String currentQuery, Map<String, String> currentEntities) {
        ConversationContext ctx = getContext(sessionId);
        if (ctx == null || currentQuery == null || currentQuery.isBlank()) {
            return new FollowUpResolution(false, null, null, currentEntities);
        }

        String lower = currentQuery.toLowerCase(Locale.ROOT).trim();
        String normalized = VietnameseTextNormalizer.removeDiacritics(lower);

        boolean isFollowUpPattern = normalized.startsWith("con ") || normalized.startsWith("the con ") ||
                normalized.startsWith("vay con ") || normalized.contains("thi sao") ||
                normalized.contains("con o ") || normalized.contains("the o ") || normalized.contains("con tai ");

        if (!isFollowUpPattern) {
            return new FollowUpResolution(false, null, null, currentEntities);
        }

        Map<String, String> mergedEntities = new HashMap<>(ctx.lastEntities());
        if (currentEntities != null) {
            mergedEntities.putAll(currentEntities);
        }

        // Case 1: Inherit Weather Query with new location
        if (ctx.lastSubIntent() == AiSubIntent.WEATHER_QUERY) {
            String newLocation = extractLocation(lower, normalized);
            if (newLocation != null) {
                mergedEntities.put("location", newLocation);
            }
            return new FollowUpResolution(true, AiDomain.OPEN_DOMAIN, AiSubIntent.WEATHER_QUERY, mergedEntities);
        }

        // Case 2: Inherit Marketplace Tutor or Class Search
        if (ctx.lastDomain() == AiDomain.MARKETPLACE) {
            return new FollowUpResolution(true, ctx.lastDomain(), ctx.lastSubIntent(), mergedEntities);
        }

        // Case 3: Inherit General Open Domain
        if (ctx.lastDomain() == AiDomain.OPEN_DOMAIN) {
            return new FollowUpResolution(true, ctx.lastDomain(), ctx.lastSubIntent(), mergedEntities);
        }

        return new FollowUpResolution(false, null, null, currentEntities);
    }

    private String extractLocation(String lower, String normalized) {
        if (normalized.contains("ha noi")) return "Hà Nội";
        if (normalized.contains("ho chi minh") || normalized.contains("hcm") || normalized.contains("sai gon")) return "TP.HCM";
        if (normalized.contains("da nang")) return "Đà Nẵng";
        if (normalized.contains("hai phong")) return "Hải Phòng";
        if (normalized.contains("can tho")) return "Cần Thơ";
        if (normalized.contains("hue")) return "Huế";
        if (normalized.contains("nha trang")) return "Nha Trang";
        if (normalized.contains("binh duong")) return "Bình Dương";
        if (normalized.contains("dong nai")) return "Đồng Nai";
        return null;
    }

    public int incrementFallbackCount(Long sessionId) {
        if (sessionId == null) return 0;
        return fallbackCountMap.merge(sessionId, 1, Integer::sum);
    }

    public void resetFallbackCount(Long sessionId) {
        if (sessionId != null) {
            fallbackCountMap.remove(sessionId);
        }
    }

    public int getFallbackCount(Long sessionId) {
        if (sessionId == null) return 0;
        return fallbackCountMap.getOrDefault(sessionId, 0);
    }

    public void clear() {
        sessionContexts.clear();
        fallbackCountMap.clear();
    }
}
