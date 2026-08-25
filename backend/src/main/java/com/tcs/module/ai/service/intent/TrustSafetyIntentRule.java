package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class TrustSafetyIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "lach san", "thu tien ngoai san", "chuyen khoan ngoai san", "chuyen khoan rieng", "ngoai san", "bao cao gia su lach san", "che tai lach san", "bao cao trung tam lach san")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.REPORT_CIRCUMVENTION, AiIntent.TICKET_SUPPORT, 0.95, "/help");
        }

        if (containsAny(normalized, "tranh chap", "mo tranh chap", "khi nao nen mo tranh chap", "tai bang chung tranh chap",
                "khieu nai gia su", "khieu nai lop hoc bi huy", "giai quyet tranh chap", "mo khieu nai", "khieu nai",
                "bo day", "gia su bo day", "bo tiet", "gia su bo tiet", "nghi day khong phep", "gia su khong den day", "khong den day", "bo ngang")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.DISPUTE_OPEN_HELP, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        if (containsAny(normalized, "bi phat", "phat canh cao", "che tai khi vi pham", "tru diem uy tin", "tai khoan bi phat", "quy dinh phat vi pham",
                "xu phat", "quy dinh xu phat", "vi pham quy che", "che tai xu phat", "quy che san", "quy dinh xu ly vi pham", "che tai")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.PENALTY_EXPLAIN, AiIntent.TICKET_SUPPORT, 0.95, "/help");
        }

        if (containsAny(normalized, "to cao vi pham", "bao cao nguoi dung vi pham", "to cao", "bao cao vi pham", "bao cao nguoi dung", "lua dao", "bi lua")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.REPORT_USER_CREATE, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        return null;
    }
}
