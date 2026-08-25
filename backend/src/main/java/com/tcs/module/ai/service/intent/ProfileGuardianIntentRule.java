package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class ProfileGuardianIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "ho so con", "ho so cho con", "child profile", "them ho so con", "tao ho so con", "tao ho so cho con")) {
            return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.CHILD_PROFILE_CREATE, AiIntent.FAQ_SUPPORT, 0.95, "/parent/students");
        }

        if (containsAny(normalized, "lien ket phu huynh", "nguoi giam ho", "lien ket tai khoan phu huynh", "xac nhan nguoi giam ho", "lien ket tai khoan", "lien ket bo me", "lien ket voi hoc sinh")) {
            return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.GUARDIAN_LINK_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/parent/students");
        }

        if (containsAny(normalized,
                "ho so ca nhan", "cap nhat ho so", "tai anh dai dien", "doi anh dai dien", "anh dai dien", "avatar", "doi avatar", "anh ca nhan", "thay anh dai dien",
                "quet can cuoc cong dan", "cccd",
                "kinh nghiem day hoc", "lich ranh", "viet bio", "chinh sua thong tin lien he", "cap nhat ho so gia su", "them lich ranh") &&
            !containsAny(normalized, "xac minh", "duyet", "tim gia su", "thue gia su", "can gia su")) {
            return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.PROFILE_UPDATE_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/profile");
        }

        return null;
    }
}
