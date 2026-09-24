package com.tcs.module.ai.service;

import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.enums.AiIntent;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] TÁI CẤU TRÚC & GIẢI MÃ ĐẠI TỪ CÂU HỎI (QUERY REWRITE SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Viết lại câu hỏi người dùng dựa trên ngữ cảnh lịch sử chat, thay thế các đại từ chỉ định ("ông ấy", "lớp này").
 * 
 * Chức năng chính:
 *   1. Giải mã đại từ chỉ định: Thay thế "thầy ấy", "môn này" bằng tên gia sư và môn học cụ thể từ lượt chat trước.
 *   2. Mở rộng câu hỏi ngắn: Biến câu hỏi cộc lốc thành câu truy vấn đầy đủ ngữ nghĩa phục vụ tìm kiếm.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận câu hỏi hiện tại và 2 lượt trao đổi gần nhất.
 *   - Bước 2: Phân tích đại từ liên kết và tạo câu hỏi độc lập (Standalone Query).
 *   - Bước 3: Chuyển câu hỏi đã được làm giàu cho bộ truy xuất tri thức Vector và BM25.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class AiQueryRewriteService {

    private final IntentClassifier intentClassifier;

    public record RewriteResult(String rewrittenQuery, boolean isFollowUp, AiIntent inferredIntent) {}

    public RewriteResult rewriteQuery(List<AiChatMessage> history, String currentMessage, AiIntent currentIntent) {
        if (history == null || history.isEmpty()) {
            return new RewriteResult(currentMessage, false, currentIntent);
        }

        String lower = currentMessage.toLowerCase(Locale.ROOT);
        boolean isFollowUp = lower.contains("rẻ hơn") || lower.contains("gần hơn") || 
                             lower.contains("online") || lower.contains("nữ") || 
                             lower.contains("nam") || lower.contains("khác") || 
                             lower.contains("thêm") || lower.contains("cao hơn") || 
                             lower.contains("thấp hơn") ||
                             lower.contains("học thử") || lower.contains("buổi đầu") ||
                             lower.contains("vậy có") || lower.contains("thế có") ||
                             lower.contains("được không") || lower.contains("đổi gia sư");
        
        if (isFollowUp) {
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 6); i--) {
                AiChatMessage msg = history.get(i);
                if ("user".equals(msg.getRole())) {
                    IntentClassifier.IntentResult prevIntent = intentClassifier.classify(msg.getContent());
                    return new RewriteResult(msg.getContent() + " và câu hỏi tiếp nối: " + currentMessage, true, currentIntent);
                }
            }
        }
        
        return new RewriteResult(currentMessage, false, currentIntent);
    }
}
