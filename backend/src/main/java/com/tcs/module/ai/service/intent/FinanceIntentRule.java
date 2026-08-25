package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class FinanceIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "nap tien", "topup", "sepay", "nap qua qr", "nap vi", "phuong thuc thanh toan", "chuyen khoan nap tien", "noi dung chuyen khoan", "ghi sai noi dung")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_TOPUP, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "rut tien", "withdraw", "rut ve ngan hang", "rut tien luong")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WITHDRAWAL_REQUEST, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "escrow", "ky quy", "tam giu", "giai ngan", "tien escrow", "bao lau gia su nhan duoc tien", "nhan duoc tien", "dong ca thang", "thanh toan theo tung buoi", "thanh toan tung buoi", "tra trong bao lau", "bao lau nhan duoc", "bao lau thi nhan")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.ESCROW_EXPLAIN, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "phi san", "phi nen tang", "10%", "chiet khau")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.PLATFORM_FEE_EXPLAIN, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "hoan tien", "refund", "tra lai tien", "bung buoi", "bung lop", "lay lai tien", "lay lai hoc phi")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.REFUND_POLICY, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        boolean isGeneralMarketFee = containsAny(normalized, "trung binh", "khung hoc phi", "bang gia hoc phi", "muc hoc phi trung binh");
        if (!isGeneralMarketFee && containsAny(normalized, "vi tien cua toi", "so du vi", "luong cua toi", "xem lich su giao dich", "xem luong", "thu nhap cua toi", "vi cua toi", "thu nhap gia su", "xem luong gia su", "luong thang nay", "tien trong vi", "so du")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_VIEW, AiIntent.PAYMENT_SUPPORT, 0.9, "/finance");
        }

        return null;
    }
}
