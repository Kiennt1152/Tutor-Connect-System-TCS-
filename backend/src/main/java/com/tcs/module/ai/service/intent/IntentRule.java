package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;

/**
 * ============================================================================
 * [UC-65] CHIẾN LƯỢC PHÂN LOẠI Ý ĐỊNH AI (INTENT RULE INTERFACE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Định nghĩa giao diện chiến lược (Strategy Interface) cho các quy tắc phân loại ý định người dùng theo phân hệ nghiệp vụ.
 * 
 * Chức năng chính:
 *   1. Định nghĩa phương thức phân loại: Nhận diện ý định từ câu truy vấn đã chuẩn hóa không dấu và câu gốc.
 *   2. Thiết lập độ ưu tiên: Xác định thứ tự chạy ưu tiên giữa các quy tắc trong Registry.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận câu truy vấn đã chuẩn hóa từ IntentClassifier.
 *   - Bước 2: So khớp mẫu câu hỏi và trả về ClassificationDetail nếu phù hợp.
 *  * ============================================================================
 */
public interface IntentRule {

    /**
     * Try to classify the normalized query.
     * @param normalized query after accented diacritic removal and teencode expansion
     * @param lower raw lowercased query
     * @return ClassificationDetail if matched, or null otherwise
     */
    ClassificationDetail classify(String normalized, String lower);

    /**
     * Priority of rule execution (lower number = higher priority).
     */
    default int priority() {
        return 100;
    }
}
