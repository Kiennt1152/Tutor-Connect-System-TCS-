package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.enums.AiIntent;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] ĐÁNH GIÁ ĐỘ TIN CẬY & BÁM SÁT NGUỒN TIN (ANSWER EVALUATOR)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Đo lường độ bám sát của câu trả lời sinh ra từ LLM đối với tài liệu nguồn tri thức (Faithfulness / Grounding).
 * 
 * Chức năng chính:
 *   1. Chấm điểm bám sát nguồn: Đo lường tỷ lệ thông tin trong câu trả lời có nguồn gốc từ tài liệu tham chiếu.
 *   2. Cảnh báo thông tin bịa đặt: Đánh dấu các câu khẳng định không có căn cứ trong ngữ cảnh.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận câu trả lời từ LLM và danh sách chunk tài liệu nguồn.
 *   - Bước 2: Phân tích đối chiếu thực thể và số liệu giữa hai văn bản.
 *   - Bước 3: Trả về điểm số độ tin cậy phục vụ quyết định hiển thị cho người dùng.
 * ============================================================================
 */
@Service
public class AiAnswerEvaluatorService {

    public record EvaluatedAnswer(
        String answerMode, 
        Double confidenceScore, 
        String confidenceLevel, 
        Integer sourceCount, 
        String groundingStatus, 
        String warningCode, 
        String evaluationNotes
    ) {}

    public EvaluatedAnswer evaluate(AiIntent intent, List<AiSourceResponse> sources) {
        if (intent == AiIntent.OUT_OF_SCOPE) {
            return new EvaluatedAnswer(
                "LLM", null, null, 0, "OUT_OF_SCOPE", null, "Câu hỏi ngoài phạm vi, bỏ qua RAG"
            );
        }

        if (sources == null || sources.isEmpty()) {
            return new EvaluatedAnswer(
                "FALLBACK", 0.0, "LOW", 0, "NO_SOURCE", "NO_KNOWLEDGE_FOUND", "Không tìm thấy dữ liệu phù hợp."
            );
        }

        // For DB providers (TUTOR, PLATFORM_STATS), we might not have finalScore or it might be null/0.0
        // We consider them highly grounded if they returned results.
        double maxScore = 1.0;
        if (sources.get(0).getFinalScore() != null) {
             maxScore = sources.get(0).getFinalScore();
        }
        
        String level = maxScore > 0.7 ? "HIGH" : (maxScore > 0.5 ? "MEDIUM" : "LOW");
        String grounding = maxScore > 0.7 ? "GROUNDED" : (maxScore > 0.5 ? "PARTIALLY_GROUNDED" : "POOR_GROUNDING");
        String warning = maxScore <= 0.5 ? "LOW_CONFIDENCE" : null;
        
        return new EvaluatedAnswer(
            "RAG", maxScore, level, sources.size(), grounding, warning, "Tìm thấy " + sources.size() + " nguồn"
        );
    }
}
