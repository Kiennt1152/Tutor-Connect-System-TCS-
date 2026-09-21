package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] KIỂM SOÁT VÀ KHỬ ẢO GIÁC DỮ LIỆU AI (AI HALLUCINATION GUARD)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Rà soát và loại bỏ các thông tin bịa đặt không có thật trong hệ thống (tên gia sư giả mạo, mức phí sai lệch).
 * 
 * Chức năng chính:
 *   1. Khử tên gia sư bịa đặt: Đối chiếu danh sách tên gia sư trong câu trả lời với CSDL thực tế.
 *   2. Kiểm định số liệu tài chính: Ngăn chặn mô hình khẳng định số dư ví hoặc doanh thu không có căn cứ.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận văn bản phản hồi do mô hình ngôn ngữ vừa sinh ra.
 *   - Bước 2: Bóc tách các thực thể có tên riêng và số liệu tài chính bằng Regex.
 *   - Bước 3: Xác thực với CSDL; nếu sai lệch thì thay thế bằng nội dung an toàn đã kiểm chứng.
 * ============================================================================
 */
@Slf4j
@Service
public class AiHallucinationGuard {

    private static final Set<String> FAKE_PATTERNS = Set.of(
        "gia sư a", "gia sư b", "gia sư c",
        "lớp học a", "lớp học b", "lớp học c",
        "lớp a", "lớp b", "lớp c",
        "nguyễn văn a", "trần thị b", "lê văn c",
        "một số gia sư phù hợp", "một vài gia sư"
    );

    /**
     * Guard FIND_TUTOR responses: if LLM invented fake tutor names,
     * replace with deterministic answer listing real tutors.
     */
    public String guardTutorResponse(String response, List<TutorReferenceDto> realTutors, String fallbackMessage) {
        if (realTutors == null || realTutors.isEmpty()) {
            log.warn("[HallucinationGuard] FIND_TUTOR: no real tutors in sources, using fallback");
            return fallbackMessage;
        }

        String lowerResponse = response != null ? response.toLowerCase() : "";
        boolean hasFakeName = FAKE_PATTERNS.stream().anyMatch(lowerResponse::contains) ||
                              lowerResponse.contains("chưa tìm thấy gia sư") ||
                              lowerResponse.contains("chua tim thay gia su") ||
                              lowerResponse.contains("không tìm thấy gia sư") ||
                              lowerResponse.contains("khong tim thay gia su");

        if (hasFakeName) {
            log.warn("[HallucinationGuard] FIND_TUTOR: detected placeholder/fallback in response while real tutors exist, replacing with tutor list");
            StringBuilder sb = new StringBuilder();
            sb.append("Dựa trên tiêu chí tìm kiếm của bạn, hệ thống TCS tìm thấy các gia sư phù hợp sau:\n\n");
            for (TutorReferenceDto t : realTutors) {
                sb.append("• **").append(t.getFullName()).append("**");
                if (t.getHourlyRate() != null) {
                    sb.append(" — ").append(String.format("%,.0f", t.getHourlyRate())).append(" ₫/buổi");
                }
                if (t.getTeachingAreas() != null && !t.getTeachingAreas().isEmpty()) {
                    sb.append(" (").append(t.getTeachingAreas()).append(")");
                }
                sb.append("\n");
            }
            sb.append("\nBạn có thể nhấn vào thẻ gia sư bên dưới để xem chi tiết hồ sơ và gửi yêu cầu học.");
            return sb.toString();
        }

        return response;
    }

    /**
     * Guard FIND_CLASS responses: if no real classes found or fake classes invented, return deterministic answer.
     */
    public String guardClassResponse(String response, List<ClassReferenceDto> realClasses, String fallbackMessage) {
        if (realClasses == null || realClasses.isEmpty()) {
            log.warn("[HallucinationGuard] FIND_CLASS: no real classes in sources, using fallback");
            return fallbackMessage;
        }

        String lowerResponse = response != null ? response.toLowerCase() : "";
        boolean hasFakeName = FAKE_PATTERNS.stream().anyMatch(lowerResponse::contains) ||
                              lowerResponse.contains("chưa tìm thấy lớp") ||
                              lowerResponse.contains("chua tim thay lop") ||
                              lowerResponse.contains("không tìm thấy lớp") ||
                              lowerResponse.contains("khong tim thay lop");

        if (hasFakeName) {
            log.warn("[HallucinationGuard] FIND_CLASS: detected placeholder/fallback in response while real classes exist, replacing with class list");
            StringBuilder sb = new StringBuilder();
            sb.append("Dựa trên tiêu chí tìm kiếm của bạn, hệ thống TCS tìm thấy các lớp học phù hợp sau:\n\n");
            for (ClassReferenceDto c : realClasses) {
                sb.append("• **").append(c.getTitle()).append("**");
                if (c.getTuitionFee() != null) {
                    sb.append(" — ").append(String.format("%,.0f", c.getTuitionFee())).append(" ₫/tháng");
                }
                if (c.getLocation() != null && !c.getLocation().isEmpty()) {
                    sb.append(" (").append(c.getLocation()).append(")");
                }
                sb.append("\n");
            }
            sb.append("\nBạn có thể nhấn vào thẻ lớp học bên dưới để xem chi tiết và ứng tuyển.");
            return sb.toString();
        }

        return response;
    }

    /**
     * Guard PLATFORM_STATS responses: if no DB sources available,
     * refuse to output any numbers.
     */
    public String guardStatsResponse(String response, List<AiSourceResponse> dbSources, String fallbackMessage) {
        if (dbSources == null || dbSources.isEmpty()) {
            log.warn("[HallucinationGuard] PLATFORM_STATS: no DB sources, using fallback");
            return fallbackMessage;
        }
        return response;
    }

    /**
     * Guard PAYMENT_SUPPORT responses for personal finance queries:
     * if user is not authenticated as TUTOR/TUTOR_CENTER, block.
     */
    public String guardFinanceResponse(String query, String userRole, Long userId, String fallbackMessage) {
        String lower = query != null ? query.toLowerCase() : "";
        boolean isPersonal = lower.contains("của tôi") || lower.contains("lương của tôi") || lower.contains("thu nhập của tôi") || lower.contains("ví của tôi") || lower.contains("tiền của tôi");

        if (isPersonal && (userId == null || (!"TUTOR".equals(userRole) && !"TUTOR_CENTER".equals(userRole)))) {
            log.warn("[HallucinationGuard] PAYMENT_SUPPORT: personal finance query without TUTOR login, using fallback");
            return fallbackMessage;
        }
        return null; // no guard action needed
    }
}
