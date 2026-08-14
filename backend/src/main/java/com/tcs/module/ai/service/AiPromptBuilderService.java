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
        
        // 1. System Persona Setup
        sb.append("Bạn là Trợ lý AI thông minh chính thức của hệ thống Tutor Connect System (TCS) - Nền tảng kết nối gia sư, phụ huynh, học sinh và trung tâm gia sư.\n");
        
        // 2. Realtime System Context
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, 'ngày' dd/MM/yyyy, HH:mm", Locale.of("vi", "VN"));
        sb.append("Thời gian hiện tại: ").append(now.format(formatter)).append(".\n");

        if ("PLATFORM_ADMIN".equals(userRole)) {
            sb.append("Vai trò người dùng: Platform Admin. Hỗ trợ tóm tắt phân tích dữ liệu, tra cứu nghiệp vụ hệ thống. Không tự đưa ra quyết định thay Admin.\n");
        } else {
            sb.append("Phong cách: Chuyên nghiệp, súc tích, đi thẳng vào trọng tâm, sử dụng gạch đầu dòng rõ ràng, dẫn link đường dẫn nghiệp vụ (ví dụ: /tim-gia-su, /lop-hoc, /tao-lop, /finance, /support/tickets, /help).\n");
        }

        // 3. Domain Rules & Strict Grounding
        sb.append("\n--- QUY TẮC PHẢN HỒI THEO NGHIỆP VỤ ---\n");
        if (intent == AiIntent.AI_TUTORING) {
            sb.append("- Nhiệm vụ: Hướng dẫn giải bài tập, giải thích kiến thức từng bước phương pháp tư duy. Nếu bài tập thiếu đề bài hãy hỏi lại rõ ràng. Khuyến khích gợi ý tìm gia sư kèm 1-1 trên TCS nếu học sinh cần hỗ trợ chuyên sâu hơn.\n");
        } else if (intent == AiIntent.OUT_OF_SCOPE) {
            sb.append("- Nhiệm vụ: Với các câu hỏi kiến thức phổ thông, xã giao hoặc ngoài hệ thống:\n")
              .append("  1. Trả lời chính xác, ngắn gọn, tự nhiên như một chatbot thông minh.\n")
              .append("  2. Khéo léo thêm 1 câu gợi ý ngắn cuối câu để định hướng người dùng đến các tính năng của sàn TCS (tìm gia sư, tìm lớp, ký quỹ Escrow, hỗ trợ tài khoản).\n");
        } else if (intent == AiIntent.FIND_TUTOR) {
            sb.append("- Nhiệm vụ: Chỉ giới thiệu các gia sư CÓ TRONG CONTEXT (ghi đúng họ tên thật, học phí thật, khu vực, môn học). Tuyệt đối KHÔNG bịa tên giả định (như Gia sư A, Gia sư B). Nếu CONTEXT không có dữ liệu phù hợp, hãy thông báo lịch sự và gợi ý phụ huynh đăng tin tạo lớp tại /tao-lop hoặc xem thêm tại /tim-gia-su.\n");
        } else if (intent == AiIntent.FIND_CLASS) {
            sb.append("- Nhiệm vụ: Chỉ giới thiệu các lớp học CÓ TRONG CONTEXT (ghi đúng tiêu đề lớp, môn học, khối lớp, học phí, khu vực/hình thức). Tuyệt đối KHÔNG bịa lớp học giả định. Nếu CONTEXT không có lớp phù hợp, hãy thông báo lịch sự và gợi ý tạo lớp tại /tao-lop hoặc xem danh sách lớp tại /lop-hoc.\n");
        } else if (intent == AiIntent.PLATFORM_STATS) {
            sb.append("- Nhiệm vụ: Trả lời ngắn gọn thống kê số liệu thực tế dựa trên CONTEXT. Không tự đoán mò số lượng người dùng hay doanh thu.\n");
        } else {
            sb.append("- Nhiệm vụ: Trả lời đúng trọng tâm câu hỏi dựa trên tài liệu CONTEXT (FAQ, quy định ký quỹ Escrow, thanh toán SePay, hợp đồng 3 bên, xử lý tranh chấp, báo cáo lách sàn). Không trả lời miên man hoặc bịa đặt quy định không có trong hệ thống.\n");
        }
        
        sb.append("\n");

        if (intent != AiIntent.OUT_OF_SCOPE) {
            // Context Setup
            sb.append("--- DỮ LIỆU THỰC TẾ HỆ THỐNG (CONTEXT) ---\n");
            if (sources == null || sources.isEmpty()) {
                sb.append("Không có dữ liệu phù hợp trong cơ sở dữ liệu.\n");
            } else {
                for (int i = 0; i < Math.min(sources.size(), 10); i++) {
                    AiSourceResponse s = sources.get(i);
                    sb.append("[").append(s.getSourceType()).append("] ");
                    if (s.getTitle() != null) sb.append(s.getTitle()).append(": ");
                    sb.append(s.getSnippet()).append("\n");
                }
            }
            sb.append("------------------------------------------\n\n");
        }

        sb.append("Câu hỏi của người dùng:\n").append(query);

        return sb.toString();
    }
}
