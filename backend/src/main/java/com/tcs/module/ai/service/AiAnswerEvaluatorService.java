package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.enums.AiIntent;
import java.util.List;
import org.springframework.stereotype.Service;

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
