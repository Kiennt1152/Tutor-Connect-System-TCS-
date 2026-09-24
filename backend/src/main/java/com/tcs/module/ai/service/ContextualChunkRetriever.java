package com.tcs.module.ai.service;

import com.tcs.module.ai.entity.AiKnowledgeChunk;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * [UC-65] MỞ RỘNG NGỮ CẢNH TRI THỨC LIỀN KỀ (CONTEXTUAL CHUNK RETRIEVER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Mở rộng cửa sổ ngữ cảnh bằng cách tự động ghép nối các đoạn văn bản liền trước và liền sau chunk được tìm thấy.
 * 
 * Chức năng chính:
 *   1. Ghép nối đoạn liền kề: Đọc thêm chunk trước và sau theo chỉ mục tài liệu (chunk_index +/- 1).
 *   2. Đảm bảo tính mạch lạc: Khắc phục nhược điểm câu từ bị cắt ngang giữa các đoạn chunk.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận danh sách chunk tri thức đạt điểm tương đồng cao nhất.
 *   - Bước 2: Truy vấn CSDL để lấy các chunk lân cận cùng thuộc tài liệu nguồn.
 *   - Bước 3: Hợp nhất nội dung theo đúng thứ tự logic cung cấp cho Prompt Builder.
 * ============================================================================
 */
@Component
public class ContextualChunkRetriever {

    public record ContextualChunk(
        AiKnowledgeChunk primaryChunk,
        List<AiKnowledgeChunk> precedingChunks,
        List<AiKnowledgeChunk> succeedingChunks,
        String mergedContext
    ) {}

    public List<ContextualChunk> retrieveWithContext(
        List<AiRetrievalService.RetrievalResult> matches,
        List<AiKnowledgeChunk> allChunks,
        int windowSize
    ) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        if (allChunks == null || allChunks.isEmpty() || windowSize <= 0) {
            return matches.stream()
                .map(m -> new ContextualChunk(m.chunk(), List.of(), List.of(), m.chunk().getContent()))
                .toList();
        }

        // Group chunks by sourceType and sourceId to find siblings within the same document
        Map<String, List<AiKnowledgeChunk>> documentGroups = new HashMap<>();
        for (AiKnowledgeChunk chunk : allChunks) {
            String key = chunk.getSourceType() + ":" + (chunk.getSourceId() != null ? chunk.getSourceId() : "DEFAULT");
            documentGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(chunk);
        }

        // Sort each document group by chunkId to preserve document sequence
        for (List<AiKnowledgeChunk> group : documentGroups.values()) {
            group.sort(Comparator.comparing(c -> c.getChunkId() != null ? c.getChunkId() : 0L));
        }

        List<ContextualChunk> contextualResults = new ArrayList<>();

        for (AiRetrievalService.RetrievalResult match : matches) {
            AiKnowledgeChunk primary = match.chunk();
            String key = primary.getSourceType() + ":" + (primary.getSourceId() != null ? primary.getSourceId() : "DEFAULT");
            List<AiKnowledgeChunk> docChunks = documentGroups.getOrDefault(key, List.of(primary));

            int primaryIndex = docChunks.indexOf(primary);
            if (primaryIndex == -1) {
                // Fallback by chunkId match
                for (int i = 0; i < docChunks.size(); i++) {
                    if (Objects.equals(docChunks.get(i).getChunkId(), primary.getChunkId())) {
                        primaryIndex = i;
                        break;
                    }
                }
            }

            List<AiKnowledgeChunk> preceding = new ArrayList<>();
            List<AiKnowledgeChunk> succeeding = new ArrayList<>();

            if (primaryIndex != -1) {
                // Preceding window
                int start = Math.max(0, primaryIndex - windowSize);
                for (int i = start; i < primaryIndex; i++) {
                    preceding.add(docChunks.get(i));
                }

                // Succeeding window
                int end = Math.min(docChunks.size(), primaryIndex + windowSize + 1);
                for (int i = primaryIndex + 1; i < end; i++) {
                    succeeding.add(docChunks.get(i));
                }
            }

            String merged = buildMergedContext(primary, preceding, succeeding);
            contextualResults.add(new ContextualChunk(primary, preceding, succeeding, merged));
        }

        return contextualResults;
    }

    private String buildMergedContext(
        AiKnowledgeChunk primary,
        List<AiKnowledgeChunk> preceding,
        List<AiKnowledgeChunk> succeeding
    ) {
        StringBuilder sb = new StringBuilder();

        if (!preceding.isEmpty()) {
            sb.append("--- [Ngữ cảnh tài liệu liên quan trước] ---\n");
            for (AiKnowledgeChunk p : preceding) {
                if (p.getContent() != null) sb.append(p.getContent().trim()).append("\n\n");
            }
        }

        sb.append("--- [Nội dung chính khớp] ---\n");
        if (primary.getContent() != null) {
            sb.append(primary.getContent().trim()).append("\n\n");
        }

        if (!succeeding.isEmpty()) {
            sb.append("--- [Ngữ cảnh tài liệu liên quan sau] ---\n");
            for (AiKnowledgeChunk s : succeeding) {
                if (s.getContent() != null) sb.append(s.getContent().trim()).append("\n\n");
            }
        }

        return sb.toString().trim();
    }
}
