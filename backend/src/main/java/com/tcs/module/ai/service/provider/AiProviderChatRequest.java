package com.tcs.module.ai.service.provider;

/**
 * ============================================================================
 * [UC-65] DTO YÊU CẦU SINH PHẢN HỒI AI (AI PROVIDER CHAT REQUEST)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Đóng gói các tham số yêu cầu sinh phản hồi gửi tới các mô hình AI bao gồm prompt, lịch sử và siêu tham số.
 * 
 * Chức năng chính:
 *   1. Chứa nội dung System Prompt và câu hỏi người dùng.
 *   2. Chứa tham số cấu hình nhiệt độ (temperature) và độ dài tối đa (maxTokens).
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Khởi tạo từ tầng điều phối AiProviderRouter.
 *   - Bước 2: Truyền sang các client chuyên biệt để chuyển đổi sang định dạng JSON của từng nhà cung cấp.
 *  * ============================================================================
 */
public record AiProviderChatRequest(
    String systemPrompt,
    String userPrompt,
    int maxOutputTokens,
    double temperature,
    long timeoutMs
) {
    public AiProviderChatRequest {
        if (systemPrompt == null) systemPrompt = "";
        if (userPrompt == null) userPrompt = "";
        if (maxOutputTokens <= 0) {
            maxOutputTokens = 700;
        } else if (maxOutputTokens > 4096) {
            maxOutputTokens = 4096;
        }
        if (Double.isNaN(temperature) || temperature < 0.0) {
            temperature = 0.0;
        } else if (temperature > 2.0) {
            temperature = 2.0;
        }
        if (timeoutMs < 0L) {
            timeoutMs = 0L;
        }
    }

    public AiProviderChatRequest(String systemPrompt, String userPrompt, int maxOutputTokens, double temperature) {
        this(systemPrompt, userPrompt, maxOutputTokens, temperature, 0L);
    }
}
