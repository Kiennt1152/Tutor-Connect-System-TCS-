package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class IntentClassifier {

    private static final Set<String> VALID_ABBREVIATIONS = Set.of(
        "thptqg", "dhqghn", "khtn", "khxh", "gdcd", "thcs", "thpt",
        "hsg", "bkhn", "ftu", "hnue", "vnu", "neu", "hmu",
        "hcmus", "ussh", "uet", "bktp", "uit", "tdtu"
    );

    private final OpenDomainClassifier openDomainClassifier;

    public IntentClassifier() {
        this.openDomainClassifier = new OpenDomainClassifier();
    }

    public IntentClassifier(OpenDomainClassifier openDomainClassifier) {
        this.openDomainClassifier = openDomainClassifier != null ? openDomainClassifier : new OpenDomainClassifier();
    }

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
        normalized = expandTeencode(normalized);

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

        // 1. PLATFORM_ADMIN (Stats, Dashboard, Analytics, Audit Log, Queue, Revenue)
        if (containsAny(normalized,
                "bao nhieu nguoi dung", "bn nguoi dung", "bao nhieu user", "bn user", "tong user", "so luong user", "tong so user",
                "bao nhieu tai khoan", "bn tai khoan", "co bao nhieu nguoi dung", "bao nhieu hoc vien", "tong so hoc vien",
                "co bao nhieu gia su", "so luong gia su", "co bao nhieu trung tam", "so luong trung tam",
                "co bao nhieu lop", "so luong lop", "thong ke he thong", "tong so nguoi dung", "tong so gia su",
                "tong so lop", "tong so trung tam", "thong ke nguoi dung")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.PLATFORM_STATS, AiIntent.PLATFORM_STATS, 0.95, null);
        }

        if (!normalized.contains("trung tam") && (containsAny(normalized,
                "bao cao doanh thu", "thong ke doanh thu", "doanh thu phi san", "cashflow", "money in money out", "doanh thu nen tang") ||
            (normalized.contains("doanh thu") && containsAny(normalized, "san", "nen tang", "he thong", "phi san", "bao cao", "thong ke", "dashboard")))) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_REVENUE_REPORT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
        }

        if (containsAny(normalized, "audit log", "nhat ky he thong", "nhat ky he thong audit log")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_AUDIT_LOG, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/audit-logs");
        }

        if (containsAny(normalized, "reindex ai", "reindex knowledge", "thong ke kien thuc ai", "ai knowledge base")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_AI_REINDEX, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
        }

        if (containsAny(normalized, "queue xac minh", "hang doi xac minh", "xac minh ho so admin", "queue xac minh ho so admin")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_VERIFICATION_QUEUE, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/verifications");
        }

        if (containsAny(normalized, "quan ly rut tien admin", "duyet rut tien admin")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_WITHDRAWAL_MANAGEMENT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/finance");
        }

        if (containsAny(normalized, "quan ly dispute admin", "xu ly tranh chap admin", "tranh chap admin")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DISPUTE_MANAGEMENT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/disputes");
        }

        if (containsAny(normalized, "xuat bao cao csv", "xuat bao cao csv admin", "csv export")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_CSV_EXPORT, AiIntent.ADMIN_DASHBOARD, 0.95, "/platform/analytics");
        }

        if (!normalized.contains("trung tam") && containsAny(normalized,
                "admin dashboard", "dashboard quan tri", "bang dieu khien quan tri", "analytics van hanh",
                "loc task qua han sla", "cau hinh he thong platform fee", "quan tri vien he thong", "dashboard")) {
            return new ClassificationDetail(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DASHBOARD, AiIntent.ADMIN_DASHBOARD, 0.9, "/platform/analytics");
        }

        // 2. MESSAGING_TICKET (Support tickets, SLA, Chat - check before TRUST_SAFETY so "ticket khiếu nại" goes here)
        if (containsAny(normalized, "nhan tin voi gia su", "chat voi phu huynh", "nhan tin voi phu huynh", "chat voi")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.MESSAGING_OPEN_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/messages");
        }

        if (containsAny(normalized, "sla", "thoi gian phan hoi sla", "quy dinh sla phan hoi")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_SLA, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        if (containsAny(normalized, "kiem tra trang thai ticket", "trang thai ticket")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_STATUS, AiIntent.TICKET_SUPPORT, 0.9, "/support/tickets");
        }

        if (containsAny(normalized, "tao ticket", "gui ticket", "dong ticket", "mo lai ticket", "xem thong bao", "yeu cau ho tro", "ticket")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_CREATE, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        // 3. TRUST_SAFETY (Disputes, Reports, Circumvention, Sanctions)
        if (containsAny(normalized, "lach san", "thu tien ngoai san", "chuyen khoan ngoai san", "bao cao gia su lach san", "che tai lach san", "bao cao trung tam lach san")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.REPORT_CIRCUMVENTION, AiIntent.TICKET_SUPPORT, 0.95, "/help");
        }

        if (containsAny(normalized, "tranh chap", "mo tranh chap", "khi nao nen mo tranh chap", "tai bang chung tranh chap", "khieu nai gia su", "khieu nai lop hoc bi huy", "giai quyet tranh chap")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.DISPUTE_OPEN_HELP, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        if (containsAny(normalized, "bi phat", "phat canh cao", "che tai khi vi pham", "tru diem uy tin", "tai khoan bi phat", "quy dinh phat vi pham")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.PENALTY_EXPLAIN, AiIntent.TICKET_SUPPORT, 0.95, "/help");
        }

        if (containsAny(normalized, "to cao vi pham", "bao cao nguoi dung vi pham", "to cao", "bao cao vi pham", "bao cao nguoi dung", "lua dao", "bi lua")) {
            return new ClassificationDetail(AiDomain.TRUST_SAFETY, AiSubIntent.REPORT_USER_CREATE, AiIntent.TICKET_SUPPORT, 0.95, "/support/tickets");
        }

        // 4. VERIFICATION (CCCD verification, Diploma, Tutor/Center Verification)
        if (containsAny(normalized,
                "xac minh", "duyet ho so", "ho so bi tu choi", "trang thai xac minh", "bang cap chung chi", "xac minh cccd",
                "giay to cccd", "cccd khong hop le", "giay to gi de duyet", "chua duoc duyet", "ho so chua", "gui lai giay to xac minh",
                "trang thai duyet ho so", "tai sao ho so chua duoc duyet", "xac minh bang cap", "quy trinh xac minh") &&
            !containsAny(normalized, "tim gia su", "can gia su", "thue gia su", "co gia su", "ai day")) {
            return new ClassificationDetail(AiDomain.VERIFICATION, AiSubIntent.TUTOR_VERIFICATION_HELP, AiIntent.TUTOR_VERIFICATION, 0.95, "/profile");
        }

        // 5. PROFILE_GUARDIAN (Child profile, Guardian link, Profile update)
        if (containsAny(normalized, "ho so con", "child profile", "them ho so con", "tao ho so con")) {
            return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.CHILD_PROFILE_CREATE, AiIntent.FAQ_SUPPORT, 0.95, "/parent/students");
        }

        if (containsAny(normalized, "lien ket phu huynh", "nguoi giam ho", "lien ket tai khoan phu huynh", "xac nhan nguoi giam ho")) {
            return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.GUARDIAN_LINK_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/parent/students");
        }

        if (containsAny(normalized,
                "ho so ca nhan", "cap nhat ho so", "tai anh dai dien", "avatar", "quet can cuoc cong dan", "cccd",
                "kinh nghiem day hoc", "lich ranh", "viet bio", "chinh sua thong tin lien he", "cap nhat ho so gia su", "them lich ranh") &&
            !containsAny(normalized, "xac minh", "duyet", "tim gia su", "thue gia su", "can gia su")) {
            return new ClassificationDetail(AiDomain.PROFILE_GUARDIAN, AiSubIntent.PROFILE_UPDATE_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/profile");
        }

        // 6. CONTRACT_REVIEW (Contracts, OTP signing, Reviews, Reputation)
        if (containsAny(normalized, "ky hop dong", "ky otp", "sign contract", "hop dong dien tu", "ky hop dong bang ma otp", "ky hop dong bang otp")) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.CONTRACT_SIGN_OTP, AiIntent.FAQ_SUPPORT, 0.95, "/contracts");
        }

        if (containsAny(normalized, "danh gia gia su", "viet review", "danh gia buoi day", "vi sao review bi an", "rating va review") ||
            (containsAny(normalized, "danh gia", "review") && !normalized.contains("trung tam"))) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.REVIEW_CREATE_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/classes");
        }

        if (containsAny(normalized, "uy tin gia su", "do uy tin gia su", "reputation") ||
            (normalized.contains("uy tin") && containsAny(normalized, "gia su", "giao vien", "thay", "co", "diem uy tin"))) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.REPUTATION_VIEW_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/profile");
        }

        if (containsAny(normalized, "danh sach hop dong", "xem hop dong", "tu choi hop dong", "hop dong lop hoc") ||
            (normalized.contains("hop dong") && !normalized.contains("trung tam"))) {
            return new ClassificationDetail(AiDomain.CONTRACT_REVIEW, AiSubIntent.CONTRACT_LIST_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/contracts");
        }

        // 7. FINANCE_WALLET (Topup, Withdraw, Escrow, Platform fees, Tutor Salary)
        if (containsAny(normalized, "nap tien", "topup", "sepay", "nap qua qr", "nap vi")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_TOPUP, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "rut tien", "withdraw", "rut ve ngan hang", "rut tien luong")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WITHDRAWAL_REQUEST, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "escrow", "ky quy", "tam giu", "giai ngan", "tien escrow")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.ESCROW_EXPLAIN, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "phi san", "phi nen tang", "10%", "chiet khau")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.PLATFORM_FEE_EXPLAIN, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "hoan tien", "refund", "tra lai tien")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.REFUND_POLICY, AiIntent.PAYMENT_SUPPORT, 0.95, "/finance");
        }

        if (containsAny(normalized, "vi tien cua toi", "so du vi", "luong cua toi", "thu nhap gia su", "xem lich su giao dich", "xem luong gia su", "thu nhap", "luong")) {
            return new ClassificationDetail(AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_VIEW, AiIntent.PAYMENT_SUPPORT, 0.9, "/finance");
        }

        // 8. IDENTITY_AUTH (Password reset, Registration, OTP, Login)
        if (containsAny(normalized, "quen mat khau", "doi mat khau", "reset password", "quen tai khoan", "toi quen mat khau")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.PASSWORD_FORGOT_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/login");
        }

        if (containsAny(normalized, "dang ky tai khoan", "dang ky lam gia su", "dang ky trung tam", "huong dan dang ky", "dang ky") &&
            !containsAny(normalized, "dang ky lop", "dang ky hoc")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.REGISTER_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/register");
        }

        if (containsAny(normalized, "ma otp", "otp xac thuc", "nhap ma otp", "khong nhan duoc ma otp", "ma xac thuc otp", "ma xac thuc")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.OTP_SEND_HELP, AiIntent.FAQ_SUPPORT, 0.95, "/login");
        }

        if (containsAny(normalized, "dang nhap", "tai khoan bi khoa", "dang nhap bang google", "het han phien", "khong co quyen truy cap", "login")) {
            return new ClassificationDetail(AiDomain.IDENTITY_AUTH, AiSubIntent.LOGIN_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/login");
        }

        // 9. CENTER_OPS (Tutor Center management)
        if (containsAny(normalized, "trung tam quan ly gia su", "thanh vien trung tam", "tuyen dung gia su cho trung tam", "duyet gia su vao trung tam",
                "hop dong trung tam", "bao cao doanh thu trung tam", "lop nhom cho trung tam", "quan ly trung tam gia su", "tuyen ung vien gia su",
                "tao bai tuyen dung", "danh sach gia su trung tam", "xoa gia su khoi trung tam", "them gia su vao trung tam") ||
            (normalized.contains("trung tam") && containsAny(normalized, "quan ly", "gia su", "tuyen", "doanh thu", "thanh vien", "hop dong"))) {
            return new ClassificationDetail(AiDomain.CENTER_OPS, AiSubIntent.CENTER_TUTOR_MANAGEMENT, AiIntent.CENTER_MANAGEMENT, 0.95, "/center");
        }

        // 10. TUTOR_OPS (Tutor attendance, schedule, reschedule, substitute)
        if (containsAny(normalized, "diem danh", "diem danh hoc vien", "cach diem danh lop hoc", "huong dan diem danh")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_ATTENDANCE_MARK, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/schedule");
        }

        if (containsAny(normalized, "xin doi lich", "doi lich day", "xin nghi day", "doi gio hoc")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_RESCHEDULE_REQUEST, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/schedule");
        }

        if (containsAny(normalized, "day thay", "day thay the", "tim nguoi day thay")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SUBSTITUTE_REQUEST, AiIntent.FAQ_SUPPORT, 0.95, "/tutor/classes");
        }

        if (containsAny(normalized, "xem lich day", "quy trinh nhan lop day kem", "lich day tuan nay", "lich day gia su", "lich day", "nhan lop day", "nhan lop")) {
            return new ClassificationDetail(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SCHEDULE_VIEW, AiIntent.FAQ_SUPPORT, 0.9, "/tutor/schedule");
        }

        // 11. CATALOG_FAQ - SPECIFIC QUESTIONS (Before Marketplace to prevent "khối lớp nào" or "quy trình kết nối" matching class search)
        if (containsAny(normalized,
                "trung tam tro giup", "tro giup o dau", "mon hoc nao", "khoi lop nao", "co nhung khoi lop", "khu vuc nao", "quy trinh ket noi",
                "gioi thieu ve tcs", "tcs la gi", "he thong tcs hoat dong", "cac vai tro", "chinh sach nen tang", "cac mon hoc tren tcs", "huong dan su dung tcs", "chinh sach bao mat")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.9, "/help");
        }

        // 12. MARKETPLACE - CREATE CLASS
        if (containsAny(normalized,
                "dang bai tim gia su", "tao yeu cau hoc moi", "tao lop tim nguoi day", "dang tin tim gia su", "tao lop tim gia su", "tao lop", "dang bai", "tao yeu cau hoc")) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.CREATE_CLASS, AiIntent.CREATE_CLASS, 0.95, "/tao-lop");
        }

        // 13. MARKETPLACE - FIND CLASS
        boolean hasTutorKeyword = containsAny(normalized,
                "gia su", "tutor", "thay giao", "co giao", "thay co", "giao vien", "nguoi day", "day kem", "gs", "tim thay", "tim co");

        if (containsAny(normalized,
                "tim lop", "lop hoc dang mo", "lop dang mo", "khoa hoc", "dang ky lop", "danh sach lop", "ung tuyen lop", "chon gia su ung tuyen", "day kem hoa",
                "tim lop toan", "tim lop ly", "tim lop hoa", "tim lop anh", "tim lop van", "tim lop su", "tim lop dia", "tim lop sinh", "tim lop tin", "tim lop tieng",
                "co lop nao", "co lop toan nao", "co lop toan ko", "co lop toan khong", "co lop day tiieng viet khong", "co lop day tieng", "co lop day", "co lop", "tim lop hoc", "lop hoc tieng", "lop day tieng", "lop day toan",
                "find class", "find classes", "find math classes", "open classes", "math classes open", "classes near me", "search classes", "can tim lop") ||
            (normalized.contains("lop") && !hasTutorKeyword && containsAny(normalized, "tim", "co ", "day", "mo tuyen", "dang mo", "nguoi di lam", "find", "classes", "hoc", "khong", "ko"))) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.FIND_CLASS, AiIntent.FIND_CLASS, 0.9, "/lop-hoc");
        }

        // 14. SIMPLE ARITHMETIC → OPEN_DOMAIN (before AI_TUTORING)
        if (Pattern.compile("[0-9]+\\s*[+\\-*/]\\s*[0-9]+").matcher(lower).find() &&
            !containsAny(normalized, "giai bai", "bai tap", "huong dan", "phuong trinh", "dinh ly", "cong thuc")) {
            return new ClassificationDetail(AiDomain.OPEN_DOMAIN, AiSubIntent.MATH_CALCULATION, AiIntent.OUT_OF_SCOPE, 0.95, null);
        }

        // 15. AI_TUTORING (Math, Science, English learning, study plan, code)
        if (!containsAny(normalized, "tim gia su", "can gia su", "thue gia su", "tim thay", "tim co", "gia su day", "giao vien day", "tim nguoi day", "day kem", "can tim gia su", "co ai day", "ai day") &&
            (containsAny(normalized,
                "giai bai", "huong dan lam bai", "phuong trinh", "bai tap", "ngu phap", "giai phuong trinh",
                "luyen tap", "ke hoach hoc", "bang may", "dinh ly", "cong thuc", "van toc", "thi hien tai", "thi qua khu", "giai thich thi",
                "lap trinh", "coding", "python", "java", "c++", "javascript", "debug", "code", "viet code",
                "vat ly", "hoa hoc", "sinh hoc", "physics", "chemistry", "biology",
                "solve math", "solve equation", "grammar check", "explain grammar", "math problem", "solve") ||
            ((lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/")) &&
             Pattern.compile("[0-9]+\\s*[+\\-*/]\\s*[0-9]+").matcher(lower).find()))) {
            if (containsAny(normalized, "ke hoach hoc", "lo trinh", "study plan", "ke hoach hoc tap", "on thi dai hoc", "luyen tap")) {
                return new ClassificationDetail(AiDomain.AI_TUTORING, AiSubIntent.AI_TUTORING_STUDY_PLAN, AiIntent.AI_TUTORING, 0.9, null);
            }
            if (containsAny(normalized, "ngu phap", "grammar", "vocabulary", "thi hien tai", "thi qua khu", "giai thich thi", "grammar check", "explain grammar")) {
                return new ClassificationDetail(AiDomain.AI_TUTORING, AiSubIntent.AI_TUTORING_ENGLISH, AiIntent.AI_TUTORING, 0.9, null);
            }
            if (containsAny(normalized, "vat ly", "hoa hoc", "sinh hoc", "physics", "chemistry", "biology", "dinh ly", "cong thuc", "van toc", "pitago")) {
                return new ClassificationDetail(AiDomain.AI_TUTORING, AiSubIntent.AI_TUTORING_SCIENCE, AiIntent.AI_TUTORING, 0.9, null);
            }
            if (containsAny(normalized, "lap trinh", "coding", "python", "java", "c++", "javascript", "debug", "code", "viet code")) {
                return new ClassificationDetail(AiDomain.AI_TUTORING, AiSubIntent.AI_TUTORING_CODE, AiIntent.AI_TUTORING, 0.9, null);
            }
            return new ClassificationDetail(AiDomain.AI_TUTORING, AiSubIntent.AI_TUTORING_MATH, AiIntent.AI_TUTORING, 0.9, null);
        }

        // 16. MARKETPLACE - FIND TUTOR
        boolean hasExclusion = containsAny(normalized,
                "luong", "tra luong", "nhan luong", "tra trong bao lau", "bao lau", "bao gio", "giai ngan", "quy dinh", "bao nhieu phan tram",
                "diem uy tin", "xac minh", "duyet ho so", "cccd", "bang cap", "hoan tien", "tranh chap", "to cao",
                "khieu nai", "hop dong", "ky hop dong", "rut tien", "phi san", "phi nen tang");

        boolean hasSearchKeyword = containsAny(normalized,
                "tim", "thue", "can", "kiem", "cho toi", "gioi thieu", "mon", "toan", "ly", "hoa", "anh", "van", "tin", "sinh", "su", "dia",
                "ielts", "toeic", "tieng anh", "ngoai ngu", "he thong", "giao tiep",
                "khu vuc", "cau giay", "dong da", "ba dinh", "ha noi", "hcm", "sai gon", "da nang",
                "hoc phi", "duoi", "khoang", "k/buoi", "vnd", "luyen thi", "find", "looking", "near", "day kem", "tai nha", "1 kem 1", "online");

        if (!hasExclusion && !normalized.contains("ngu phap") && ((hasTutorKeyword && hasSearchKeyword) || containsAny(normalized,
                "tim gia su", "thue gia su", "can gia su", "can thue gia su", "gia su day", "giao vien day", "tim thay", "tim co", "ai re hon",
                "tim gs", "tim gs toan", "tim gs ly", "tim gs hoa", "tim gs anh", "tim thay day toan", "tim co day toan", "co gia su toan ko", "co gia su nao",
                "gia su toan", "gia su ly", "gia su hoa", "gia su anh", "gia su van", "gia su tin", "gia su luyen thi", "gia su tieng", "gia su ielts",
                "co ai day", "ai day ielts", "co ai day ielts", "day ielts", "co ai day toan", "co ai day van", "ai day",
                "find tutor", "math tutor", "tutor near me", "looking for tutor", "need a tutor", "hire tutor", "math tutor near"))) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR, AiIntent.FIND_TUTOR, 0.95, "/tim-gia-su");
        }

        // 17. OPEN_DOMAIN (Math, Weather, Time/Date, General Knowledge, Entertainment)
        OpenDomainClassifier.OpenDomainResult openResult = openDomainClassifier.classifyOpen(message);
        if (openResult.confidence() >= 0.7) {
            return new ClassificationDetail(
                AiDomain.OPEN_DOMAIN,
                openResult.subIntent(),
                AiIntent.OUT_OF_SCOPE,
                openResult.confidence(),
                null
            );
        }

        // 18. CATALOG_FAQ - GENERAL FALLBACK
        if (containsAny(normalized,
                "huong dan", "cach dung", "quy trinh", "chinh sach", "faq", "ho tro chung",
                "tcs la gi", "vai tro", "tinh nang san", "cac mon hoc")) {
            return new ClassificationDetail(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH, AiIntent.FAQ_SUPPORT, 0.9, "/help");
        }

        return new ClassificationDetail(AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.3, null);
    }

    private String expandTeencode(String text) {
        String result = text;
        result = result.replaceAll("\\bielst\\b", "ielts");
        result = result.replaceAll("\\btopic\\b", "topik");
        result = result.replaceAll("\\bgv\\b", "giao vien");
        result = result.replaceAll("\\bhs\\b", "hoc sinh");
        result = result.replaceAll("\\bph\\b", "phu huynh");
        result = result.replaceAll("\\bacc\\b", "tai khoan");
        result = result.replaceAll("\\bmk\\b", "mat khau");
        result = result.replaceAll("\\bpass\\b", "mat khau");
        result = result.replaceAll("\\bib\\b", "nhan tin");
        result = result.replaceAll("\\binbox\\b", "nhan tin");
        result = result.replaceAll("(?<![0-9])\\bko\\b", "khong");
        result = result.replaceAll("(?<![0-9])\\bdc\\b", "duoc");
        result = result.replaceAll("\\bgiasu\\b", "gia su");
        result = result.replaceAll("\\bhocphi\\b", "hoc phi");
        result = result.replaceAll("\\bdaykem\\b", "day kem");
        result = result.replaceAll("\\btimlop\\b", "tim lop");
        return result;
    }

    private boolean hasBusinessIntent(String text) {
        return containsAny(text,
            "gia su", "gs", "lop", "tim", "can", "thue", "nap tien", "rut tien",
            "escrow", "hop dong", "dang ky", "hoc", "day", "toan", "ly", "hoa",
            "anh", "van", "ticket", "ho tro", "khieu nai", "tranh chap", "bao cao");
    }

    private ClassificationDetail checkConversationSafety(String lower, String normalized) {
        // GREETING
        if (normalized.equals("xin chao") || normalized.equals("chao bot") || normalized.equals("hello") ||
            normalized.equals("hi bot") || normalized.equals("hey") || normalized.equals("alo") ||
            normalized.equals("chao em") || normalized.equals("chao anh") || normalized.equals("hi tcs") ||
            normalized.equals("chao ban") || normalized.equals("chao") || normalized.equals("hi") ||
            normalized.equals("hello tcs")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GREETING, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        if (normalized.startsWith("xin chao ") || normalized.startsWith("chao bot ") || normalized.startsWith("hello ")) {
            String afterGreeting = normalized;
            if (normalized.startsWith("xin chao ")) afterGreeting = normalized.substring(9).trim();
            else if (normalized.startsWith("chao bot ")) afterGreeting = normalized.substring(9).trim();
            else if (normalized.startsWith("hello ")) afterGreeting = normalized.substring(6).trim();
            afterGreeting = afterGreeting.replaceFirst("^[,!.\\s]+", "").trim();
            if (afterGreeting.isBlank() || !hasBusinessIntent(afterGreeting)) {
                return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GREETING, AiIntent.OUT_OF_SCOPE, 1.0, null);
            }
            return null;
        }

        // GOODBYE
        if (containsAny(normalized, "tam biet", "bye", "bye bot", "hen gap lai", "bai bai", "goodbye")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GOODBYE, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // THANKS
        if (containsAny(normalized, "cam on", "thank you", "thanks", "tks", "cam on bot", "cam on nha", "cam on ban nhe")) {
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

        // PROFANITY_OR_FRUSTRATION (Use whole-word token matching for short curse words, 'lua dao' removed)
        if (hasWord(normalized, "dm") || hasWord(normalized, "du") || hasWord(normalized, "vcl") ||
            hasWord(normalized, "dit") || hasWord(normalized, "clmm") || hasWord(normalized, "dmm") ||
            hasWord(normalized, "dcm") || containsAny(normalized, "bot ngu", "bot nhu cac", "khon nan", "me kiep", "dm bot", "vcl")) {
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
        if (VALID_ABBREVIATIONS.contains(clean.toLowerCase(Locale.ROOT))) return false;

        if (clean.length() >= 6) {
            boolean hasVowel = clean.matches(".*[aeiouy].*");
            if (!hasVowel) return true;

            if (Pattern.compile("(.)\\1{4,}").matcher(clean).find()) return true;

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
