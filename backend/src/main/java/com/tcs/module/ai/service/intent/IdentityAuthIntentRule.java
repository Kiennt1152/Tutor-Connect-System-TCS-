package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class IdentityAuthIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 32;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "quen mat khau", "doi mat khau", "reset password", "quen tai khoan", "toi quen mat khau", "quen pass", "mat khau quen mat khau", "quen mat khau tai khoan")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.PASSWORD_FORGOT_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/login");
        }

        if (containsAny(normalized, "dang ky tai khoan", "dang ky lam gia su", "dang ky trung tam", "huong dan dang ky", "tao tai khoan", "tao tai khoan moi", "dang ky tao tai khoan", "dang ky") &&
            !containsAny(normalized, "dang ky lop", "dang ky hoc", "dang ky tim gia su", "quy trinh dang ky", "quy trinh tim gia su")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.REGISTER_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/register");
        }

        if (containsAny(normalized, "ma otp", "otp xac thuc", "nhap ma otp", "khong nhan duoc ma otp", "ma xac thuc otp", "ma xac thuc") &&
            !containsAny(normalized, "hop dong", "ky hop dong")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.OTP_SEND_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/login");
        }

        if (containsAny(normalized, "dang nhap", "tai khoan bi khoa", "dang nhap bang google", "het han phien", "khong co quyen truy cap", "login")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.LOGIN_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/login");
        }

        return null;
    }
}
