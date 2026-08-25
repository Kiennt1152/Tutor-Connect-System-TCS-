package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

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
