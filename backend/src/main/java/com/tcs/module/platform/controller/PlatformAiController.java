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

/**
 * ============================================================================
 * PHÂN HỆ QUẢN TRỊ HẠ TẦNG AI TOÀN DIỆN (PLATFORM AI GOVERNANCE CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Đức)
 * Liên quan Use Case:
 *   - [UC-65] Trợ lý AI & Hạ tầng RAG
 *   - [UC-67] Quản lý cơ sở tri thức FAQ & Tự động tổng hợp FAQ từ Support Tickets
 * 
 * Chức năng quản trị:
 *   1. Quét & Đánh chỉ mục tri thức (Knowledge Reindexing): Đồng bộ FAQ, chính sách, dữ liệu gia sư vào Vector DB.
 *   2. Giám sát & Đo lường RAG (RAG Metrics & Experiments): Theo dõi tỷ lệ hit cache, latency của từng chiến lược.
 *   3. Quản trị bộ nhớ đệm ngữ nghĩa (Semantic Cache Management): Thống kê và dọn dẹp cache hết hạn.
 *   4. Giám sát sức khỏe các nhà cung cấp LLM (Provider Health Checks): Theo dõi Groq, Cerebras, DeepSeek, Gemini.
 *   5. Tự động sinh câu hỏi FAQ từ Ticket hỗ trợ (Dynamic FAQ Mining): Tự động phát hiện các thắc mắc lặp lại.
 */
@RestController
@RequestMapping("/api/platform/ai")
@RequiredArgsConstructor
public class PlatformAiController {

    private final KnowledgeIndexerService indexerService;
    private final RagExperimentService ragExperimentService;
    private final AiSemanticCacheService semanticCacheService;
    private final DynamicFaqGenerationService dynamicFaqGenerationService;
    private final AiProviderRouter aiProviderRouter;

    /**
     * Kích hoạt quét và tái lập chỉ mục toàn bộ cơ sở tri thức RAG (Reindex Knowledge Base).
     * 
     * Nghiệp vụ:
     *   - Thu thập toàn bộ FAQ đang hoạt động, tài liệu chính sách sàn, danh mục môn học,
     *     và hồ sơ gia sư tiêu biểu để cắt chunk (chunking) và lưu vào bảng {@code ai_knowledge_chunks}.
     *   - Bắt buộc quyền Quản trị viên hệ thống (PLATFORM_ADMIN).
     * 
     * @return {@link ResponseEntity} Map chứa số lượng chunk được tạo mới cho từng loại nguồn tri thức
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Integer>> reindexAll() {
        Map<String, Integer> stats = indexerService.reindexAll();
        return ResponseEntity.ok(stats);
    }

    /**
     * Thống kê tổng lượng tri thức RAG đang lưu trữ trong hệ thống.
     * 
     * @return Map chứa tổng số chunks, phân loại theo domain và trạng thái active
     */
    @GetMapping("/knowledge/stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getKnowledgeStats() {
        return ResponseEntity.ok(indexerService.getKnowledgeStats());
    }

    /**
     * Báo cáo hiệu năng và chỉ số đo lường các chiến lược RAG (A/B Testing & Evaluation Metrics).
     * 
     * @return Thông số so sánh giữa các chiến lược RAG (Hit rate, Average Latency, Fallback rate)
     */
    @GetMapping("/experiments/metrics")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getExperimentMetrics() {
        return ResponseEntity.ok(Map.of(
            "strategies", ragExperimentService.getStrategyMetrics(),
            "summary", ragExperimentService.getSummary()
        ));
    }

    /**
     * Tra cứu trạng thái và hiệu suất của Bộ nhớ đệm ngữ nghĩa AI (Semantic Cache Stats).
     * 
     * @return Số lượng cache hit, cache miss, tổng kích thước bộ đệm và tỷ lệ tiết kiệm chi phí gọi LLM
     */
    @GetMapping("/cache/stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<AiSemanticCacheService.CacheStats> getCacheStats() {
        return ResponseEntity.ok(semanticCacheService.getStats());
    }

    /**
     * Dọn dẹp các mục bộ nhớ đệm AI đã hết hạn (TTL Expired) để tối ưu dung lượng DB.
     * 
     * @return Thông báo kết quả và số lượng mục đã dọn dẹp
     */
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

    /**
     * Kiểm tra trạng thái hoạt động (Liveness / Readiness) của toàn bộ các Provider LLM.
     * 
     * @return Trạng thái kết nối hiện tại của Groq, Cerebras, DeepSeek, Gemini kèm độ trễ phản hồi
     */
    @GetMapping("/providers/health")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getProviderHealth() {
        return ResponseEntity.ok(aiProviderRouter.getHealthStatus());
    }

    /**
     * [UC-67 - Tự động tổng hợp FAQ]: Khai phá và tự động đề xuất câu hỏi FAQ từ Support Tickets gần đây.
     * 
     * Nghiệp vụ:
     *   - Quét qua các Support Ticket đã giải quyết trong {@code daysBack} ngày qua.
     *   - Dùng AI gom nhóm các câu hỏi cùng chủ đề xuất hiện tối thiểu {@code minOccurrences} lần.
     *   - Tạo bản thảo FAQ Entry mới ở trạng thái chờ duyệt, giúp tiết kiệm thời gian biên soạn nội dung.
     * 
     * @param daysBack Khoảng thời gian quét (mặc định: 7 ngày gần nhất)
     * @param minOccurrences Ngưỡng lặp lại tối thiểu để xem là thắc mắc phổ biến (mặc định: 2 lần)
     * @return Danh sách các bản thảo FAQ {@link FaqEntry} do AI đề xuất
     */
    @PostMapping("/faq/generate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<FaqEntry>> generateFaqsFromTickets(
            @RequestParam(defaultValue = "7") int daysBack,
            @RequestParam(defaultValue = "2") int minOccurrences) {
        List<FaqEntry> drafts = dynamicFaqGenerationService.generateFaqsFromRecentTickets(daysBack, minOccurrences);
        return ResponseEntity.ok(drafts);
    }
}
