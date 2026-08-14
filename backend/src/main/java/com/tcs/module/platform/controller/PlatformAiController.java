package com.tcs.module.platform.controller;

import com.tcs.module.ai.service.KnowledgeIndexerService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/ai")
@RequiredArgsConstructor
public class PlatformAiController {

    private final KnowledgeIndexerService indexerService;

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Integer>> reindexAll() {
        Map<String, Integer> stats = indexerService.reindexAll();
        return ResponseEntity.ok(stats);
    }

    @org.springframework.web.bind.annotation.GetMapping("/knowledge/stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getKnowledgeStats() {
        return ResponseEntity.ok(indexerService.getKnowledgeStats());
    }
}
