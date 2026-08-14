package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 6-level fallback engine for the TCS AI Assistant.
 * Inspired by production chatbot patterns (Rasa handoff, MS CLU None intent, Dialogflow fallback).
 */
@Service
public class AiFallbackService {

    public record FallbackResult(
        int level,
        String message,
        String suggestedRoute,
        List<String> clarificationOptions
    ) {}

    // Level 0: Deterministic Safety & Conversational responses (Fast-path, no LLM)
    private static final Map<AiSubIntent, String> LEVEL_0_MESSAGES = Map.of(
        AiSubIntent.GREETING,
        "Xin chào! Tôi là Trợ lý AI của Tutor Connect System (TCS). Tôi có thể giúp bạn tìm gia sư, tìm lớp học, hướng dẫn thanh toán Escrow hoặc hỗ trợ các quy trình trên hệ thống.",

        AiSubIntent.GOODBYE,
        "Tạm biệt bạn! Chúc bạn một ngày tốt lành và có trải nghiệm học tập hiệu quả cùng TCS.",

        AiSubIntent.THANKS,
        "Rất vui được hỗ trợ bạn! Nếu bạn cần tìm gia sư, lớp học hoặc giải đáp quy định hệ thống, đừng ngần ngại hỏi tôi nhé.",

        AiSubIntent.SMALL_TALK,
        "Tôi là Trợ lý AI thông minh của nền tảng TCS, sẵn sàng hỗ trợ kết nối học viên, phụ huynh và gia sư 24/7.",

        AiSubIntent.BOT_CAPABILITY_ASK,
        "Tôi có thể hỗ trợ bạn các công việc sau:\n" +
        "1. **Tìm gia sư & Lớp học**: Tìm gia sư theo môn, lớp, học phí và khu vực (/tim-gia-su, /lop-hoc).\n" +
        "2. **Tài chính & Escrow**: Hướng dẫn nạp ví, rút tiền, giải ngân ký quỹ và chính sách hoàn tiền (/finance).\n" +
        "3. **Hỗ trợ & Khiếu nại**: Hướng dẫn tạo ticket, báo cáo lách sàn hoặc mở tranh chấp (/support/tickets).\n" +
        "4. **Trợ giảng học tập**: Hướng dẫn giải bài tập và ôn luyện kiến thức.",

        AiSubIntent.PROFANITY_OR_FRUSTRATION,
        "TCS luôn hướng tới môi trường giao tiếp văn minh và tôn trọng lẫn nhau. Nếu bạn đang gặp sự cố hoặc bức xúc về dịch vụ, vui lòng mô tả chi tiết vấn đề để tôi hỗ trợ hoặc chuyển tiếp đội ngũ quản trị viên xử lý ngay nhé.",

        AiSubIntent.GIBBERISH,
        "Tôi chưa hiểu rõ yêu cầu của bạn. Bạn có thể thử đặt câu hỏi như:\n" +
        "• *'Tìm cho tôi gia sư môn Toán lớp 12 khu vực Cầu Giấy dưới 250k'*\n" +
        "• *'Làm sao để đăng ký làm gia sư?'*\n" +
        "• *'Khi nào tiền học phí được giải ngân?'*",

        AiSubIntent.HUMAN_SUPPORT_REQUEST,
        "Bạn có thể gửi yêu cầu hỗ trợ trực tiếp tới đội ngũ quản trị viên TCS bằng cách tạo phiếu hỗ trợ tại mục /support/tickets hoặc liên hệ hotline hỗ trợ."
    );

    /**
     * Level 0: Check if subIntent is a fast-path conversational/safety intent.
     */
    public FallbackResult checkLevel0Safety(AiSubIntent subIntent) {
        String msg = LEVEL_0_MESSAGES.get(subIntent);
        if (msg != null) {
            String route = subIntent == AiSubIntent.HUMAN_SUPPORT_REQUEST ? "/support/tickets" : null;
            return new FallbackResult(0, msg, route, List.of());
        }
        return null;
    }

    /**
     * Level 1: Clarification Fallback when intent is ambiguous.
     */
    public FallbackResult getLevel1Clarification(String originalQuery) {
        return new FallbackResult(
            1,
            "Tôi có thể hỗ trợ bạn theo các hướng sau. Vui lòng chọn nội dung bạn quan tâm:",
            null,
            List.of(
                "Tìm gia sư theo môn và khu vực",
                "Tìm lớp học đang tuyển gia sư",
                "Hướng dẫn thanh toán / Rút tiền ví",
                "Tạo ticket khiếu nại / Hỗ trợ"
            )
        );
    }

    /**
     * Level 2: Missing Slot Prompt when required search parameters are missing.
     */
    public FallbackResult getLevel2MissingSlots(AiSubIntent subIntent, Set<String> missingSlots) {
        if (subIntent == AiSubIntent.FIND_TUTOR && missingSlots.contains("subject")) {
            return new FallbackResult(
                2,
                "Bạn muốn tìm gia sư cho **môn học nào**, **khối lớp mấy** và tại **khu vực nào** (hoặc học Online)?",
                "/tim-gia-su",
                List.of("Gia sư Toán 12", "Gia sư Tiếng Anh IELTS", "Gia sư Vật lý 10")
            );
        }
        if (subIntent == AiSubIntent.FIND_CLASS && missingSlots.contains("subject")) {
            return new FallbackResult(
                2,
                "Bạn muốn tìm lớp dạy kèm cho **môn học nào** và tại **khu vực nào**?",
                "/lop-hoc",
                List.of("Lớp Toán đang mở", "Lớp Tiếng Anh đang mở", "Lớp Hóa đang mở")
            );
        }
        return new FallbackResult(2, "Vui lòng cung cấp thêm thông tin chi tiết để tôi hỗ trợ bạn chính xác nhất.", null, List.of());
    }

    /**
     * Level 3: Domain No-Data Fallback when database returns zero matched records.
     */
    public FallbackResult getLevel3NoData(AiSubIntent subIntent, Map<String, String> entities) {
        if (subIntent == AiSubIntent.FIND_TUTOR) {
            String location = entities.getOrDefault("location", "");
            String subject = entities.getOrDefault("subject", "");
            return new FallbackResult(
                3,
                "Hiện tại hệ thống chưa tìm thấy gia sư đã xác minh nào khớp hoàn toàn với tiêu chí " +
                (subject.isEmpty() ? "" : "môn " + subject + " ") +
                (location.isEmpty() ? "" : "tại " + location + " ") +
                "của bạn. Bạn có thể thử điều chỉnh mức học phí, mở rộng khu vực hoặc đăng bài tìm gia sư tại /tao-lop để nhận hồ sơ ứng tuyển nhé.",
                "/tao-lop",
                List.of("Đăng bài tìm gia sư (/tao-lop)", "Xem tất cả gia sư (/tim-gia-su)")
            );
        }
        if (subIntent == AiSubIntent.FIND_CLASS) {
            return new FallbackResult(
                3,
                "Hiện tại chưa có lớp học nào đang mở khớp với tiêu chí tìm kiếm của bạn. Bạn có thể theo dõi danh sách lớp mới tại mục /lop-hoc.",
                "/lop-hoc",
                List.of("Xem danh sách lớp học (/lop-hoc)")
            );
        }
        return new FallbackResult(
            3,
            "Hiện tại hệ thống chưa tìm thấy dữ liệu phù hợp với yêu cầu này. Vui lòng kiểm tra lại điều kiện lọc hoặc liên hệ bộ phận hỗ trợ.",
            null,
            List.of()
        );
    }

    /**
     * Level 4: Permission / Authentication Fallback.
     */
    public FallbackResult getLevel4AuthRoleRequired(String requiredRoleDesc, String targetRoute) {
        return new FallbackResult(
            4,
            "Để thực hiện thao tác này và xem dữ liệu cá nhân, bạn cần đăng nhập với vai trò **" + requiredRoleDesc + "**.",
            targetRoute != null ? targetRoute : "/login",
            List.of("Đăng nhập tài khoản (/login)", "Đăng ký tài khoản (/register)")
        );
    }

    /**
     * Level 5: Safety / Human Escalation Fallback after multiple unsuccessful attempts.
     */
    public FallbackResult getLevel5HumanEscalation() {
        return new FallbackResult(
            5,
            "Có vẻ vấn đề của bạn cần sự can thiệp trực tiếp từ bộ phận Chăm sóc khách hàng TCS. Bạn vui lòng tạo một phiếu hỗ trợ tại mục **Yêu cầu hỗ trợ (/support/tickets)** để chuyên viên liên hệ giải quyết sớm nhất.",
            "/support/tickets",
            List.of("Tạo ticket hỗ trợ (/support/tickets)", "Trung tâm trợ giúp (/help)")
        );
    }
}
