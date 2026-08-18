package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiSubIntent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenDomainRateLimiter {

    private static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MILLIS = 60_000L; // 1 minute

    private final int maxRequestsPerMinute;
    private final Map<String, Deque<Long>> requestHistory = new ConcurrentHashMap<>();

    public OpenDomainRateLimiter() {
        this(DEFAULT_MAX_REQUESTS_PER_MINUTE);
    }

    public OpenDomainRateLimiter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public boolean allowRequest(Long userId, Long sessionId, AiSubIntent subIntent) {
        if (subIntent == null) {
            return true;
        }

        String userIdentifier = userId != null ? "U_" + userId : "S_" + (sessionId != null ? sessionId : "ANON");
        String key = userIdentifier + ":" + subIntent.name();

        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MILLIS;

        Deque<Long> timestamps = requestHistory.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            // Evict timestamps older than 1 minute
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequestsPerMinute) {
                log.warn("Rate limit exceeded for key {} on subIntent {}. Count: {}", key, subIntent, timestamps.size());
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    public void reset() {
        requestHistory.clear();
    }
}
