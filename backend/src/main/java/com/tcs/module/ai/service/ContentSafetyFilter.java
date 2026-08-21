package com.tcs.module.ai.service;

import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ContentSafetyFilter {

    public record SafetyCheckResult(
        boolean isSafe,
        String reason,
        String suggestedResponse,
        boolean isCrisis
    ) {}

    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
        "che tao bom", "che tao vu khi", "thuoc no", "chat no", "sung dan", "bom thuong",
        "ma tuy", "heroin", "thuoc phien", "can sa", "ke da", "mua ban ma tuy",
        "hack pass", "hack facebook", "ddos server", "hack the tin dung", "tan cong mang",
        "lua dao chiem doat", "khieu dam", "doi truy", "sex clip"
    );

    private static final Set<String> CRISIS_KEYWORDS = Set.of(
        "tu tu", "tu sat", "muon chet", "cat co tay", "tu hai", "ket thuc cuoc doi",
        "suicide", "self harm", "kill myself", "tram cam muon chet"
    );

    private static final Set<String> PRIVACY_DATA_EXFILTRATION_PATTERNS = Set.of(
        "danh sach acc", "danh sach user", "danh sach tai khoan", "danh sach nguoi dung",
        "tat ca acc", "tat ca user", "tat ca tai khoan", "tat ca nguoi dung",
        "toan bo acc", "toan bo user", "toan bo tai khoan", "toan bo nguoi dung",
        "lay tat ca acc", "lay danh sach acc", "lay toan bo acc", "lay tat ca user",
        "lay danh sach user", "lay toan bo user", "lay tat ca tai khoan", "lay danh sach tai khoan",
        "lay toan bo tai khoan", "lay tat ca nguoi dung", "lay danh sach nguoi dung",
        "dump database", "dump user", "dump acc", "xuat toan bo database", "lay database",
        "danh sach mat khau", "xem mat khau", "lay mat khau", "danh sach email",
        "export all users", "get all users", "list all accounts", "dump all accounts"
    );

    private static final Set<String> ROLEPLAY_ADMIN_PATTERNS = Set.of(
        "gia su toi la admin", "gia su minh la admin", "gia su em la admin",
        "gia su la admin", "dong vai admin", "dong vai quan tri vien",
        "gia vo la admin", "gia vo lam admin", "coi nhu toi la admin",
        "coi nhu minh la admin", "neu toi la admin", "neu minh la admin",
        "toi la admin he thong hay", "toi la admin hay cho toi", "toi la admin hay dua",
        "act as admin", "pretend you are admin", "assume i am admin"
    );

    public SafetyCheckResult checkQuery(String query) {
        if (query == null || query.isBlank()) {
            return new SafetyCheckResult(true, null, null, false);
        }

        String lower = query.toLowerCase(Locale.ROOT).trim();
        String normalized = VietnameseTextNormalizer.removeDiacritics(lower);

        // 1. Check Crisis Topics (Suicide & Self-Harm) -> Return compassionate emergency helpline
        for (String kw : CRISIS_KEYWORDS) {
            if (normalized.contains(kw)) {
                String crisisMessage = "Nếu bạn hoặc ai đó đang cảm thấy bế tắc, áp lực hay gặp khó khăn về mặt tâm lý, xin hãy nhớ rằng bạn không hề đơn độc. Đội ngũ chuyên gia luôn sẵn sàng lắng nghe và đồng hành cùng bạn 24/7:\n\n" +
                        "📞 **Tổng đài Quốc gia Bảo vệ Trẻ em & Học sinh**: `111` (Miễn phí)\n" +
                        "📞 **Đường dây nóng Sức khỏe Tâm thần Quốc gia**: `1800 599 920`\n" +
                        "📞 **Đường dây nóng Tư vấn Tâm lý Ngày Mai**: `1900 599 920`\n\n" +
                        "Hãy giữ bình tĩnh và liên hệ với các kênh trên hoặc người thân ngay bạn nhé. Bạn luôn có thể quay lại trò chuyện học tập cùng TCS bất cứ lúc nào.";
                return new SafetyCheckResult(false, "CRISIS_TOPIC", crisisMessage, true);
            }
        }

        // 2. Check Blocked Dangerous Topics (Weapons, Illegal Drugs, Cyber Attacks)
        for (String kw : BLOCKED_KEYWORDS) {
            if (normalized.contains(kw)) {
                String blockedMessage = "Yêu cầu của bạn vi phạm Chính sách An toàn & Tiêu chuẩn Cộng đồng của hệ thống TCS. " +
                        "Hệ thống không cung cấp thông tin hoặc hướng dẫn liên quan đến các nội dung nguy hại, vi phạm pháp luật.\n\n" +
                        "Nếu bạn cần hỗ trợ tìm kiếm gia sư, tham gia lớp học hoặc giải đáp thắc mắc học tập, vui lòng đặt câu hỏi liên quan đến dịch vụ của TCS.";
                return new SafetyCheckResult(false, "BLOCKED_CONTENT", blockedMessage, false);
            }
        }

        // 3. Check Privacy, Data Protection & Admin Roleplay Injection
        boolean isDataExfiltration = containsAny(normalized, PRIVACY_DATA_EXFILTRATION_PATTERNS);
        boolean isRoleplayAdmin = containsAny(normalized, ROLEPLAY_ADMIN_PATTERNS);

        if (isDataExfiltration || isRoleplayAdmin) {
            if (isDataExfiltration || containsAny(normalized, "acc", "user", "tai khoan", "nguoi dung", "database", "mat khau", "du lieu", "danh sach", "thong tin")) {
                String privacyMessage = "Vì lý do bảo mật thông tin và quyền riêng tư theo chính sách của TCS, " +
                        "Trợ lý AI không được phép truy xuất hoặc cung cấp danh sách tài khoản, mật khẩu hay dữ liệu cá nhân của người dùng trên hệ thống.\n\n" +
                        "🔒 **Dành cho Quản trị viên (Platform Admin):**\n" +
                        "• Nếu bạn là Quản trị viên có thẩm quyền, vui lòng đăng nhập tài khoản Quản trị và truy cập trực tiếp vào [Quản lý Người dùng](/platform/users) hoặc [Bảng điều khiển Quản trị](/platform/analytics).\n" +
                        "• Mọi thao tác tra cứu và quản lý dữ liệu cần được thực hiện trực tiếp trên giao diện quản trị với phiên xác thực hợp lệ theo quy định phân quyền (RBAC).";
                return new SafetyCheckResult(false, "PRIVACY_AND_ACCESS_RESTRICTED", privacyMessage, false);
            }
        }

        return new SafetyCheckResult(true, null, null, false);
    }

    private boolean containsAny(String text, Set<String> patterns) {
        for (String p : patterns) {
            if (text.contains(p)) {
                return true;
            }
        }
        return false;
    }
    private boolean containsAny(String text, String... patterns) {
        for (String p : patterns) {
            if (text.contains(p)) {
                return true;
            }
        }
        return false;
    }
}
