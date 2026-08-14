package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.enums.AiIntent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AiPromptBuilderService {

    public String buildPrompt(String query, AiIntent intent, String userRole, List<AiSourceResponse> sources) {
        StringBuilder sb = new StringBuilder();
        
        // System Prompt Setup
        sb.append("Bạn là Trợ lý AI của hệ thống kết nối gia sư Tutor Connect System (TCS). ");
        
        // Inject current system time and date
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, 'ngày' dd/MM/yyyy, HH:mm", Locale.of("vi", "VN"));
        sb.append("Thời gian hiện tại của hệ thống: ").append(now.format(formatter)).append(". ");

        if ("PLATFORM_ADMIN".equals(userRole)) {
            sb.append("Bạn là trợ lý vận hành cho Platform Admin. Chỉ tóm tắt và gợi ý, không tự đưa ra quyết định thay admin. ");
        } else {
            sb.append("Bạn hỗ trợ người dùng nhiệt tình, chính xác và chuyên nghiệp. ");
        }

        // Rules based on intent
        if (intent == AiIntent.AI_TUTORING) {
            sb.append("Luật: Hướng dẫn từng bước, hỏi lại nếu thiếu đề bài, ưu tiên giải thích phương pháp tư duy. Gợi ý tìm gia sư trên TCS nếu học sinh cần kèm cặp sâu hơn. ");
        } else if (intent == AiIntent.OUT_OF_SCOPE) {
            sb.append("Luật: Đối với các câu hỏi ngoài phạm vi hệ thống (như hỏi ngày giờ, thời tiết, toán học cơ bản, kiến thức chung, chào hỏi xã giao): "
                    + "1. Trả lời một cách ngắn gọn, chính xác, tự nhiên và thân thiện. "
                    + "2. Sau câu trả lời, hãy khéo léo và nhẹ nhàng thêm 1 câu gợi ý ngắn định hướng người dùng quay lại các tính năng chính của Tutor Connect System (TCS) như: tìm gia sư phù hợp, đăng tin tìm lớp, hướng dẫn thanh toán ký quỹ Escrow, hoặc giải đáp các thắc mắc về quy trình dạy & học. ");
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
