package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class CenterOpsIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 35;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (!normalized.contains("xac minh") && (containsAny(normalized, "trung tam quan ly gia su", "thanh vien trung tam", "tuyen dung gia su cho trung tam", "duyet gia su vao trung tam",
                "hop dong trung tam", "bao cao doanh thu trung tam", "lop nhom cho trung tam", "quan ly trung tam gia su", "tuyen ung vien gia su",
                "tao bai tuyen dung", "danh sach gia su trung tam", "xoa gia su khoi trung tam", "them gia su vao trung tam",
                "quan ly gia su", "duyet ung vien", "tuyen ung vien") ||
            (normalized.contains("trung tam") && containsAny(normalized, "quan ly", "gia su", "tuyen", "doanh thu", "thanh vien", "hop dong", "ung vien", "duyet")))) {
            return new ClassificationDetail(AiDomain.CENTER_OPS, AiSubIntent.CENTER_TUTOR_MANAGEMENT, AiIntent.CENTER_MANAGEMENT, 0.95, "/center");
        }

        return null;
    }
}
