package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ConversationContextService {

    public record ConversationContext(
        AiDomain lastDomain,
        AiSubIntent lastSubIntent,
        Map<String, String> lastEntities,
        String lastQuery,
        List<Long> mentionedTutorIds,
        List<Long> mentionedClassIds,
        List<Long> mentionedFaqIds,
        Map<String, Integer> topicFrequency,
        String userGoal,
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
        saveContext(sessionId, domain, subIntent, entities, query, null, null, null, null);
    }

    public void saveContext(
        Long sessionId,
        AiDomain domain,
        AiSubIntent subIntent,
        Map<String, String> entities,
        String query,
        List<Long> tutorIds,
        List<Long> classIds,
        List<Long> faqIds,
        String userGoal
    ) {
        if (sessionId == null) return;

        ConversationContext existing = getContext(sessionId);
        Map<String, String> safeEntities = entities != null ? new HashMap<>(entities) : new HashMap<>();
        
        List<Long> accumulatedTutors = new ArrayList<>(existing != null ? existing.mentionedTutorIds() : List.of());
        if (tutorIds != null) {
            for (Long tid : tutorIds) {
                if (!accumulatedTutors.contains(tid)) accumulatedTutors.add(tid);
            }
        }

        List<Long> accumulatedClasses = new ArrayList<>(existing != null ? existing.mentionedClassIds() : List.of());
        if (classIds != null) {
            for (Long cid : classIds) {
                if (!accumulatedClasses.contains(cid)) accumulatedClasses.add(cid);
            }
        }

        List<Long> accumulatedFaqs = new ArrayList<>(existing != null ? existing.mentionedFaqIds() : List.of());
        if (faqIds != null) {
            for (Long fid : faqIds) {
                if (!accumulatedFaqs.contains(fid)) accumulatedFaqs.add(fid);
            }
        }

        Map<String, Integer> freqMap = new HashMap<>(existing != null ? existing.topicFrequency() : Map.of());
        if (subIntent != null) {
            freqMap.put(subIntent.name(), freqMap.getOrDefault(subIntent.name(), 0) + 1);
        }
        if (safeEntities.containsKey("subject")) {
            String sub = safeEntities.get("subject");
            freqMap.put("SUBJECT_" + sub, freqMap.getOrDefault("SUBJECT_" + sub, 0) + 1);
        }

        String effectiveGoal = userGoal != null && !userGoal.isBlank()
            ? userGoal
            : (existing != null ? existing.userGoal() : null);

        sessionContexts.put(sessionId, new ConversationContext(
            domain,
            subIntent,
            safeEntities,
            query,
            accumulatedTutors,
            accumulatedClasses,
            accumulatedFaqs,
            freqMap,
            effectiveGoal,
            Instant.now()
        ));
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
                normalized.contains("con o ") || normalized.contains("the o ") || normalized.contains("con tai ") ||
                normalized.contains("thay do") || normalized.contains("gia su do") || normalized.contains("lop do") ||
                normalized.contains("thay khac") || normalized.contains("nguoi khac");

        if (!isFollowUpPattern) {
            return new FollowUpResolution(false, null, null, currentEntities);
        }

        Map<String, String> mergedEntities = new HashMap<>(ctx.lastEntities());
        if (currentEntities != null) {
            mergedEntities.putAll(currentEntities);
        }

        // Attach referenced tutor or class id if available
        if (!ctx.mentionedTutorIds().isEmpty() && !mergedEntities.containsKey("tutorId")) {
            mergedEntities.put("lastMentionedTutorId", String.valueOf(ctx.mentionedTutorIds().get(ctx.mentionedTutorIds().size() - 1)));
        }
        if (!ctx.mentionedClassIds().isEmpty() && !mergedEntities.containsKey("classId")) {
            mergedEntities.put("lastMentionedClassId", String.valueOf(ctx.mentionedClassIds().get(ctx.mentionedClassIds().size() - 1)));
        }

        // Inherit Marketplace or Domain search
        if (ctx.lastDomain() == AiDomain.MARKETPLACE || ctx.lastDomain() == AiDomain.FINANCE_WALLET || ctx.lastDomain() == AiDomain.CONTRACT_REVIEW) {
            return new FollowUpResolution(true, ctx.lastDomain(), ctx.lastSubIntent(), mergedEntities);
        }

        return new FollowUpResolution(false, null, null, currentEntities);
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
