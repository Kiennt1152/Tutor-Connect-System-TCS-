package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

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
