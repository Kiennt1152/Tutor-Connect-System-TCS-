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
        return 108;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        // 1. Pricing & Fee Benchmarks (High Priority)
        if (containsAny(normalized,
                "luong trung binh", "hoc phi trung binh", "thu nhap trung binh",
                "bang gia hoc phi", "muc hoc phi trung binh", "khung hoc phi", "gia su kiem duoc bao nhieu",
                "chi phi thue gia su", "chi phi gia su", "gia mot buoi", "gia 1 buoi", "gia bao nhieu mot buoi",
                "hoc phi bao nhieu", "bao nhieu mot buoi", "bao nhieu 1 buoi", "chi phi bao nhieu",
                "gia bao nhieu", "muc gia mot buoi", "hoc phi mot buoi")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.95, "/tim-gia-su");
        }

        // 2. Platform Policies & Operational Inquiries
        if (containsAny(normalized,
                "day online khong", "co day online khong", "co gia su day online khong", "online hay tai nha", "tai nha hay online",
                "day online hay", "hinh thuc day", "day truc tuyen",
                "hoc thu", "hoc thu 1 buoi", "doi gia su", "khong hop thi doi", "khong hop co duoc doi", "doi gia su khac",
                "tieu chuan tuyen chon", "tieu chuan gia su", "linh vuc nao duoc kiem duyet", "kiem duyet gia su", "dieu kien lam gia su", "tieu chuan chon gia su",
                "quy trinh dang ky tim gia su", "quy trinh tim gia su", "cach dang ky tim gia su", "cach thue gia su", "cac buoc tim gia su",
                "thanh toan truc tiep hay chuyen khoan", "chuyen tien cho gia su hay trung tam", "thanh toan truc tiep", "thanh toan qua dau", "thanh toan hoc phi nhu the nao")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.95, "/help");
        }

        // 3. General TCS Information
        if (containsAny(normalized,
                "trung tam tro giup", "tro giup o dau", "mon hoc nao", "khoi lop nao", "co nhung khoi lop", "khu vuc nao", "quy trinh ket noi",
                "gioi thieu ve tcs", "tcs la gi", "he thong tcs hoat dong", "he thong hoat dong", "hoat dong nhu the nao", "hoat dong ra sao", "mo hinh hoat dong", "he thong ket noi",
                "cac vai tro", "chinh sach nen tang", "cac mon hoc tren tcs", "huong dan su dung tcs", "chinh sach bao mat",
                "huong dan su dung", "huong dan tcs", "huong dan he thong", "cach dung", "quy trinh chung", "chinh sach chung", "faq", "ho tro chung",
                "vai tro", "tinh nang san", "cac mon hoc")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.9, "/help");
        }

        return null;
    }
}
