package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.enums.AiIntent;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiPromptBuilderService {

    public String buildPrompt(String query, AiIntent intent, String userRole, List<AiSourceResponse> sources) {
        StringBuilder sb = new StringBuilder();
        
        // System Prompt Setup
        sb.append("Bạn là Trợ lý AI của hệ thống kết nối gia sư Tutor Connect System (TCS). ");
        if ("PLATFORM_ADMIN".equals(userRole)) {
            sb.append("Bạn là trợ lý vận hành cho Platform Admin. Chỉ tóm tắt và gợi ý, không tự đưa ra quyết định thay admin. ");
        } else {
            sb.append("Bạn hỗ trợ người dùng nhiệt tình, chính xác và chuyên nghiệp. ");
        }

        // Rules based on intent
        if (intent == AiIntent.AI_TUTORING) {
            sb.append("Luật: Hướng dẫn từng bước, hỏi lại nếu thiếu đề bài, ưu tiên giải thích phương pháp tư duy. Gợi ý tìm gia sư trên TCS nếu học sinh cần kèm cặp sâu hơn. ");
        } else if (intent == AiIntent.OUT_OF_SCOPE) {
            sb.append("Luật: Trả lời tự nhiên, thân thiện, lịch sự các câu hỏi thông thường. ");
        } else if (intent == AiIntent.FIND_TUTOR) {
            sb.append("Luật: Hãy giới thiệu các gia sư CÓ TRONG CONTEXT (nêu đúng họ tên thật, học phí thật, khu vực và đánh giá). Tuyệt đối KHÔNG được tự bịa ra bất kỳ tên gia sư giả định nào như 'Gia sư A', 'Gia sư B', 'Gia sư C'. Nếu trong CONTEXT không có gia sư nào hoặc ghi chưa có dữ liệu phù hợp, hãy thông báo lịch sự rằng hiện chưa tìm thấy gia sư phù hợp trong hệ thống và gợi ý người dùng nới lỏng mức giá/khu vực hoặc đăng bài tìm gia sư tại /tao-lop. ");
        } else if (intent == AiIntent.PLATFORM_STATS) {
            sb.append("Luật: Trả lời ngắn gọn thống kê số liệu thực tế của nền tảng dựa trên CONTEXT. ");
        } else {
            sb.append("Luật: Bạn chỉ được dùng dữ liệu trong CONTEXT để trả lời. Nếu CONTEXT không đủ, hãy nói rõ chưa đủ dữ liệu và gợi ý thao tác tiếp theo. Không tự bịa ID, trạng thái thanh toán, trạng thái ticket, học phí, rating, tên người dùng. ");
        }
        
        sb.append("\n\n");

        if (intent != AiIntent.OUT_OF_SCOPE) {
            // Context Setup
            sb.append("--- CONTEXT ---\n");
            if (sources == null || sources.isEmpty()) {
                sb.append("Không có dữ liệu.\n");
            } else {
                for (int i = 0; i < Math.min(sources.size(), 10); i++) {
                    AiSourceResponse s = sources.get(i);
                    sb.append("[").append(s.getSourceType()).append("] ");
                    if (s.getTitle() != null) sb.append(s.getTitle()).append(": ");
                    sb.append(s.getSnippet()).append("\n");
                }
            }
            sb.append("---------------\n\n");
        }

        sb.append("Câu hỏi của người dùng:\n").append(query);

        return sb.toString();
    }
}
