package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ============================================================================
 * [UC-65] KẾT NỐI MÔ HÌNH GROQ LLAMA (GROQ CHAT CLIENT)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Hiện thực kết nối tốc độ siêu cao tới hạ tầng phần cứng LPU của Groq chạy mô hình Llama-3.3-70b.
 * 
 * Chức năng chính:
 *   1. Tốc độ sinh văn bản cực nhanh: Đạt trên 200 token/giây phục vụ trải nghiệm người dùng tức thì.
 *   2. Tự động thử lại và bắt lỗi 429: Báo cáo quá tải cho Router kích hoạt Failover.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Đóng gói tin nhắn theo chuẩn OpenAI REST API gửi tới endpoint Groq.
 *   - Bước 2: Tiếp nhận và bóc tách câu trả lời từ choices[0].message.content.
 *  * ============================================================================
 */
public class GroqChatClient extends OpenAiCompatibleChatClient {

    public GroqChatClient(String apiKey, String baseUrl, String model, ObjectMapper objectMapper, long timeoutMs) {
        super(apiKey, baseUrl, model, objectMapper, timeoutMs);
    }

    @Override
    public String providerName() {
        return "Groq";
    }
}
