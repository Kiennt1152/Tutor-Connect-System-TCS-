package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.FaqReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] CHUẨN HÓA & ĐÓNG GÓI PHẢN HỒI AI (AI RESPONSE BUILDER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Xử lý hậu kỳ văn bản từ LLM, loại bỏ định dạng thừa, làm sạch Markdown và gắn siêu dữ liệu nguồn tin.
 * 
 * Chức năng chính:
 *   1. Làm sạch văn bản: Loại bỏ khoảng trắng thừa, thẻ HTML lỗi hoặc cú pháp Markdown bị hỏng.
 *   2. Gắn metadata nguồn: Ghi nhận nhà cung cấp LLM, thời gian phản hồi và danh sách nguồn trích dẫn.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận phản hồi thô từ nhà cung cấp mô hình ngôn ngữ.
 *   - Bước 2: Chuẩn hóa định dạng Markdown và kiểm tra độ hoàn chỉnh câu từ.
 *   - Bước 3: Đóng gói thành đối tượng phản hồi hoàn chỉnh gửi về client.
 * ============================================================================
 */
@Service
public class AiResponseBuilderService {

    public AiMessageResponse build(
        Long messageId,
        Long sessionId,
        String content,
        String intent,
        String domain,
        String subIntent,
        String suggestedRoute,
        List<String> options,
        String answerMode,
        double score,
        String level,
        int sourceCount,
        String groundingStatus,
        List<AiSourceResponse> sources,
        List<TutorReferenceDto> tutors,
        List<ClassReferenceDto> classes,
        List<FaqReferenceDto> faqs,
        String rewrittenQuery,
        boolean isFollowUp,
        String evalNotes,
        String warningCode
    ) {
        return AiMessageResponse.builder()
            .messageId(messageId)
            .sessionId(sessionId)
            .role("assistant")
            .content(content)
            .intent(intent)
            .domain(domain)
            .subIntent(subIntent)
            .suggestedRoute(suggestedRoute)
            .clarificationOptions(options != null ? options : List.of())
            .answerMode(answerMode)
            .confidenceScore(score)
            .confidenceLevel(level)
            .sourceCount(sourceCount)
            .groundingStatus(groundingStatus)
            .sources(sources != null ? sources : List.of())
            .referencedTutors(tutors != null ? tutors : List.of())
            .referencedClasses(classes != null ? classes : List.of())
            .referencedFaqs(faqs != null ? faqs : List.of())
            .rewrittenQuery(rewrittenQuery)
            .followUp(isFollowUp)
            .evaluationNotes(evalNotes)
            .warningCode(warningCode)
            .build();
    }
}
