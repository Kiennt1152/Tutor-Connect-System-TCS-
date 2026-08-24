package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class PlatformAdminIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized,
                "bao nhieu nguoi dung", "bn nguoi dung", "bao nhieu user", "bn user", "tong user", "so luong user", "tong so user",
                "bao nhieu tai khoan", "bn tai khoan", "co bao nhieu nguoi dung", "bao nhieu hoc vien", "tong so hoc vien",
                "co bao nhieu gia su", "so luong gia su", "bao nhieu gia su", "tong so gia su", "tong gia su",
                "co bao nhieu trung tam", "so luong trung tam", "bao nhieu trung tam", "tong so trung tam",
                "co bao nhieu lop", "so luong lop", "bao nhieu lop", "tong so lop", "tong lop",
                "co bao nhieu lop hoc", "bao nhieu lop hoc", "so luong lop hoc", "lop dang mo", "lop hoc dang mo",
                "he thong co bao nhieu", "tren he thong co bao nhieu", "thong ke he thong", "thong ke nen tang",
                "quy mo he thong", "quy mo nen tang", "thong ke nguoi dung")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.PLATFORM_STATS, AiIntent.PLATFORM_STATS, 0.95, null);
        }

        if (!normalized.contains("trung tam") && (containsAny(normalized,
                "bao cao doanh thu", "thong ke doanh thu", "doanh thu phi san", "cashflow", "money in money out", "doanh thu nen tang") ||
            (normalized.contains("doanh thu") && containsAny(normalized, "san", "nen tang", "he thong", "phi san", "bao cao", "thong ke", "dashboard")))) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_REVENUE_REPORT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
        }

        if (containsAny(normalized, "audit log", "nhat ky he thong", "nhat ky he thong audit log")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_AUDIT_LOG, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/audit-logs");
        }

        if (containsAny(normalized, "reindex ai", "reindex knowledge", "thong ke kien thuc ai", "ai knowledge base")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_AI_REINDEX, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
        }

        if (containsAny(normalized, "queue xac minh", "hang doi xac minh", "xac minh ho so admin", "queue xac minh ho so admin")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_VERIFICATION_QUEUE, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/verifications");
        }

        if (containsAny(normalized, "quan ly rut tien admin", "duyet rut tien admin")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_WITHDRAWAL_MANAGEMENT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/finance");
        }

        if (containsAny(normalized, "quan ly dispute admin", "xu ly tranh chap admin", "tranh chap admin")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DISPUTE_MANAGEMENT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/disputes");
        }

        if (containsAny(normalized, "xuat bao cao csv", "xuat bao cao csv admin", "csv export")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_CSV_EXPORT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
        }

        if (!normalized.contains("trung tam") && containsAny(normalized,
                "admin dashboard", "dashboard quan tri", "bang dieu khien quan tri", "analytics van hanh",
                "loc task qua han sla", "cau hinh he thong platform fee", "quan tri vien he thong", "dashboard")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DASHBOARD, AiIntent.ADMIN_DASHBOARD, 0.9, "/platform/analytics");
        }

        return null;
    }
}
