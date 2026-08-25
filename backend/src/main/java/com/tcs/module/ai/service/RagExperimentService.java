package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.RagStrategy;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RagExperimentService {

    public record StrategyMetric(
        RagStrategy strategy,
        long totalQueries,
        double avgConfidence,
        double avgLatencyMs,
        long cacheHits
    ) {}

    public record StrategyMetrics(
        long queryCount,
        double avgLatencyMs,
        double avgConfidence,
        double cacheHitRate
    ) {}

    private static class MetricAccumulator {
        final AtomicLong queryCount = new AtomicLong(0);
        final AtomicLong totalLatencyMs = new AtomicLong(0);
        final AtomicLong cacheHitCount = new AtomicLong(0);
        private double totalConfidence = 0.0;

        synchronized void record(double confidence, long latencyMs, boolean cacheHit) {
            queryCount.incrementAndGet();
            totalLatencyMs.addAndGet(latencyMs);
            totalConfidence += confidence;
            if (cacheHit) {
                cacheHitCount.incrementAndGet();
            }
        }

        synchronized StrategyMetric snapshot(RagStrategy strategy) {
            long count = queryCount.get();
            double avgConf = count > 0 ? totalConfidence / count : 0.0;
            double avgLat = count > 0 ? (double) totalLatencyMs.get() / count : 0.0;
            return new StrategyMetric(strategy, count, avgConf, avgLat, cacheHitCount.get());
        }
    }

    private final Map<RagStrategy, MetricAccumulator> metrics = new EnumMap<>(RagStrategy.class);

    public RagExperimentService() {
        for (RagStrategy s : RagStrategy.values()) {
            metrics.put(s, new MetricAccumulator());
        }
    }

    public RagStrategy selectStrategy(Long userId, Long sessionId) {
        long key = (userId != null ? userId : 0) * 31 + (sessionId != null ? sessionId : 0);
        int bucket = (int) (Math.abs(key) % 100);

        if (bucket < 25) {
            return RagStrategy.PURE_VECTOR;
        } else if (bucket < 75) {
            return RagStrategy.HYBRID_VECTOR_BM25;
        } else {
            return RagStrategy.RERANK_COLBERT_HYBRID;
        }
    }

    public void recordExecution(RagStrategy strategy, double confidenceScore, int sourceCount, long latencyMs, boolean cacheHit) {
        if (strategy == null) strategy = RagStrategy.HYBRID_VECTOR_BM25;
        MetricAccumulator acc = metrics.get(strategy);
        if (acc != null) {
            acc.record(confidenceScore, latencyMs, cacheHit);
        }
    }

    public Map<RagStrategy, StrategyMetric> getExperimentSummary() {
        Map<RagStrategy, StrategyMetric> summary = new EnumMap<>(RagStrategy.class);
        for (Map.Entry<RagStrategy, MetricAccumulator> entry : metrics.entrySet()) {
            summary.put(entry.getKey(), entry.getValue().snapshot(entry.getKey()));
        }
        return summary;
    }

    public Map<String, StrategyMetrics> getStrategyMetrics() {
        Map<String, StrategyMetrics> result = new LinkedHashMap<>();
        for (RagStrategy strategy : RagStrategy.values()) {
            MetricAccumulator acc = metrics.get(strategy);
            if (acc != null) {
                StrategyMetric snapshot = acc.snapshot(strategy);
                double hitRate = snapshot.totalQueries() > 0 ? (double) snapshot.cacheHits() / snapshot.totalQueries() : 0.0;
                result.put(strategy.name(), new StrategyMetrics(
                    snapshot.totalQueries(),
                    snapshot.avgLatencyMs(),
                    snapshot.avgConfidence(),
                    hitRate
                ));
            }
        }
        return result;
    }

    public Map<String, Object> getSummary() {
        Map<RagStrategy, StrategyMetric> summary = getExperimentSummary();
        long totalQueries = summary.values().stream().mapToLong(StrategyMetric::totalQueries).sum();
        double avgLatency = summary.values().stream()
            .filter(m -> m.totalQueries() > 0)
            .mapToDouble(StrategyMetric::avgLatencyMs)
            .average().orElse(0.0);

        RagStrategy bestStrategy = summary.entrySet().stream()
            .filter(e -> e.getValue().totalQueries() > 0)
            .max(Comparator.comparingDouble(e -> e.getValue().avgConfidence()))
            .map(Map.Entry::getKey)
            .orElse(RagStrategy.HYBRID_VECTOR_BM25);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalQueries", totalQueries);
        map.put("avgLatencyMs", avgLatency);
        map.put("bestStrategy", bestStrategy.name());
        return map;
    }

    public void reset() {
        metrics.clear();
        for (RagStrategy s : RagStrategy.values()) {
            metrics.put(s, new MetricAccumulator());
        }
    }
}
