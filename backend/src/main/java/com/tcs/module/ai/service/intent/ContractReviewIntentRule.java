package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH HỢP ĐỒNG & ĐÁNH GIÁ (CONTRACT REVIEW INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Xử lý ý định tra cứu hợp đồng dạy học, ký kết điện tử, hủy hợp đồng và viết đánh giá nhận xét gia sư.
 * 
 * Chức năng chính:
 *   1. Nhận diện nghiệp vụ hợp đồng: Bắt từ khóa về hợp đồng dạy học, điều khoản cam kết, ký số OTP.
 *   2. Nhận diện đánh giá & khiếu nại: Bắt từ khóa viết review, chấm điểm sao và phản ánh chất lượng.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quét từ khóa liên quan đến hợp đồng và đánh giá trong câu hỏi.
 *   - Bước 2: Xác định ý định CONTRACT_VIEW hoặc REVIEW_SUBMIT.
 *   - Bước 3: Trả về AiDomain.CONTRACT_REVIEW kèm liên kết trang hợp đồng.
 * ============================================================================
 */
@Component
public class ContractReviewIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "ky hop dong", "ky otp", "sign contract", "hop dong dien tu", "ky hop dong bang ma otp", "ky hop dong bang otp")) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.CONTRACT_SIGN_OTP, AiIntent.FAQ_SUPPORT, 0.95, "/contracts");
        }

        if (containsAny(normalized, "danh gia gia su", "viet review", "danh gia buoi day", "vi sao review bi an", "rating va review") ||
            (containsAny(normalized, "danh gia", "review") && !normalized.contains("trung tam"))) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.REVIEW_CREATE_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/classes");
        }

        if (containsAny(normalized, "uy tin gia su", "do uy tin gia su", "reputation", "diem uy tin", "xem diem uy tin", "tinh diem uy tin", "xem diem uy tin gia su", "cach tinh diem uy tin") ||
            (normalized.contains("uy tin") && containsAny(normalized, "gia su", "giao vien", "thay", "co", "diem", "xem", "tinh"))) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.REPUTATION_VIEW_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/profile");
        }

        if (containsAny(normalized, "danh sach hop dong", "xem hop dong", "tu choi hop dong", "hop dong lop hoc") ||
            (normalized.contains("hop dong") && !normalized.contains("trung tam"))) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.CONTRACT_LIST_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/contracts");
        }

        return null;
    }
}
