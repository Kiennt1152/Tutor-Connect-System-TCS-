package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH VẬN HÀNH TRUNG TÂM GIA SƯ (CENTER OPS INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Nhận diện các truy vấn liên quan đến quản lý lớp học, phân công gia sư và điều hành hoạt động của Trung tâm.
 * 
 * Chức năng chính:
 *   1. Nhận diện quản trị trung tâm: Nhận biết yêu cầu tạo lớp, quản lý hợp đồng và lịch dạy của trung tâm.
 *   2. Điều hướng trang quản trị: Cung cấp liên kết tới màn hình quản lý dành cho đối tác trung tâm.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận câu truy vấn và kiểm tra các từ khóa quản trị trung tâm.
 *   - Bước 2: Ánh xạ sang miền AiDomain.CENTER_OPS với độ tin cậy cao.
 *   - Bước 3: Trả về thông tin điều hướng trang quản trị trung tâm.
 * ============================================================================
 */
@Component
public class CenterOpsIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 35;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (normalized.contains("xac minh") || containsAny(normalized, "thanh toan", "hoc phi", "chuyen khoan qua", "thanh toan truc tiep", "dong tien", "nap tien", "tien hoc")) {
            return null;
        }

        if (containsAny(normalized, "tuyen dung gia su cho trung tam", "dang bai tuyen dung gia su", "tuyen dung gia su", "tao bai tuyen dung", "tuyen ung vien gia su", "tuyen dung")) {
            return new ClassificationDetail(AiDomain.CENTER_OPS, AiSubIntent.CENTER_RECRUITMENT_POST, AiIntent.CENTER_MANAGEMENT, 0.95, "/center/recruitment");
        }

        if (containsAny(normalized, "trung tam quan ly gia su", "thanh vien trung tam", "duyet gia su vao trung tam",
                "hop dong trung tam", "bao cao doanh thu trung tam", "lop nhom cho trung tam", "quan ly trung tam gia su",
                "danh sach gia su trung tam", "xoa gia su khoi trung tam", "them gia su vao trung tam",
                "quan ly gia su", "duyet ung vien", "tuyen ung vien") ||
            (normalized.contains("trung tam") && containsAny(normalized, "quan ly", "gia su", "doanh thu", "thanh vien", "hop dong", "ung vien", "duyet"))) {
            return new ClassificationDetail(AiDomain.CENTER_OPS, AiSubIntent.CENTER_TUTOR_MANAGEMENT, AiIntent.CENTER_MANAGEMENT, 0.95, "/center");
        }

        return null;
    }
}
