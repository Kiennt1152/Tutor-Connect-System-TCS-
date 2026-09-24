package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH TÀI KHOẢN & XÁC THỰC (IDENTITY AUTH INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Phân loại các vấn đề đăng nhập, đăng ký tài khoản mới, quên mật khẩu và quản lý thông tin bảo mật.
 * 
 * Chức năng chính:
 *   1. Nhận diện xác thực tài khoản: Bắt từ khóa đăng nhập, tạo tài khoản gia sư/phụ huynh.
 *   2. Nhận diện khôi phục mật khẩu: Bắt từ khóa quên mật khẩu, đổi mật khẩu và mã OTP.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quét các mẫu câu hỏi liên quan đến tài khoản và quyền truy cập.
 *   - Bước 2: Gán miền AiDomain.IDENTITY_AUTH và gán độ tin cậy cao.
 *   - Bước 3: Cung cấp liên kết trang đăng nhập /login hoặc quên mật khẩu /forgot-password.
 * ============================================================================
 */
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
