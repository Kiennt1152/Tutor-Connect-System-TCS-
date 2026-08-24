package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class CatalogFaqIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 110;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized,
                "luong trung binh", "hoc phi trung binh", "thu nhap trung binh",
                "bang gia hoc phi", "muc hoc phi trung binh", "khung hoc phi", "gia su kiem duoc bao nhieu")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.95, "/tim-gia-su");
        }

        if (containsAny(normalized,
                "trung tam tro giup", "tro giup o dau", "mon hoc nao", "khoi lop nao", "co nhung khoi lop", "khu vuc nao", "quy trinh ket noi",
                "gioi thieu ve tcs", "tcs la gi", "he thong tcs hoat dong", "he thong hoat dong", "hoat dong nhu the nao", "hoat dong ra sao", "mo hinh hoat dong", "he thong ket noi",
                "cac vai tro", "chinh sach nen tang", "cac mon hoc tren tcs", "huong dan su dung tcs", "chinh sach bao mat",
                "huong dan su dung", "huong dan tcs", "huong dan he thong", "cach dung", "quy trinh", "chinh sach", "faq", "ho tro chung",
                "vai tro", "tinh nang san", "cac mon hoc")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.9, "/help");
        }

        return null;
    }
}
