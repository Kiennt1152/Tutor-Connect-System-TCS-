package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH XÁC THỰC CCCD & BẰNG CẤP (VERIFICATION INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Phân loại các câu hỏi về quy trình tải ảnh CCCD/bằng cấp, thời gian xét duyệt và trạng thái xác thực hồ sơ.
 * 
 * Chức năng chính:
 *   1. Nhận diện tải hồ sơ xác thực: Bắt từ khóa nộp bằng đại học, chụp căn cước công dân.
 *   2. Nhận diện tiến độ phê duyệt: Bắt từ khóa bao lâu được duyệt hồ sơ, kiểm duyệt tài khoản.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quét từ khóa liên quan đến chứng minh nhân thân và bằng cấp chuyên môn.
 *   - Bước 2: Phân loại sang AiDomain.VERIFICATION.
 *   - Bước 3: Trả về liên kết màn hình nộp hồ sơ xác thực.
 * ============================================================================
 */
@Component
public class VerificationIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized,
                "xac minh", "duyet ho so", "ho so bi tu choi", "trang thai xac minh", "bang cap chung chi", "xac minh cccd",
                "giay to cccd", "cccd khong hop le", "giay to gi de duyet", "chua duoc duyet", "ho so chua", "gui lai giay to xac minh",
                "trang thai duyet ho so", "tai sao ho so chua duoc duyet", "xac minh bang cap", "quy trinh xac minh") &&
            !containsAny(normalized, "tim gia su", "can gia su", "thue gia su", "co gia su", "ai day")) {
            return new ClassificationDetail(AiDomain.VERIFICATION, AiSubIntent.TUTOR_VERIFICATION_HELP, AiIntent.TUTOR_VERIFICATION, 0.95, "/profile");
        }

        return null;
    }
}
