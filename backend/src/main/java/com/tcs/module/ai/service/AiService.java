package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSessionResponse;
import java.util.List;

/**
 * ============================================================================
 * [UC-65] DỊCH VỤ TRỢ LÝ ẢO AI & HẠ TẦNG RAG (AI SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Dịch vụ hội thoại thông minh hỗ trợ giải đáp chính sách sàn và tìm kiếm gia sư/lớp học.
 *   - Tích hợp công nghệ RAG trích xuất tri thức và các bộ đệm ngữ nghĩa phản hồi siêu tốc.
 * 
 * Chức năng chính:
 *   1. Xử lý hội thoại: Tiếp nhận câu hỏi, phân loại ý định và sinh câu trả lời tự nhiên.
 *   2. Quản lý phiên hội thoại: Tạo mới, lưu vết lịch sử trò chuyện và quản lý các phiên chat của người dùng.
 *   3. Gợi ý thực thể: Trả về danh sách gia sư, lớp học phù hợp với nhu cầu của phụ huynh.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Nhận yêu cầu trò chuyện từ người dùng (`chat`).
 *   - Bước 2: Nhận diện ý định và truy xuất tri thức phù hợp từ bộ nhớ đệm hoặc cơ sở tri thức.
 *   - Bước 3: Tổng hợp phản hồi qua LLM và hậu kiểm chống ảo giác (Hallucination Guard).
 *   - Bước 4: Lưu lịch sử hội thoại và trả kết quả về cho giao diện client.
 * ============================================================================
 */
public interface AiService {

    /**
     * [UC-65] Xử lý hội thoại thông minh RAG đa bước với người dùng.
     * 
     * @param request Dữ liệu câu hỏi và ngữ cảnh phiên hội thoại {@link ChatRequest}
     * @param userId ID người dùng đang chat (null nếu là khách vãng lai)
     * @return Câu trả lời từ AI kèm thẻ gợi ý thực thể {@link AiMessageResponse}
     */
    AiMessageResponse chat(ChatRequest request, Long userId);

    /**
     * [UC-65] Lấy danh sách các phiên hội thoại AI của người dùng hiện tại.
     * 
     * @param userId ID người dùng đã xác thực
     * @return Danh sách các phiên trò chuyện {@link AiSessionResponse}
     */
    List<AiSessionResponse> getUserSessions(Long userId);

    /**
     * [UC-65] Tải toàn bộ tin nhắn trong một phiên hội thoại kèm các thẻ tham chiếu.
     * 
     * @param sessionId ID phiên trò chuyện
     * @param userId ID người dùng yêu cầu truy cập
     * @return Danh sách tin nhắn trao đổi {@link AiMessageResponse}
     */
    List<AiMessageResponse> getSessionMessages(Long sessionId, Long userId);

    /**
     * [UC-65] Xóa bỏ phiên trò chuyện và toàn bộ lịch sử tin nhắn liên quan.
     * 
     * @param sessionId ID phiên trò chuyện cần xóa
     * @param userId ID người dùng sở hữu phiên chat
     */
    void deleteSession(Long sessionId, Long userId);
}
