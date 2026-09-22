package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * [UC-65] ĐĂNG KÝ & ĐIỀU PHỐI QUY TẮC Ý ĐỊNH (INTENT RULE REGISTRY)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Quản lý danh mục tập trung tất cả các quy tắc IntentRule và điều phối thực thi theo thứ tự ưu tiên.
 * 
 * Chức năng chính:
 *   1. Tập hợp quy tắc: Tự động phát hiện và đăng ký tất cả bean Spring thực thi IntentRule.
 *   2. Sắp xếp ưu tiên: Sắp xếp các quy tắc theo priority tăng dần để thực thi bộ lọc nhạy cảm trước.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Khởi tạo danh sách quy tắc khi khởi động ứng dụng Spring Boot.
 *   - Bước 2: Lặp qua từng quy tắc theo thứ tự ưu tiên khi nhận truy vấn người dùng.
 *   - Bước 3: Trả về kết quả phân loại đầu tiên đạt độ tin cậy thỏa mãn.
 * ============================================================================
 */
@Slf4j
@Component
public class IntentRuleRegistry {

    private final List<IntentRule> rules;

    public IntentRuleRegistry(List<IntentRule> rules) {
        if (rules != null) {
            this.rules = rules.stream()
                .sorted(Comparator.comparingInt(IntentRule::priority))
                .toList();
        } else {
            this.rules = Collections.emptyList();
        }
        log.info("Registered {} intent classification rules in strategy registry", this.rules.size());
    }

    /**
     * Evaluate rules in priority order until the first matching rule produces a result.
     * @return ClassificationDetail or null if no rule matched
     */
    public ClassificationDetail evaluate(String normalized, String lower) {
        for (IntentRule rule : rules) {
            ClassificationDetail result = rule.classify(normalized, lower);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public List<IntentRule> getRules() {
        return rules;
    }
}
