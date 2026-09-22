package com.tcs.module.ai.service.provider;

/**
 * ============================================================================
 * [UC-65] DTO PHẢN HỒI TỪ MÔ HÌNH AI (AI PROVIDER CHAT RESPONSE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Đóng gói kết quả sinh văn bản từ các mô hình AI kèm thông tin nhà cung cấp và số lượng token tiêu thụ.
 * 
 * Chức năng chính:
 *   1. Chứa văn bản sinh ra từ LLM và tên nhà cung cấp thực thi.
 *   2. Chứa thời gian xử lý và trạng thái thành công/thất bại.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận phản hồi thô từ REST API của nhà cung cấp.
 *   - Bước 2: Chuẩn hóa thành đối tượng AiProviderChatResponse trả về cho tầng RAG.
 *  * ============================================================================
 */
public record AiProviderChatResponse(
    String provider,
    String model,
    String content,
    int statusCode
) {}
