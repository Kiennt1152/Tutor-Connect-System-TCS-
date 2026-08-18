package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 6-level hierarchical fallback engine for the TCS AI Assistant.
 * Granular slot-aware, context-sensitive clarification & actionable guidance.
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
        "• *'Có lớp dạy Tiếng Việt nào đang mở không?'*\n" +
        "• *'Làm sao để nạp tiền vào ví Escrow?'*",

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
     * Level 1: Context-sensitive Clarification Fallback when intent is ambiguous.
     */
    public FallbackResult getLevel1Clarification(String originalQuery) {
        String normalized = originalQuery != null ? VietnameseTextNormalizer.removeDiacritics(originalQuery.toLowerCase()) : "";

        // Xác minh hồ sơ & Giấy tờ
        if (containsAny(normalized, "cccd", "can cuoc", "bang cap", "chung chi", "xac minh", "duyet ho so", "kyc", "giay to")) {
            return new FallbackResult(
                1,
                "Bạn cần hỗ trợ về quy trình xác minh hồ sơ? Vui lòng chọn nội dung cụ thể:",
                "/profile",
                List.of(
                    "Quy trình xác minh bằng cấp & CCCD (/profile)",
                    "Thời gian duyệt hồ sơ gia sư (24-48h)",
                    "Hướng dẫn khi hồ sơ bị từ chối (/support/tickets)"
                )
            );
        }

        // Hợp đồng & OTP
        if (containsAny(normalized, "hop dong", "ky otp", "ma otp", "cam ket", "dieu khoan", "chu ky")) {
            return new FallbackResult(
                1,
                "Bạn cần hỗ trợ về hợp đồng điện tử? Vui lòng chọn nội dung cụ thể:",
                "/contracts",
                List.of(
                    "Danh sách hợp đồng điện tử (/contracts)",
                    "Hướng dẫn ký hợp đồng bằng mã OTP",
                    "Điều khoản cam kết chất lượng (/help)"
                )
            );
        }

        // Vận hành giảng dạy
        if (containsAny(normalized, "diem danh", "doi lich", "day thay", "nghi day", "bao nghi", "gio day", "lich day")) {
            return new FallbackResult(
                1,
                "Bạn cần hỗ trợ về lịch giảng dạy? Vui lòng chọn nội dung cụ thể:",
                "/tutor/classes",
                List.of(
                    "Điểm danh buổi học (/tutor/classes)",
                    "Xem thời khóa biểu (/tutor/schedule)",
                    "Quy trình xin dời lịch / dạy thay"
                )
            );
        }

        if (containsAny(normalized, "tien", "vi", "nap", "rut", "escrow", "phi",
                "bang gia", "thu lao", "chiet khau", "hoa don", "sepay", "chuyen khoan")) {
            return new FallbackResult(
                1,
                "Có vẻ bạn đang quan tâm đến vấn đề Tài chính & Thanh toán. Vui lòng chọn nội dung cụ thể bạn cần hỗ trợ:",
                "/finance",
                List.of(
                    "Hướng dẫn nạp tiền ví qua QR SePay (/finance)",
                    "Quy trình rút tiền về tài khoản ngân hàng (/finance)",
                    "Chính sách bảo vệ học phí ký quỹ Escrow (/help)",
                    "Chính sách hoàn tiền khi hủy lớp học (/help)"
                )
            );
        }

        if (normalized.contains("lop") || normalized.contains("gia su") || normalized.contains("hoc") || normalized.contains("day")) {
            return new FallbackResult(
                1,
                "Tôi có thể hỗ trợ bạn tìm kiếm và kết nối lớp học. Vui lòng chọn hướng bạn muốn thực hiện:",
                "/tim-gia-su",
                List.of(
                    "Tìm hồ sơ gia sư uy tín (/tim-gia-su)",
                    "Xem danh sách lớp học đang mở (/lop-hoc)",
                    "Đăng bài tạo lớp mới tìm gia sư (/tao-lop)",
                    "Hướng dẫn quy trình đăng ký làm gia sư (/register)"
                )
            );
        }

        if (normalized.contains("khieu nai") || normalized.contains("tranh chap") || normalized.contains("bao cao") || normalized.contains("ticket") || normalized.contains("loi")) {
            return new FallbackResult(
                1,
                "Bạn cần hỗ trợ xử lý sự cố hoặc khiếu nại? Vui lòng chọn nội dung bên dưới:",
                "/support/tickets",
                List.of(
                    "Tạo phiếu yêu cầu hỗ trợ (Ticket) (/support/tickets)",
                    "Báo cáo hành vi lách sàn / vi phạm quy định",
                    "Quy trình mở tranh chấp lớp học",
                    "Tra cứu thời gian cam kết phản hồi SLA"
                )
            );
        }

        if (normalized.contains("bai tap") || normalized.contains("giai bai") || normalized.contains("luyen tap") || normalized.contains("kien thuc")) {
            return new FallbackResult(
                1,
                "Bạn cần hỗ trợ giải đáp bài tập hoặc học tập? Tôi có thể giúp bạn:",
                "/tim-gia-su",
                List.of(
                    "Tìm gia sư dạy kèm 1-1 (/tim-gia-su)",
                    "Giải bài tập & trợ giảng cùng AI",
                    "Xem danh sách lớp học đang mở (/lop-hoc)",
                    "Trung tâm trợ giúp học tập (/help)"
                )
            );
        }

        if (normalized.contains("ngay") || normalized.contains("gio") || normalized.contains("thoi gian") || normalized.contains("lich")) {
            return new FallbackResult(
                1,
                "Nếu bạn đang lên lịch học, TCS có thể giúp bạn kết nối gia sư linh hoạt thời gian:",
                "/tim-gia-su",
                List.of(
                    "Tìm gia sư có lịch phù hợp (/tim-gia-su)",
                    "Xem lịch dạy của gia sư (/tutor/schedule)",
                    "Đăng bài tạo lớp theo khung giờ cụ thể (/tao-lop)"
                )
            );
        }

        return new FallbackResult(
            1,
            "Tôi có thể hỗ trợ bạn theo các hướng sau. Vui lòng chọn nội dung bạn quan tâm:",
            null,
            List.of(
                "Tìm gia sư theo môn và khu vực (/tim-gia-su)",
                "Tìm lớp học đang tuyển gia sư (/lop-hoc)",
                "Hướng dẫn nạp tiền / Rút tiền ví (/finance)",
                "Tạo ticket khiếu nại / Hỗ trợ (/support/tickets)"
            )
        );
    }

    /**
     * Level 2: Missing Slot Prompt with context-aware parameter suggestions.
     */
    public FallbackResult getLevel2MissingSlots(AiSubIntent subIntent, Set<String> missingSlots) {
        if (subIntent == AiSubIntent.FIND_TUTOR && missingSlots.contains("subject")) {
            return new FallbackResult(
                2,
                "Bạn muốn tìm gia sư cho **môn học nào** (ví dụ: Toán, Lý, Hóa, Tiếng Anh, Tiếng Việt...), **khối lớp mấy** và tại **khu vực nào** (hoặc học Online)?",
                "/tim-gia-su",
                List.of("Gia sư Toán 12 Cầu Giấy", "Gia sư Tiếng Anh IELTS Online", "Gia sư Tiếng Việt Tiểu học", "Gia sư Vật lý 10")
            );
        }
        if (subIntent == AiSubIntent.FIND_CLASS && missingSlots.contains("subject")) {
            return new FallbackResult(
                2,
                "Bạn muốn tìm lớp dạy kèm cho **môn học nào** và tại **khu vực nào** (hoặc lớp Online)?",
                "/lop-hoc",
                List.of("Lớp Toán đang mở", "Lớp Tiếng Anh đang mở", "Lớp Tiếng Việt đang mở", "Lớp Hóa đang mở")
            );
        }
        return new FallbackResult(2, "Vui lòng cung cấp thêm thông tin chi tiết về môn học, khối lớp hoặc khu vực để tôi hỗ trợ bạn chính xác nhất.", null, List.of());
    }

    /**
     * Level 3: Domain No-Data Fallback with helpful Call-To-Action (CTA).
     */
    public FallbackResult getLevel3NoData(AiSubIntent subIntent, Map<String, String> entities) {
        return getLevel3EnhancedNoData(subIntent, entities);
    }

    /**
     * Level 3.5: Enhanced No-Data Fallback with specific subject/location/certLevel context
     */
    public FallbackResult getLevel3EnhancedNoData(AiSubIntent subIntent, Map<String, String> entities) {
        String subject = entities != null ? entities.getOrDefault("subject", "") : "";
        String location = entities != null ? entities.getOrDefault("location", "") : "";
        String certLevel = entities != null ? entities.getOrDefault("certLevel", "") : "";
        String grade = entities != null ? entities.getOrDefault("grade", "") : "";
        
        StringBuilder criteria = new StringBuilder();
        if (!subject.isEmpty()) criteria.append("môn ").append(subject).append(" ");
        if (!certLevel.isEmpty()) criteria.append("(").append(certLevel).append(") ");
        if (!grade.isEmpty()) criteria.append("lớp ").append(grade).append(" ");
        if (!location.isEmpty()) criteria.append("khu vực ").append(location).append(" ");
        
        String criteriaText = criteria.length() > 0 ? criteria.toString().trim() : "yêu cầu này";
        
        if (subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) {
            return new FallbackResult(
                3,
                String.format(
                    "Hiện tại hệ thống TCS **chưa tìm thấy gia sư phù hợp** với tiêu chí %s.\n\n" +
                    "📌 **Giải pháp dành cho bạn:**\n" +
                    "• [Đăng tin tạo lớp](/tao-lop): Miễn phí, các gia sư sẽ chủ động liên hệ trong vòng 24h.\n" +
                    "• [Xem tất cả gia sư](/tim-gia-su): Mở rộng điều kiện lọc (Online, khu vực lân cận, học phí cao hơn).",
                    criteriaText
                ),
                "/tao-lop",
                List.of("Đăng tin tìm gia sư miễn phí (/tao-lop)", "Xem tất cả gia sư (/tim-gia-su)")
            );
        }
        
        if (subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) {
            return new FallbackResult(
                3,
                String.format(
                    "Hiện tại **chưa có lớp học nào đang mở** khớp với tiêu chí %s.\n\n" +
                    "📌 **Bạn có thể:**\n" +
                    "• [Xem danh sách lớp học](/lop-hoc): Theo dõi lớp mới được đăng liên tục.\n" +
                    "• [Tìm gia sư dạy kèm 1-1](/tim-gia-su): Linh hoạt hơn về lịch học và nội dung.",
                    criteriaText
                ),
                "/lop-hoc",
                List.of("Xem danh sách lớp học (/lop-hoc)", "Tìm gia sư 1-1 (/tim-gia-su)")
            );
        }
        
        return new FallbackResult(
            3,
            "Hiện tại hệ thống chưa tìm thấy dữ liệu phù hợp với yêu cầu của bạn. Vui lòng thử điều chỉnh tiêu chí tìm kiếm hoặc liên hệ bộ phận hỗ trợ.",
            "/help",
            List.of("Trung tâm trợ giúp (/help)", "Tạo ticket hỗ trợ (/support/tickets)")
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

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
