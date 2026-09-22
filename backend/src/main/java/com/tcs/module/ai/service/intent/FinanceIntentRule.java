package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH TÀI CHÍNH & VÍ TIỀN (FINANCE INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Nhận diện các thắc mắc về số dư ví, nạp tiền học phí, rút tiền thu nhập và đối soát tiền bảo chứng Escrow.
 * 
 * Chức năng chính:
 *   1. Nhận diện nạp/rút tiền: Bắt từ khóa nạp ví VNPAY/SePay, rút tiền về tài khoản ngân hàng.
 *   2. Nhận diện bảo chứng Escrow: Giải thích cơ chế giữ tiền học phí an toàn và điều kiện hoàn tiền.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận câu hỏi về ví tiền hoặc giao dịch tài chính.
 *   - Bước 2: Phân loại ý định FINANCE_WALLET hoặc FINANCE_ESCROW.
 *   - Bước 3: Trả về AiDomain.FINANCE và liên kết tới trang ví /finance.
 * ============================================================================
 */
@Component
public class FinanceIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        // Incident / Delayed Wallet Top-up -> Route directly to Support Ticket Creation
        if (containsAny(normalized, "chua thay cong tien", "chua nhan duoc tien", "chua vao vi", "chua duoc cong tien",
                "chua cong tien", "chua thay vao so du", "chua thay vao vi", "loi nap tien", "su co nap tien",
                "2 tieng chua thay", "chuyen khoan lau", "nap tien bi loi", "nap vi bi loi", "chuyen tien ma chua thay",
                "chuyen khoan ma chua", "nap tien chua vao", "nap vi chua vao", "chua thay cong vao so du")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_CREATE, AiIntent.TICKET_SUPPORT, 0.98, "/messaging/tickets?action=create&subject=Sự+cố+nạp+tiền+chưa+cộng+số+dư");
        }

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
