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

        return new SafetyCheckResult(true, null, null, false);
    }
}
