package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH HỒ SƠ & GIÁM HỘ HỌC VIÊN (PROFILE GUARDIAN INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Phân loại các câu hỏi về cập nhật hồ sơ cá nhân, liên kết tài khoản phụ huynh với học sinh và phê duyệt giám hộ.
 * 
 * Chức năng chính:
 *   1. Nhận diện cập nhật hồ sơ: Bắt từ khóa chỉnh sửa thông tin, đổi avatar, cập nhật tiểu sử.
 *   2. Nhận diện giám hộ học viên: Bắt từ khóa liên kết tài khoản con, xác nhận người giám hộ.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: So khớp các mẫu câu hỏi về quản lý hồ sơ và mối quan hệ giám hộ.
 *   - Bước 2: Phân loại sang AiDomain.PROFILE_GUARDIAN.
 *   - Bước 3: Trả về liên kết màn hình hồ sơ cá nhân /profile.
 * ============================================================================
 */
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
