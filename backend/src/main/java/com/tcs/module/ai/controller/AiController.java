package com.tcs.module.ai.controller;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSessionResponse;
import com.tcs.module.ai.service.AiService;
import com.tcs.security.AuthHelper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * PHÂN HỆ AI & TRỢ LÝ THÔNG MINH RAG CHATBOT (AI MODULE & RAG CONTROLLER)
 * ============================================================================
 * 
 * Mã Use Case: [UC-65] Trợ lý ảo AI & Hỏi đáp thông minh toàn sàn
 * Tác giả: mduc1011-swp (Đức)
 * 
 * Chức năng tổng quan:
 *   - Tiếp nhận câu hỏi trò chuyện từ Widget AI nổi (Floating Chat Widget) trên toàn bộ hệ thống.
 *   - Hỗ trợ cả 2 chế độ:
 *       1. Người dùng vãng lai / Chưa đăng nhập (Guest): Trả lời chính sách, bảng giá, hướng dẫn sử dụng.
 *       2. Người dùng đã đăng nhập (Client, Tutor, Center, Admin): Cá nhân hóa ngữ cảnh theo vai trò,
 *          truy vấn lịch học, hợp đồng, trạng thái thanh toán hoặc thống kê KPI tương ứng.
 *   - Quản lý phiên hội thoại (Chat Sessions) và lịch sử trao đổi tin nhắn (Session Messages).
 *   - Tích hợp kiến trúc RAG nâng cao (Retrieval-Augmented Generation):
 *       Semantic Cache -> Rule Intent -> Hybrid Search (Vector/BM25) -> Dynamic Context -> Anti-Hallucination.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AuthHelper authHelper;

    /**
     * [UC-65 - Bước 1]: Tiếp nhận câu hỏi trò chuyện và sinh câu trả lời thông minh từ AI RAG Pipeline.
     * 
     * Luồng thực thi (RAG Pipeline Execution Flow):
     *   1. Nhận diện danh tính người dùng (authHelper.currentUserIdOrNull): Xác định Guest hay User.
     *   2. Semantic Cache: Kiểm tra xem câu hỏi tương tự đã có câu trả lời lưu trong Cache chưa.
     *   3. Phân loại Ý định (Intent Classification): Phân tích ngữ nghĩa để biết người dùng muốn:
     *      - Tìm gia sư (TUTOR_SEARCH), tra cứu lớp (CLASS_SEARCH), chính sách hoàn tiền (REFUND_POLICY),
     *      - Hỗ trợ sự cố (DISPUTE_SUPPORT), hay hỏi đáp chung (FAQ_GENERAL).
     *   4. Truy xuất tri thức (Retrieval): Lấy top chunk tài liệu, câu hỏi FAQ và dữ liệu thực tế từ DB.
     *   5. Xây dựng Prompt (Prompt Building): Bổ sung Persona, System Instructions và Context động.
     *   6. Gọi Multi-LLM (Provider Routing): Điều phối qua Groq, Cerebras, DeepSeek hoặc Gemini.
     *   7. Kiểm duyệt ảo giác (Hallucination Guard): Xác minh số liệu, tiền tệ, cam kết không bịa đặt.
     *   8. Gắn Reference Cards: Tự động đính kèm thẻ Gia sư/Lớp học/FAQ gợi ý trực quan cho UI.
     * 
     * @param request Chứa sessionId (nếu tiếp tục phiên cũ), message (câu hỏi) và domain ngữ cảnh
     * @return {@link AiMessageResponse} Câu trả lời đã kiểm duyệt, gợi ý tiếp theo và danh sách thẻ tham chiếu
     */
    @PostMapping("/chat")
    public AiMessageResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiService.chat(request, getOptionalUserId());
    }

    /**
     * [UC-65 - Quản lý phiên]: Lấy danh sách các phiên hội thoại của người dùng hiện tại.
     * 
     * Nghiệp vụ:
     *   - Cho phép người dùng xem lại danh sách các chủ đề đã từng hỏi AI trước đây.
     *   - Nếu là Guest (chưa đăng nhập), chỉ trả về các phiên tạm của session hiện tại.
     * 
     * @return Danh sách các phiên {@link AiSessionResponse} kèm tiêu đề tự động tóm tắt và thời gian tạo
     */
    @GetMapping("/sessions")
    public List<AiSessionResponse> getUserSessions() {
        return aiService.getUserSessions(getOptionalUserId());
    }

    /**
     * [UC-65 - Lịch sử hội thoại]: Xem chi tiết toàn bộ tin nhắn trong một phiên hội thoại cụ thể.
     * 
     * Kiểm tra bảo mật:
     *   - Chỉ chủ sở hữu của phiên hội thoại (hoặc Admin) mới có quyền đọc lịch sử tin nhắn của phiên đó.
     * 
     * @param sessionId ID của phiên hội thoại cần tra cứu
     * @return Danh sách các tin nhắn {@link AiMessageResponse} theo thứ tự thời gian tăng dần
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public List<AiMessageResponse> getSessionMessages(@PathVariable Long sessionId) {
        return aiService.getSessionMessages(sessionId, getOptionalUserId());
    }

    /**
     * [UC-65 - Xóa dữ liệu]: Xóa vĩnh viễn một phiên hội thoại và toàn bộ tin nhắn liên quan.
     * 
     * Nghiệp vụ:
     *   - Hỗ trợ người dùng dọn dẹp lịch sử trò chuyện để bảo đảm quyền riêng tư cá nhân.
     * 
     * @param sessionId ID phiên hội thoại cần xóa
     */
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable Long sessionId) {
        aiService.deleteSession(sessionId, getOptionalUserId());
    }

    /**
     * Trích xuất User ID từ JWT Token nếu người dùng đã đăng nhập, trả về null nếu là khách vãng lai.
     */
    private Long getOptionalUserId() {
        return authHelper.currentUserIdOrNull();
    }
}
