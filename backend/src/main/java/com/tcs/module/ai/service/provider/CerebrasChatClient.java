package com.tcs.module.ai.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ============================================================================
 * [UC-65] KẾT NỐI MÔ HÌNH CEREBRAS (CEREBRAS CHAT CLIENT)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Hiện thực kết nối tới cụm siêu máy tính Cerebras Wafer-Scale Engine chạy mô hình Llama 3.
 * 
 * Chức năng chính:
 *   1. Xử lý tốc độ cao: Đóng vai trò là phương án dự phòng số 1 sau Groq.
 *   2. Kiểm soát thời gian chờ (Timeout): Thiết lập ngưỡng ngắt kết nối an toàn.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Gửi yêu cầu HTTP POST có kèm bearer token tới Cerebras inference API.
 *   - Bước 2: Xử lý phản hồi và trả về cấu trúc kết quả chuẩn hóa.
 *  * ============================================================================
 */
public class CerebrasChatClient extends OpenAiCompatibleChatClient {

    public CerebrasChatClient(String apiKey, String baseUrl, String model, ObjectMapper objectMapper, long timeoutMs) {
        super(apiKey, baseUrl, model, objectMapper, timeoutMs);
    }

    @Override
    public String providerName() {
        return "Cerebras";
    }
}
