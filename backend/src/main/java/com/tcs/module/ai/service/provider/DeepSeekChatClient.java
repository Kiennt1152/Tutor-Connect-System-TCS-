package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ============================================================================
 * [UC-65] KẾT NỐI MÔ HÌNH DEEPSEEK (DEEPSEEK CHAT CLIENT)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Tích hợp mô hình ngôn ngữ lớn DeepSeek V3 với khả năng lập luận và hiểu tiếng Việt xuất sắc.
 * 
 * Chức năng chính:
 *   1. Suy luận nghiệp vụ phức tạp: Đưa ra lời giải và tư vấn học tập chi tiết.
 *   2. Xử lý chuyển đổi dự phòng: Tự động ghi nhận lỗi kết nối khi máy chủ phản hồi chậm.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu trò chuyện từ Router.
 *   - Bước 2: Gửi request tới API DeepSeek và trích xuất nội dung văn bản phản hồi.
 *  * ============================================================================
 */
public class DeepSeekChatClient extends OpenAiCompatibleChatClient {

    public DeepSeekChatClient(String apiKey, String baseUrl, String model, ObjectMapper objectMapper, long timeoutMs) {
        super(apiKey, baseUrl, model, objectMapper, timeoutMs);
    }

    @Override
    public String providerName() {
        return "DeepSeek";
    }
}
