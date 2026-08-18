package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiSubIntent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenDomainAnalytics {

    public record OpenDomainEvent(
        String userId,
        Long sessionId,
        AiSubIntent subIntent,
        String query,
        String suggestedRoute,
        List<String> ctaButtons,
        Instant timestamp
    ) {}

    public record OpenDomainStats(
        long totalQueries,
        Map<String, Long> subIntentBreakdown,
        double ctaSteeringRate,
        List<String> recentQueries
    ) {}

    private static final int MAX_EVENTS_IN_MEMORY = 1000;
    private final ConcurrentLinkedDeque<OpenDomainEvent> eventQueue = new ConcurrentLinkedDeque<>();

    public void track(Long userId, Long sessionId, AiSubIntent subIntent, String query, String suggestedRoute, List<String> ctaButtons) {
        String uIdStr = userId != null ? String.valueOf(userId) : "GUEST";
        OpenDomainEvent event = new OpenDomainEvent(
            uIdStr, sessionId, subIntent, query, suggestedRoute, ctaButtons, Instant.now()
        );

        eventQueue.addFirst(event);
        while (eventQueue.size() > MAX_EVENTS_IN_MEMORY) {
            eventQueue.pollLast();
        }

        log.debug("Tracked OpenDomainEvent: subIntent={}, user={}, query={}", subIntent, uIdStr, query);
    }

    public OpenDomainStats getStats() {
        long total = eventQueue.size();
        if (total == 0) {
            return new OpenDomainStats(0, Map.of(), 0.0, List.of());
        }

        Map<String, Long> breakdown = eventQueue.stream()
                .filter(e -> e.subIntent() != null)
                .collect(Collectors.groupingBy(e -> e.subIntent().name(), Collectors.counting()));

        long withCta = eventQueue.stream()
                .filter(e -> e.ctaButtons() != null && !e.ctaButtons().isEmpty())
                .count();

        double steeringRate = Math.round(((double) withCta / total) * 1000.0) / 10.0;

        List<String> recent = eventQueue.stream()
                .limit(10)
                .map(OpenDomainEvent::query)
                .toList();

        return new OpenDomainStats(total, breakdown, steeringRate, recent);
    }

    public void clear() {
        eventQueue.clear();
    }
}
