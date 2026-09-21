package com.tcs.module.catalog.service;

import java.util.Optional;

/**
 * ============================================================================
 * [UC-65] DỊCH VỤ KẾT NỐI MÔ HÌNH GOOGLE GEMINI (GEMINI SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-13
 * 
 * Mô tả Use Case:
 *   - Định nghĩa giao diện kết nối API với mô hình ngôn ngữ lớn Google Gemini.
 *   - Đóng vai trò lớp xử lý dự phòng (Fallback Layer) giải đáp thắc mắc người dùng khi hệ thống chưa có dữ liệu FAQ.
 * 
 * Chức năng chính:
 *   1. Truy vấn mô hình: Gửi câu hỏi và tiếp nhận câu trả lời được sinh từ Gemini AI.
 *   2. Quản lý dự phòng: Trả về Optional rỗng khi chưa cấu hình API key hoặc gặp lỗi kết nối mạng.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận chuỗi câu hỏi từ người dùng (askQuestion).
 *   - Bước 2: Kiểm tra tính sẵn sàng của cấu hình Gemini API.
 *   - Bước 3: Gọi API và trả về kết quả định dạng văn bản cho người dùng.
 * ============================================================================
 */
public interface GeminiService {

    /** Tra ve Optional.empty() neu chua cau hinh API key hoac goi API loi (rate limit, timeout...). */
    Optional<String> askQuestion(String question);
}
