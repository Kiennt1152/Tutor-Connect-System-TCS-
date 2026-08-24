package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

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
        
        // O(1) deduplicated accumulation with LinkedHashSet
        Set<Long> tutorSet = new LinkedHashSet<>(existing != null ? existing.mentionedTutorIds() : List.of());
        if (tutorIds != null) tutorSet.addAll(tutorIds);

        Set<Long> classSet = new LinkedHashSet<>(existing != null ? existing.mentionedClassIds() : List.of());
        if (classIds != null) classSet.addAll(classIds);

        Set<Long> faqSet = new LinkedHashSet<>(existing != null ? existing.mentionedFaqIds() : List.of());
        if (faqIds != null) faqSet.addAll(faqIds);

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
            new ArrayList<>(tutorSet),
            new ArrayList<>(classSet),
            new ArrayList<>(faqSet),
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

    public String resolveFollowUpQuery(Long sessionId, String currentQuery) {
        ConversationContext ctx = getContext(sessionId);
        if (ctx == null || currentQuery == null || currentQuery.isBlank() || ctx.lastQuery() == null) {
            return currentQuery;
        }

        String lower = currentQuery.toLowerCase(Locale.ROOT).trim();
        String normalized = VietnameseTextNormalizer.removeDiacritics(lower);

        boolean isFollowUp = normalized.startsWith("con ") || normalized.startsWith("the con ") ||
                normalized.startsWith("vay con ") || normalized.contains("thi sao") ||
                normalized.contains("con o ") || normalized.contains("the o ") || normalized.contains("con tai ") ||
                normalized.contains("thay do") || normalized.contains("gia su do") || normalized.contains("lop do") ||
                normalized.contains("thay khac") || normalized.contains("nguoi khac") ||
                normalized.contains("gia bao nhieu") || normalized.contains("gia mot buoi") || normalized.contains("gia 1 buoi") ||
                normalized.contains("hoc phi bao nhieu") || normalized.contains("chi phi bao nhieu") || normalized.contains("bao nhieu mot buoi") ||
                normalized.contains("bao nhieu 1 buoi") || normalized.equals("gia bao nhieu") || normalized.equals("hoc phi bao nhieu") ||
                normalized.startsWith("gia ") || normalized.startsWith("hoc phi ") || normalized.startsWith("chi phi ");

        if (!isFollowUp) {
            return currentQuery;
        }

        // Expand query with action verb and inherited entities from previous turn
        StringBuilder expanded = new StringBuilder(currentQuery);

        boolean alreadyHasRole = normalized.contains("gia su") || normalized.contains("giao vien") ||
                normalized.contains("thay giao") || normalized.contains("co giao") || normalized.contains("thay co");
        boolean alreadyHasClass = normalized.contains("lop hoc") || normalized.contains("khoa hoc") ||
                normalized.startsWith("lop ") || normalized.contains(" lop ") || normalized.endsWith(" lop");

        boolean currentHasSubject = hasAnySubject(normalized);
        boolean currentHasGrade = hasAnyGrade(normalized);

        // 1. Pricing inquiries (Inherit subject & grade if omitted)
        if (normalized.contains("gia") || normalized.contains("hoc phi") || normalized.contains("chi phi") || normalized.contains("bao nhieu")) {
            if (ctx.lastEntities() != null) {
                if (!currentHasSubject && ctx.lastEntities().containsKey("subject")) {
                    expanded.append(" môn ").append(ctx.lastEntities().get("subject"));
                }
                if (!currentHasGrade && ctx.lastEntities().containsKey("grade")) {
                    expanded.append(" lớp ").append(ctx.lastEntities().get("grade"));
                }
            }
            return expanded.toString().trim();
        }

        // 2. Subject / Grade Switch Follow-up ("còn toán thì sao?", "thế còn môn lý?", "còn lớp 10?")
        if (normalized.startsWith("con ") || normalized.startsWith("the con ") || normalized.startsWith("vay con ") || normalized.contains("thi sao")) {
            if (ctx.lastSubIntent() == AiSubIntent.FIND_TUTOR || ctx.lastDomain() == AiDomain.MARKETPLACE) {
                expanded.setLength(0);
                expanded.append("Tìm gia sư ");
                if (currentHasSubject) {
                    expanded.append(currentQuery);
                } else if (ctx.lastEntities() != null && ctx.lastEntities().containsKey("subject")) {
                    expanded.append("môn ").append(ctx.lastEntities().get("subject"));
                }
                if (!currentHasGrade && ctx.lastEntities() != null && ctx.lastEntities().containsKey("grade")) {
                    expanded.append(" lớp ").append(ctx.lastEntities().get("grade"));
                }
                if (ctx.lastEntities() != null && ctx.lastEntities().containsKey("location") && !normalized.contains(ctx.lastEntities().get("location").toLowerCase(Locale.ROOT))) {
                    expanded.append(" tại ").append(ctx.lastEntities().get("location"));
                }
                return expanded.toString().trim();
            } else if (ctx.lastSubIntent() == AiSubIntent.FIND_CLASS) {
                expanded.setLength(0);
                expanded.append("Tìm lớp ");
                if (currentHasSubject) {
                    expanded.append(currentQuery);
                } else if (ctx.lastEntities() != null && ctx.lastEntities().containsKey("subject")) {
                    expanded.append("môn ").append(ctx.lastEntities().get("subject"));
                }
                if (!currentHasGrade && ctx.lastEntities() != null && ctx.lastEntities().containsKey("grade")) {
                    expanded.append(" lớp ").append(ctx.lastEntities().get("grade"));
                }
                return expanded.toString().trim();
            }
        }

        if ((ctx.lastSubIntent() == AiSubIntent.FIND_TUTOR || ctx.lastDomain() == AiDomain.MARKETPLACE) &&
            !alreadyHasRole && !alreadyHasClass) {
            expanded.insert(0, "Tìm gia sư ");
        } else if (ctx.lastSubIntent() == AiSubIntent.FIND_CLASS && !alreadyHasClass) {
            expanded.insert(0, "Tìm lớp ");
        }

        if (ctx.lastEntities() != null) {
            if (!currentHasSubject && ctx.lastEntities().containsKey("subject") && !normalized.contains(ctx.lastEntities().get("subject").toLowerCase(Locale.ROOT))) {
                expanded.append(" môn ").append(ctx.lastEntities().get("subject"));
            }
            if (!currentHasGrade && ctx.lastEntities().containsKey("grade") && !normalized.contains(ctx.lastEntities().get("grade").toLowerCase(Locale.ROOT))) {
                expanded.append(" lớp ").append(ctx.lastEntities().get("grade"));
            }
            if (ctx.lastEntities().containsKey("location") && !normalized.contains(ctx.lastEntities().get("location").toLowerCase(Locale.ROOT))) {
                expanded.append(" tại ").append(ctx.lastEntities().get("location"));
            }
        }

        return expanded.toString().trim();
    }

    private boolean hasAnySubject(String normalized) {
        return containsAny(normalized,
                "toan", "ly", "hoa", "anh", "van", "ngu van", "sinh", "su", "dia", "tin", "tin hoc", "gdcd",
                "tieng anh", "tieng nhat", "tieng phap", "tieng trung", "tieng han", "tieng viet",
                "ielts", "toeic", "toefl", "lap trinh", "scratch", "python", "java", "c++");
    }

    private boolean hasAnyGrade(String normalized) {
        return normalized.contains("lop ") || normalized.matches(".*\\blop\\s*\\d+.*") ||
                containsAny(normalized, "cap 1", "cap 2", "cap 3", "dai hoc", "tieu hoc", "thcs", "thpt");
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
                normalized.contains("thay khac") || normalized.contains("nguoi khac") ||
                normalized.contains("gia bao nhieu") || normalized.contains("gia mot buoi") || normalized.contains("gia 1 buoi") ||
                normalized.contains("hoc phi bao nhieu") || normalized.contains("chi phi bao nhieu") || normalized.contains("bao nhieu mot buoi") ||
                normalized.contains("bao nhieu 1 buoi") || normalized.equals("gia bao nhieu") || normalized.equals("hoc phi bao nhieu") ||
                normalized.startsWith("gia ") || normalized.startsWith("hoc phi ") || normalized.startsWith("chi phi ");

        if (!isFollowUpPattern) {
            return new FollowUpResolution(false, null, null, currentEntities);
        }

        Map<String, String> mergedEntities = new HashMap<>(ctx.lastEntities());
        if (currentEntities != null && !currentEntities.isEmpty()) {
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
