package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH TÁC NGHIỆP DẠY HỌC GIA SƯ (TUTOR OPS INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Xử lý các câu hỏi về lịch dạy, điểm danh từng buổi, đăng ký giờ bận và nhận thù lao dạy học.
 * 
 * Chức năng chính:
 *   1. Nhận diện điểm danh: Bắt từ khóa điểm danh học viên, xác nhận buổi học đã diễn ra.
 *   2. Nhận diện lịch trình giảng dạy: Bắt từ khóa xem thời khóa biểu, đăng ký thời gian bận.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Nhận diện câu hỏi tác nghiệp giảng dạy của gia sư.
 *   - Bước 2: Phân loại vào AiDomain.TUTOR_OPS.
 *   - Bước 3: Cung cấp liên kết tới trang quản lý lịch dạy /tutor/schedule.
 * ============================================================================
 */
@Component
public class TutorOpsIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "diem danh", "diem danh hoc vien", "cach diem danh lop hoc", "huong dan diem danh")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_ATTENDANCE_MARK, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/schedule");
        }

        if (containsAny(normalized, "xin doi lich", "doi lich day", "xin nghi day", "doi gio hoc", "xin doi gio", "doi lich", "doi gio", "doi buoi", "doi buoi hoc")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_RESCHEDULE_REQUEST, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/schedule");
        }

        if (containsAny(normalized, "day thay", "day thay the", "tim nguoi day thay", "nho nguoi day thay", "nho day thay")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SUBSTITUTE_REQUEST, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/classes");
        }

        if (containsAny(normalized, "xem lich day", "lich day tuan nay", "lich day gia su", "lich day", "lich day cua gia su",
                "thoi khoa bieu", "thoi khoa bieu day", "thoi gian bieu", "lich giang day", "thoi gian day", "xem thoi khoa bieu",
                "quy trinh nhan lop day kem", "nhan lop day")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SCHEDULE_VIEW, AiIntent.FAQ_SUPPORT, 0.9, "/tutor/schedule");
        }

        return null;
    }
}
