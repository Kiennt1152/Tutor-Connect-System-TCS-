package com.tcs.module.platform.controller;

import com.tcs.module.ai.service.AiSemanticCacheService;
import com.tcs.module.ai.service.DynamicFaqGenerationService;
import com.tcs.module.ai.service.KnowledgeIndexerService;
import com.tcs.module.ai.service.RagExperimentService;
import com.tcs.module.ai.service.provider.AiProviderRouter;
import com.tcs.module.catalog.entity.FaqEntry;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/ai")
@RequiredArgsConstructor
public class PlatformAiController {

    private final KnowledgeIndexerService indexerService;
    private final RagExperimentService ragExperimentService;
    private final AiSemanticCacheService semanticCacheService;
    private final DynamicFaqGenerationService dynamicFaqGenerationService;
    private final AiProviderRouter aiProviderRouter;

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Integer>> reindexAll() {
        Map<String, Integer> stats = indexerService.reindexAll();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/knowledge/stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getKnowledgeStats() {
        return ResponseEntity.ok(indexerService.getKnowledgeStats());
    }

    @GetMapping("/experiments/metrics")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getExperimentMetrics() {
        return ResponseEntity.ok(Map.of(
            "strategies", ragExperimentService.getStrategyMetrics(),
            "summary", ragExperimentService.getSummary()
        ));
    }

    @GetMapping("/cache/stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<AiSemanticCacheService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(semanticCacheService.getStats());
    }

    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache() {
        long sizeBefore = semanticCacheService.getStats().totalCaches();
        semanticCacheService.clearExpiredCaches();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Expired cache cleared (total tracked before: " + sizeBefore + ")"
        ));
    }

    @GetMapping("/providers/health")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getProviderHealth() {
        return ResponseEntity.ok(aiProviderRouter.getHealthStatus());
    }

    @PostMapping("/faq/generate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<FaqEntry>> generateFaqsFromTickets(
            @RequestParam(defaultValue = "7") int daysBack,
            @RequestParam(defaultValue = "2") int minOccurrences) {
        List<FaqEntry> drafts = dynamicFaqGenerationService.generateFaqsFromRecentTickets(daysBack, minOccurrences);
        return ResponseEntity.ok(drafts);
    }
}
