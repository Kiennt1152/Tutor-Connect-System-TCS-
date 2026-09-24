package com.tcs.module.ai.service.provider;

/**
 * ============================================================================
 * [UC-65] GIAO DIỆN KHÁCH HÀNG MÔ HÌNH AI (AI CHAT PROVIDER CLIENT)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Giao diện chuẩn hóa tích hợp các nhà cung cấp mô hình ngôn ngữ lớn (Groq, Cerebras, DeepSeek, Gemini).
 * 
 * Chức năng chính:
 *   1. Xác thực cấu hình: Kiểm tra tính sẵn sàng của API Key và Endpoint của provider.
 *   2. Sinh phản hồi chat: Đóng gói yêu cầu và gọi sinh văn bản từ LLM.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Nhận yêu cầu chat chuẩn hóa AiProviderChatRequest.
 *   - Bước 2: Gửi truy vấn tới dịch vụ LLM tương ứng và trả về AiProviderChatResponse.
 *  * ============================================================================
 */
public interface AiChatProviderClient {
    String providerName();
    boolean isConfigured();
    AiProviderChatResponse chat(AiProviderChatRequest request);
}
