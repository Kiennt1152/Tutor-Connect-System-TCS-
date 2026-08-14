package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class IntentClassifier {

    public record IntentResult(AiIntent intent, double confidence) {}

    public record ClassificationDetail(
        AiDomain domain,
        AiSubIntent subIntent,
        AiIntent legacyIntent,
        double confidence,
        String suggestedRoute
    ) {}

    public IntentResult classify(String message) {
        ClassificationDetail detail = classifyDetailed(message);
        return new IntentResult(detail.legacyIntent(), detail.confidence());
    }

    public ClassificationDetail classifyDetailed(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new ClassificationDetail(AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.3, null);
        }

        String lower = message.trim().toLowerCase(Locale.ROOT);
        String normalized = VietnameseTextNormalizer.normalize(lower);

        // =========================================================================
        // TIER 1: CONVERSATION & SAFETY FAST-PATH (Deterministic Level 0)
        // =========================================================================
        ClassificationDetail safetyDetail = checkConversationSafety(lower, normalized);
        if (safetyDetail != null) {
            return safetyDetail;
        }

        // =========================================================================
        // TIER 2: BUSINESS DOMAIN & SUB-INTENT MATCHING (Priority Ordered)
        // =========================================================================

        // 1. PLATFORM_STATS (System scale and entity counts)
        if (containsAny(normalized,
                "bao nhieu nguoi dung", "bn nguoi dung", "bao nhieu user", "bn user", "tong user", "so luong user", "tong so user",
                "bao nhieu tai khoan", "bn tai khoan", "co bao nhieu nguoi dung", "bao nhieu hoc vien", "tong so hoc vien",
                "co bao nhieu gia su", "so luong gia su", "co bao nhieu trung tam", "so luong trung tam",
                "co bao nhieu lop", "so luong lop", "thong ke he thong", "tong so nguoi dung", "tong so gia su",
                "tong so lop", "tong so trung tam", "thong ke nguoi dung")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.PLATFORM_STATS, AiIntent.PLATFORM_STATS, 0.95, null);
        }

        // 2. CONTRACT_REVIEW (Contracts, OTP signing, Reviews - check before generic OTP/Auth)
        if (containsAny(normalized,
                "hop dong", "ky hop dong", "ky otp", "tu choi hop dong", "danh gia gia su", "danh gia",
                "review", "uy tin gia su", "reputation", "sign contract", "hop dong dien tu") &&
            !normalized.contains("hop dong trung tam")) {

            if (containsAny(normalized, "ky otp", "ky hop dong", "sign contract", "hop dong dien tu")) {
                return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.CONTRACT_SIGN_OTP, AiIntent.FAQ_SUPPORT, 0.95, "/contracts");
            }
            if (containsAny(normalized, "danh gia", "review", "rating")) {
                return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.REVIEW_CREATE_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/classes");
            }
            if (containsAny(normalized, "uy tin", "reputation")) {
                return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.REPUTATION_VIEW_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/profile");
            }
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.CONTRACT_LIST_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/contracts");
        }

        // 3. VERIFICATION (Check before center operations)
        if (containsAny(normalized,
                "xac minh", "duyet ho so", "giay to", "cccd", "bang cap", "chung chi", "ho so bi tu choi",
                "xac minh trung tam", "xac minh ho so", "duyet gia su", "chua duoc duyet", "ho so chua") &&
            !containsAny(normalized, "quet cccd", "can cuoc cong dan", "vao trung tam", "admin", "queue")) {
            return new ClassificationDetail(AiDomain.VERIFICATION, AiSubIntent.TUTOR_VERIFICATION_HELP, AiIntent.TUTOR_VERIFICATION, 0.9, "/profile");
        }

        // 4. PLATFORM_ADMIN (Dashboard & admin operations - check before generic SLA/tickets)
        if ((containsAny(normalized,
                "bao cao doanh thu", "xem bao cao", "bang dieu khien", "dashboard", "analytics",
                "thong ke doanh thu", "cashflow", "audit log", "nhat ky he thong", "cau hinh he thong",
                "platform fee rate", "reindex ai", "ai knowledge", "reindex", "thong ke kien thuc", "task qua han", "loc task", "queue xac minh",
                "quan ly rut tien admin", "quan ly dispute admin", "quan tri vien", "xuat bao cao csv") ||
            (normalized.contains("doanh thu") && containsAny(normalized, "san", "he thong", "nen tang", "xem", "bao cao", "dashboard", "admin"))) &&
            !normalized.contains("trung tam")) {

            if (containsAny(normalized, "reindex", "knowledge", "thong ke kien thuc")) {
                return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_AI_REINDEX, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
            }
            if (containsAny(normalized, "audit log", "nhat ky")) {
                return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_AUDIT_LOG, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform");
            }
            if (containsAny(normalized, "doanh thu", "cashflow", "money in", "money out", "thong ke doanh thu")) {
                return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_REVENUE_REPORT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
            }
            if (containsAny(normalized, "csv")) {
                return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_CSV_EXPORT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform");
            }
            if (containsAny(normalized, "queue xac minh")) {
                return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_VERIFICATION_QUEUE, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/verifications");
            }
            if (containsAny(normalized, "quan ly rut tien")) {
                return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_WITHDRAWAL_MANAGEMENT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/withdrawals");
            }
            if (containsAny(normalized, "quan ly dispute", "dispute admin")) {
                return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DISPUTE_MANAGEMENT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/disputes");
            }
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DASHBOARD, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform");
        }

        // 5. MESSAGING_TICKET (Explicit ticket creation / status)
        if (containsAny(normalized,
                "tao ticket", "ticket", "yeu cau ho tro", "phan hoi ho tro", "sla", "trang thai ticket",
                "dong ticket", "mo lai ticket", "nhan tin", "tin nhan", "chat voi", "thong bao he thong", "xem thong bao")) {

            if (containsAny(normalized, "sla", "thoi gian phan hoi")) {
                return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_SLA, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
            }
            if (containsAny(normalized, "trang thai ticket", "kiem tra ticket", "kiem tra trang thai ticket")) {
                return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_STATUS, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
            }
            if (containsAny(normalized, "nhan tin", "chat voi", "tin nhan")) {
                return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.MESSAGING_OPEN_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/chat");
            }
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_CREATE, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        // 6. TRUST_SAFETY (Disputes, Circumvention reports, Penalties)
        if (containsAny(normalized,
                "lach san", "bao cao lach san", "thu tien ngoai san", "bao cao vi pham", "to cao gia su", "to cao trung tam", "to cao vi pham",
                "bao cao nguoi dung", "tranh chap", "mo tranh chap", "khi nao nen mo tranh chap", "khieu nai lop hoc", "khieu nai gia su", "khieu nai",
                "che tai", "bi phat", "tai khoan bi phat", "bang chung tranh chap", "tru diem uy tin", "quy dinh phat")) {

            if (containsAny(normalized, "lach san", "circumvention", "ngoai san", "che tai lach san")) {
                return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.REPORT_CIRCUMVENTION, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
            }
            if (containsAny(normalized, "tranh chap", "dispute", "khieu nai", "bang chung")) {
                return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.DISPUTE_OPEN_HELP, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
            }
            if (containsAny(normalized, "bi phat", "che tai", "penalty", "tru diem", "quy dinh phat")) {
                return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.PENALTY_EXPLAIN, AiIntent.TICKET_SUPPORT, 0.95, "/help");
            }
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.REPORT_USER_CREATE, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        // 7. IDENTITY_AUTH - REGISTRATION & PASSWORD
        if (containsAny(normalized,
                "dang ky tai khoan", "dang ky lam gia su", "dang ky trung tam", "huong dan dang ky", "dang ky",
                "quen mat khau", "reset mat khau", "reset password", "quen tai khoan", "doi mat khau",
                "otp", "ma xac thuc", "het phien", "quyen truy cap", "bi tu choi quyen", "tai khoan bi khoa", "dang nhap")) {

            if (containsAny(normalized, "quen mat khau", "reset mat khau", "reset password", "quen tai khoan", "doi mat khau")) {
                return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.PASSWORD_FORGOT_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/forgot-password");
            }
            if (containsAny(normalized, "otp", "ma xac thuc")) {
                return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.OTP_SEND_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/login");
            }
            if (containsAny(normalized, "dang ky", "register")) {
                return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.REGISTER_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/register");
            }
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.LOGIN_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/login");
        }

        // 8. CENTER_OPS (Group classes, center tutors, center contracts, center revenue)
        if (!containsAny(normalized, "xac minh", "to cao", "lach san", "tro giup", "ho tro", "dang ky") &&
            (containsAny(normalized,
                "trung tam gia su", "quan ly trung tam", "thanh vien trung tam", "bai tuyen dung", "hop dong trung tam",
                "tuyen ung vien", "lop nhom", "gia su trung tam", "duyet gia su vao trung tam", "them gia su vao trung tam",
                "xoa gia su khoi trung tam", "danh sach gia su trung tam", "bao cao doanh thu trung tam", "gia su vao trung tam") ||
             normalized.contains("trung tam"))) {
            return new ClassificationDetail(AiDomain.CENTER_OPS, AiSubIntent.CENTER_TUTOR_MANAGEMENT, AiIntent.CENTER_MANAGEMENT, 0.9, "/center");
        }

        // 9. FINANCE_WALLET (Wallet, Escrow, Refund, Payout, Fees)
        if (containsAny(normalized,
                "vi tien", "vi cua toi", "nap tien", "nap vi", "rut tien", "luong gia su", "luong cua toi",
                "thu nhap", "tien kiem duoc", "escrow", "ky quy", "hoan tien", "refund", "phi nen tang",
                "phi san", "chuyen khoan", "qr sepay", "thanh toan", "giao dich", "lich su giao dich")) {

            if (containsAny(normalized, "nap tien", "nap vi", "qr", "sepay")) {
                return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_TOPUP, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
            }
            if (containsAny(normalized, "rut tien", "yeu cau rut tien")) {
                return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WITHDRAWAL_REQUEST, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
            }
            if (containsAny(normalized, "escrow", "ky quy", "giai ngan")) {
                return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.ESCROW_EXPLAIN, AiIntent.PAYMENT_SUPPORT, 0.95, "/help");
            }
            if (containsAny(normalized, "hoan tien", "refund")) {
                return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.REFUND_POLICY, AiIntent.PAYMENT_SUPPORT, 0.95, "/help");
            }
            if (containsAny(normalized, "phi nen tang", "phi san")) {
                return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.PLATFORM_FEE_EXPLAIN, AiIntent.PAYMENT_SUPPORT, 0.95, "/help");
            }
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_VIEW, AiIntent.PAYMENT_SUPPORT, 0.9, "/finance");
        }

        // 10. PROFILE_GUARDIAN (Profiles, Dependent/Child profiles, Guardian linking)
        if (containsAny(normalized,
                "ho so ca nhan", "cap nhat ho so", "tai avatar", "avatar", "quet cccd", "can cuoc cong dan", "ho so con", "tao ho so con",
                "tai khoan phu huynh", "nguoi giam ho", "lien ket phu huynh", "lich ranh", "kinh nghiem gia su", "kinh nghiem day hoc",
                "bio gia su", "thong tin lien he")) {

            if (containsAny(normalized, "ho so con", "child profile", "tao ho so con")) {
                return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.CHILD_PROFILE_CREATE, AiIntent.FAQ_SUPPORT, 0.95, "/profile");
            }
            if (containsAny(normalized, "giam ho", "phu huynh", "guardian", "lien ket phu huynh")) {
                return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.GUARDIAN_LINK_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/profile");
            }
            return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.PROFILE_UPDATE_HELP, AiIntent.TUTOR_OPTIMIZATION, 0.9, "/profile");
        }

        // 11. TUTOR_OPS (Tutor teaching schedule, attendance, substitution)
        if (containsAny(normalized,
                "lich day", "diem danh", "doi lich", "doi gio hoc", "xin nghi", "day thay", "nhan lop day", "quy trinh day", "nhan lop")) {

            if (containsAny(normalized, "diem danh", "attendance")) {
                return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_ATTENDANCE_MARK, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/classes");
            }
            if (containsAny(normalized, "doi lich", "doi gio hoc", "reschedule", "xin nghi")) {
                return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_RESCHEDULE_REQUEST, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/schedule");
            }
            if (containsAny(normalized, "day thay", "substitute", "nguoi day thay")) {
                return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SUBSTITUTE_REQUEST, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/classes");
            }
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SCHEDULE_VIEW, AiIntent.FAQ_SUPPORT, 0.9, "/tutor/schedule");
        }

        // 12. CATALOG_FAQ - SPECIFIC QUESTIONS
        if (containsAny(normalized,
                "trung tam tro giup", "tro giup o dau", "mon hoc nao", "khoi lop nao", "co nhung khoi lop", "khu vuc nao", "quy trinh ket noi",
                "gioi thieu ve tcs", "tcs la gi", "he thong tcs hoat dong", "cac vai tro", "chinh sach nen tang")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.9, "/help");
        }

        // 13. MARKETPLACE - CREATE CLASS
        if (containsAny(normalized,
                "tao lop", "dang bai tim gia su", "tao yeu cau hoc", "mo yeu cau hoc", "tim nguoi day", "dang tin tim gia su", "dang lop")) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.CREATE_CLASS, AiIntent.CREATE_CLASS, 0.95, "/tao-lop");
        }

        // 14. MARKETPLACE - FIND CLASS
        boolean hasTutorKeyword = containsAny(normalized,
                "gia su", "tutor", "thay giao", "co giao", "thay co", "giao vien", "nguoi day", "day kem", "gs", "tim thay", "tim co", "thue");

        if (containsAny(normalized,
                "tim lop", "lop hoc dang mo", "lop dang mo", "khoa hoc", "dang ky lop", "danh sach lop", "ung tuyen lop", "chon gia su ung tuyen", "day kem hoa",
                "tim lop toan", "tim lop ly", "tim lop hoa", "tim lop anh", "tim lop van", "tim lop su", "tim lop dia", "tim lop sinh", "tim lop tin", "tim lop tieng",
                "co lop nao", "co lop toan nao", "co lop toan ko", "co lop day", "co lop", "tim lop hoc", "lop hoc tieng", "lop day tieng", "lop day toan",
                "find class", "find classes", "find math classes", "open classes", "math classes open", "classes near me", "search classes", "can tim lop") ||
            (normalized.contains("lop") && !hasTutorKeyword && containsAny(normalized, "tim", "co ", "day", "mo tuyen", "dang mo", "nguoi di lam", "find", "classes", "hoc"))) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.FIND_CLASS, AiIntent.FIND_CLASS, 0.9, "/lop-hoc");
        }

        // 15. AI_TUTORING (Math, Science, English learning, grammar, problem solving)
        if (containsAny(normalized,
                "giai bai", "huong dan lam bai", "phuong trinh", "bai tap", "ngu phap", "ielts",
                "luyen tap", "ke hoach hoc", "bang may", "dinh ly", "cong thuc", "van toc", "thi hien tai", "thi qua khu", "giai thich thi",
                "solve math", "solve equation", "grammar check", "explain grammar", "math problem", "solve") ||
            (lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/")) &&
             Pattern.compile("[0-9]+\\s*[+\\-*/]\\s*[0-9]+").matcher(lower).find()) {
            return new ClassificationDetail(AiDomain.AI_TUTORING, AiSubIntent.AI_TUTORING_MATH, AiIntent.AI_TUTORING, 0.9, null);
        }

        // 16. MARKETPLACE - FIND TUTOR (Resilient multi-word & accent-free matching)
        boolean hasSearchKeyword = containsAny(normalized,
                "tim", "thue", "can", "kiem", "cho toi", "gioi thieu", "mon", "toan", "ly", "hoa", "anh", "van", "tin", "sinh", "su", "dia",
                "lop", "khu vuc", "cau giay", "dong da", "ba dinh", "ha noi", "hcm", "sai gon", "da nang",
                "hoc phi", "duoi", "khoang", "tam", "k/buoi", "vnd", "tien", "luyen thi", "find", "looking", "near");

        if (!normalized.contains("ngu phap") && ((hasTutorKeyword && hasSearchKeyword) || containsAny(normalized,
                "tim gia su", "thue gia su", "can gia su", "can thue gia su", "gia su day", "giao vien day", "tim thay", "tim co", "ai re hon",
                "tim gs", "tim gs toan", "tim gs ly", "tim gs hoa", "tim gs anh", "tim thay day toan", "tim co day toan", "co gia su toan ko", "co gia su nao",
                "gia su toan", "gia su ly", "gia su hoa", "gia su anh", "gia su van", "gia su tin", "gia su luyen thi", "gia su tieng",
                "find tutor", "math tutor", "tutor near me", "looking for tutor", "need a tutor", "hire tutor", "math tutor near"))) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR, AiIntent.FIND_TUTOR, 0.95, "/tim-gia-su");
        }

        // 17. CATALOG_FAQ - GENERAL FALLBACK
        if (containsAny(normalized,
                "huong dan", "cach dung", "quy trinh", "chinh sach", "faq", "ho tro chung", "the nao",
                "la gi", "vai tro", "tinh nang", "cac mon hoc")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.9, "/help");
        }

        return new ClassificationDetail(AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.3, null);
    }

    private ClassificationDetail checkConversationSafety(String lower, String normalized) {
        // GREETING
        if (normalized.equals("xin chao") || normalized.equals("chao bot") || normalized.equals("hello") ||
            normalized.equals("hi bot") || normalized.equals("hey") || normalized.equals("alo") ||
            normalized.equals("chao em") || normalized.equals("chao anh") || normalized.equals("hi tcs") ||
            normalized.equals("chao ban") || normalized.equals("chao") || normalized.equals("hi") ||
            normalized.startsWith("xin chao ") || normalized.startsWith("chao bot ") || normalized.startsWith("hello ")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GREETING, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // GOODBYE
        if (containsAny(normalized, "tam biet", "bye", "bye bot", "hen gap lai", "bai bai", "goodbye")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GOODBYE, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // THANKS
        if (containsAny(normalized, "cam on", "thank you", "thanks", "tks", "cam on bot", "cam on nha")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.THANKS, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // BOT_CAPABILITY_ASK
        if (containsAny(normalized, "ban lam duoc gi", "bot lam duoc gi", "chuc nang cua bot", "huong dan su dung bot", "ban co the lam gi")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.BOT_CAPABILITY_ASK, AiIntent.FAQ_SUPPORT, 0.95, null);
        }

        // SMALL_TALK
        if (containsAny(normalized, "ban la ai", "may la ai", "who are you", "ban ten gi", "bot la ai")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.SMALL_TALK, AiIntent.OUT_OF_SCOPE, 0.95, null);
        }

        // PROFANITY_OR_FRUSTRATION (Use whole-word token matching for short curse words)
        if (hasWord(normalized, "dm") || hasWord(normalized, "du") || hasWord(normalized, "vcl") ||
            hasWord(normalized, "dit") || hasWord(normalized, "clmm") || hasWord(normalized, "dmm") ||
            hasWord(normalized, "dcm") || containsAny(normalized, "bot ngu", "bot nhu cac", "khon nan", "me kiep", "dm bot", "lua dao")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.PROFANITY_OR_FRUSTRATION, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // HUMAN_SUPPORT_REQUEST
        if (containsAny(normalized, "gap nguoi ho tro", "gap nhan vien", "gap admin", "cham soc khach hang", "gap cskh", "gap tong dai", "cho toi gap nguoi ho tro")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.HUMAN_SUPPORT_REQUEST, AiIntent.TICKET_SUPPORT, 1.0, "/support/tickets");
        }

        // GIBBERISH (Random strings like "asdfghjk", "twyalk", "zzzzz")
        if (isGibberish(normalized)) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GIBBERISH, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        return null;
    }

    private boolean isGibberish(String text) {
        if (text == null || text.isBlank()) return false;
        String clean = text.replaceAll("\\s+", "");
        if (clean.length() >= 6) {
            // Check for lack of vowels (a, e, i, o, u, y) in a long string
            boolean hasVowel = clean.matches(".*[aeiouy].*");
            if (!hasVowel) return true;

            // Check repeated characters 4+ times (e.g. "aaaaa", "zzzzz")
            if (Pattern.compile("(.)\\1{4,}").matcher(clean).find()) return true;

            // Common gibberish patterns
            if (clean.contains("asdf") || clean.contains("qwer") || clean.contains("zxcv") || clean.contains("twyalk")) return true;
        }
        return false;
    }

    private boolean hasWord(String normalized, String word) {
        String[] tokens = normalized.split("[^a-z0-9]+");
        for (String t : tokens) {
            if (t.equals(word)) return true;
        }
        return false;
    }

    private boolean containsPhrase(String normalized, String phrase) {
        String pNorm = VietnameseTextNormalizer.normalize(phrase);
        if (!pNorm.contains(" ")) {
            return hasWord(normalized, pNorm);
        }
        return normalized.contains(pNorm);
    }

    private boolean containsAny(String normalized, String... targets) {
        for (String target : targets) {
            if (containsPhrase(normalized, target)) {
                return true;
            }
        }
        return false;
    }
}
