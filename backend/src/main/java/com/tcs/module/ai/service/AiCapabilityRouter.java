package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AiCapabilityRouter {

    public enum CardPolicy {
        NONE,
        TUTOR_CARDS,
        CLASS_CARDS,
        FAQ_CARDS,
        ADMIN_LINK_ONLY,
        TICKET_LINK_ONLY,
        FINANCE_LINK_ONLY,
        MIXED_ALLOWED
    }

    public enum GuardType {
        TUTOR_NAME_SCRUB,
        STATS_NUMBER_GUARD,
        FINANCE_LOGIN_GUARD,
        NONE
    }

    public record CapabilityPolicy(
        Set<String> allowedSourceTypes,
        boolean requireAuth,
        Set<String> allowedRoles,
        boolean allowLlmCall,
        boolean requireDbSource,
        CardPolicy cardPolicy,
        GuardType guardType,
        String deepLinkRoute,
        String fallbackMessage
    ) {}

    private static final CapabilityPolicy DEFAULT_OUT_OF_SCOPE_POLICY = new CapabilityPolicy(
        Set.of(), false, Set.of(), true, false,
        CardPolicy.NONE, GuardType.NONE, null,
        "Xin lỗi, câu hỏi này nằm ngoài phạm vi hỗ trợ của hệ thống TCS. Tôi có thể giúp bạn tìm gia sư, tìm lớp hoặc giải đáp chính sách hệ thống."
    );

    private static final Map<AiDomain, CapabilityPolicy> DOMAIN_POLICIES = Map.ofEntries(
        Map.entry(AiDomain.CONVERSATION_SAFETY, new CapabilityPolicy(
            Set.of(), false, Set.of(), false, false,
            CardPolicy.NONE, GuardType.NONE, null,
            "Xin chào! Tôi là Trợ lý AI TCS. Tôi có thể giúp gì cho bạn?"
        )),

        Map.entry(AiDomain.IDENTITY_AUTH, new CapabilityPolicy(
            Set.of("FAQ", "POLICY"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/login",
            "Vui lòng truy cập màn hình Đăng nhập hoặc liên hệ hỗ trợ để được trợ giúp về tài khoản."
        )),

        Map.entry(AiDomain.PROFILE_GUARDIAN, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/profile",
            "Bạn có thể cập nhật thông tin cá nhân và hồ sơ người học trong mục 'Hồ sơ cá nhân'."
        )),

        Map.entry(AiDomain.VERIFICATION, new CapabilityPolicy(
            Set.of("FAQ", "POLICY"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/profile",
            "Hồ sơ gia sư và trung tâm được duyệt trong vòng 24–48h làm việc sau khi tải đủ CCCD và bằng cấp trong mục 'Hồ sơ cá nhân'."
        )),

        Map.entry(AiDomain.MARKETPLACE, new CapabilityPolicy(
            Set.of("TUTOR", "CLASS", "FAQ"), false, Set.of(), true, true,
            CardPolicy.TUTOR_CARDS, GuardType.TUTOR_NAME_SCRUB, "/tim-gia-su",
            "Hiện tại chưa tìm thấy kết quả phù hợp với tiêu chí của bạn. Bạn vui lòng thử tìm với bộ lọc rộng hơn tại mục 'Tìm gia sư'."
        )),

        Map.entry(AiDomain.TUTOR_OPS, new CapabilityPolicy(
            Set.of("FAQ", "SCHEDULE"), true, Set.of("TUTOR"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/tutor/schedule",
            "Bạn có thể quản lý lịch dạy, điểm danh và xin đổi lịch trong mục 'Lịch dạy'."
        )),

        Map.entry(AiDomain.CENTER_OPS, new CapabilityPolicy(
            Set.of("FAQ", "CENTER"), true, Set.of("TUTOR_CENTER", "PLATFORM_ADMIN"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/center",
            "Chức năng quản lý gia sư, tuyển dụng và hợp đồng dành cho Quản trị viên Trung tâm tại trang 'Quản lý Trung tâm'."
        )),

        Map.entry(AiDomain.FINANCE_WALLET, new CapabilityPolicy(
            Set.of("WALLET", "ESCROW", "FAQ"), true, Set.of("TUTOR", "TUTOR_CENTER", "PARENT", "STUDENT"), true, false,
            CardPolicy.FINANCE_LINK_ONLY, GuardType.FINANCE_LOGIN_GUARD, "/finance",
            "Thông tin tài chính được bảo mật. Vui lòng đăng nhập để xem số dư ví hoặc yêu cầu rút tiền tại mục 'Ví tiền & Tài chính'."
        )),

        Map.entry(AiDomain.CONTRACT_REVIEW, new CapabilityPolicy(
            Set.of("FAQ", "CONTRACT"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/contracts",
            "Hợp đồng được ký điện tử bảo mật qua mã OTP. Bạn có thể xem danh sách hợp đồng tại mục 'Quản lý Hợp đồng'."
        )),

        Map.entry(AiDomain.MESSAGING_TICKET, new CapabilityPolicy(
            Set.of("FAQ", "TICKET"), false, Set.of(), true, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "Bạn có thể tạo yêu cầu hỗ trợ hoặc khiếu nại trực tiếp tại mục 'Hỗ trợ & Khiếu nại'."
        )),

        Map.entry(AiDomain.TRUST_SAFETY, new CapabilityPolicy(
            Set.of("FAQ", "POLICY"), false, Set.of(), true, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "TCS nghiêm cấm hành vi lách sàn và thu tiền ngoài hệ thống. Vui lòng gửi báo cáo hoặc mở tranh chấp tại mục 'Hỗ trợ & Khiếu nại'."
        )),

        Map.entry(AiDomain.CATALOG_FAQ, new CapabilityPolicy(
            Set.of("FAQ", "SYSTEM_DOC"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/help",
            "TCS là nền tảng kết nối gia sư và học viên uy tín với hợp đồng điện tử và ký quỹ Escrow bảo vệ quyền lợi."
        )),

        Map.entry(AiDomain.PLATFORM_ADMIN, new CapabilityPolicy(
            Set.of("ADMIN_STATS", "SYSTEM_DOC"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.STATS_NUMBER_GUARD, "/platform",
            "Trang quản trị và báo cáo doanh thu dành riêng cho Quản trị viên hệ thống tại bảng điều khiển Admin."
        ))
    );

    private static final Map<AiSubIntent, CapabilityPolicy> SUB_INTENT_POLICIES = Map.ofEntries(
        // Safety & Conversation
        Map.entry(AiSubIntent.HUMAN_SUPPORT_REQUEST, new CapabilityPolicy(
            Set.of(), false, Set.of(), false, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "Bạn có thể kết nối ngay với đội ngũ hỗ trợ tại mục 'Hỗ trợ & Khiếu nại'."
        )),
        Map.entry(AiSubIntent.BOT_CAPABILITY_ASK, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), false, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, null,
            "Tôi là Trợ lý AI TCS. Tôi có thể hỗ trợ bạn tìm gia sư, tìm lớp, giải đáp chính sách, tính toán và hỗ trợ học tập."
        )),

        // Auth
        Map.entry(AiSubIntent.LOGIN_HELP, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/login",
            "Vui lòng truy cập màn hình Đăng nhập để tiếp tục."
        )),
        Map.entry(AiSubIntent.REGISTER_HELP, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/register",
            "Bạn có thể bấm vào nút 'Đăng ký' ở góc trên màn hình để tạo tài khoản mới."
        )),
        Map.entry(AiSubIntent.PASSWORD_FORGOT_HELP, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/forgot-password",
            "Bạn có thể bấm vào liên kết 'Quên mật khẩu' trên màn hình Đăng nhập để đặt lại mật khẩu mới."
        )),

        // Marketplace
        Map.entry(AiSubIntent.FIND_TUTOR, new CapabilityPolicy(
            Set.of("TUTOR"), false, Set.of(), true, true,
            CardPolicy.TUTOR_CARDS, GuardType.TUTOR_NAME_SCRUB, "/tim-gia-su",
            "Hiện tại chưa tìm thấy gia sư phù hợp với tiêu chí của bạn. Bạn vui lòng thử mở rộng điều kiện lọc tại trang 'Tìm gia sư'."
        )),
        Map.entry(AiSubIntent.FIND_CLASS, new CapabilityPolicy(
            Set.of("CLASS"), false, Set.of(), true, true,
            CardPolicy.CLASS_CARDS, GuardType.NONE, "/lop-hoc",
            "Hiện tại chưa có lớp học nào đang mở phù hợp với tiêu chí của bạn. Bạn có thể xem danh sách lớp đang mở tại mục 'Lớp học'."
        )),
        Map.entry(AiSubIntent.CREATE_CLASS, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of("PARENT", "STUDENT"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/tao-lop",
            "Bạn có thể đăng bài tìm gia sư hoặc tạo yêu cầu học mới tại mục 'Tạo lớp học'."
        )),

        // Tutor Ops
        Map.entry(AiSubIntent.TUTOR_SCHEDULE_VIEW, new CapabilityPolicy(
            Set.of("FAQ", "SCHEDULE"), true, Set.of("TUTOR"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/tutor/schedule",
            "Bạn có thể theo dõi và sắp xếp ca dạy tại mục 'Lịch dạy'."
        )),
        Map.entry(AiSubIntent.TUTOR_ATTENDANCE_MARK, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of("TUTOR"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/tutor/classes",
            "Bạn có thể thực hiện điểm danh học viên sau mỗi buổi học tại mục 'Lớp học của tôi'."
        )),
        Map.entry(AiSubIntent.TUTOR_RESCHEDULE_REQUEST, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of("TUTOR"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/tutor/schedule",
            "Bạn có thể gửi yêu cầu dời hoặc đổi lịch buổi dạy tại mục 'Lịch dạy'."
        )),
        Map.entry(AiSubIntent.TUTOR_SUBSTITUTE_REQUEST, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of("TUTOR"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/tutor/classes",
            "Bạn có thể tạo yêu cầu tìm người dạy thay tại mục 'Lớp học của tôi'."
        )),

        // Finance
        Map.entry(AiSubIntent.WALLET_VIEW, new CapabilityPolicy(
            Set.of("WALLET"), true, Set.of("TUTOR", "TUTOR_CENTER", "PARENT", "STUDENT"), true, false,
            CardPolicy.FINANCE_LINK_ONLY, GuardType.FINANCE_LOGIN_GUARD, "/finance",
            "Vui lòng đăng nhập để xem số dư ví và chi tiết thu nhập của bạn tại mục 'Ví tiền & Tài chính'."
        )),
        Map.entry(AiSubIntent.WALLET_TOPUP, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of("PARENT", "STUDENT", "TUTOR"), true, false,
            CardPolicy.FINANCE_LINK_ONLY, GuardType.NONE, "/finance",
            "Bạn có thể nạp tiền vào ví bằng hình thức quét mã VietQR tự động tại mục 'Ví tiền & Tài chính'."
        )),
        Map.entry(AiSubIntent.WITHDRAWAL_REQUEST, new CapabilityPolicy(
            Set.of("FAQ", "WALLET"), true, Set.of("TUTOR", "TUTOR_CENTER"), true, false,
            CardPolicy.FINANCE_LINK_ONLY, GuardType.FINANCE_LOGIN_GUARD, "/finance",
            "Gia sư và trung tâm có thể gửi yêu cầu rút tiền về tài khoản ngân hàng chính chủ tại mục 'Ví tiền & Tài chính'."
        )),
        Map.entry(AiSubIntent.ESCROW_EXPLAIN, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/help",
            "Cơ chế Escrow giữ học phí an toàn cho đến khi buổi học hoàn tất."
        )),
        Map.entry(AiSubIntent.PLATFORM_FEE_EXPLAIN, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/help",
            "Phí nền tảng TCS duy trì hệ thống và đảm bảo giao dịch an toàn."
        )),
        Map.entry(AiSubIntent.REFUND_POLICY, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/help",
            "Chính sách hoàn tiền học phí được xử lý minh bạch qua hệ thống Escrow."
        )),

        // Contract & Review
        Map.entry(AiSubIntent.CONTRACT_LIST_HELP, new CapabilityPolicy(
            Set.of("FAQ", "CONTRACT"), true, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/contracts",
            "Xem danh sách hợp đồng điện tử tại mục 'Quản lý Hợp đồng'."
        )),
        Map.entry(AiSubIntent.CONTRACT_SIGN_OTP, new CapabilityPolicy(
            Set.of("FAQ", "CONTRACT"), true, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/contracts",
            "Xác nhận ký hợp đồng lớp học an toàn bằng mã OTP tại mục 'Quản lý Hợp đồng'."
        )),
        Map.entry(AiSubIntent.REVIEW_CREATE_HELP, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of("PARENT", "STUDENT"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/classes",
            "Đánh giá và gửi nhận xét về gia sư sau khóa học tại mục 'Lớp học'."
        )),
        Map.entry(AiSubIntent.REPUTATION_VIEW_HELP, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/profile",
            "Điểm uy tín của gia sư được tính dựa trên đánh giá và mức độ hoàn thành lớp học."
        )),

        // Messaging & Tickets
        Map.entry(AiSubIntent.MESSAGING_OPEN_HELP, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/chat",
            "Bạn có thể trò chuyện trực tiếp qua mục 'Tin nhắn'."
        )),
        Map.entry(AiSubIntent.SUPPORT_TICKET_CREATE, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "Bạn có thể gửi yêu cầu hỗ trợ hoặc tạo phiếu khiếu nại tại mục 'Hỗ trợ & Khiếu nại'."
        )),
        Map.entry(AiSubIntent.SUPPORT_TICKET_STATUS, new CapabilityPolicy(
            Set.of("FAQ"), true, Set.of(), true, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "Kiểm tra trạng thái và tiến độ xử lý ticket tại mục 'Hỗ trợ & Khiếu nại'."
        )),
        Map.entry(AiSubIntent.SUPPORT_TICKET_SLA, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "Thời gian phản hồi cam kết SLA là 24h đối với yêu cầu thông thường."
        )),

        // Trust & Safety
        Map.entry(AiSubIntent.REPORT_CIRCUMVENTION, new CapabilityPolicy(
            Set.of("FAQ", "POLICY"), false, Set.of(), true, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "TCS bảo vệ giao dịch qua Escrow. Báo cáo lách sàn hoặc gian lận được tiếp nhận tại mục 'Hỗ trợ & Khiếu nại'."
        )),
        Map.entry(AiSubIntent.DISPUTE_OPEN_HELP, new CapabilityPolicy(
            Set.of("FAQ", "POLICY"), true, Set.of(), true, false,
            CardPolicy.TICKET_LINK_ONLY, GuardType.NONE, "/support/tickets",
            "Tranh chấp lớp học có thể được mở khi có vi phạm cam kết giảng dạy tại mục 'Hỗ trợ & Khiếu nại'."
        )),
        Map.entry(AiSubIntent.PENALTY_EXPLAIN, new CapabilityPolicy(
            Set.of("FAQ", "POLICY"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/help",
            "Hành vi vi phạm quy định sàn có thể bị trừ điểm uy tín hoặc khóa tài khoản vĩnh viễn."
        )),

        // Admin & Stats
        Map.entry(AiSubIntent.PLATFORM_STATS, new CapabilityPolicy(
            Set.of("PLATFORM_STATS"), false, Set.of(), false, true,
            CardPolicy.NONE, GuardType.STATS_NUMBER_GUARD, null,
            "Hiện tại không thể truy xuất số liệu thống kê hệ thống từ cơ sở dữ liệu."
        )),
        Map.entry(AiSubIntent.ADMIN_REVENUE_REPORT, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.STATS_NUMBER_GUARD, "/platform/analytics",
            "Báo cáo doanh thu và phân tích dòng tiền dành riêng cho Quản trị viên hệ thống tại bảng thống kê Admin."
        )),
        Map.entry(AiSubIntent.ADMIN_DASHBOARD, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.STATS_NUMBER_GUARD, "/platform",
            "Bảng điều khiển quản trị hệ thống dành riêng cho Quản trị viên tại trang Admin."
        )),
        Map.entry(AiSubIntent.ADMIN_AUDIT_LOG, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.NONE, "/platform",
            "Nhật ký hệ thống (Audit Log) dành riêng cho Quản trị viên tại trang Quản trị."
        )),
        Map.entry(AiSubIntent.ADMIN_AI_REINDEX, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.NONE, "/platform/analytics",
            "Tính năng đánh chỉ mục và thống kê tri thức AI dành riêng cho Quản trị viên tại trang Quản trị AI."
        )),

        // Marketplace Action Policies
        Map.entry(AiSubIntent.APPLY_TO_CLASS, new CapabilityPolicy(
            Set.of("FAQ"), false, Set.of(), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/lop-hoc",
            "Gia sư có thể tìm lớp phù hợp tại mục 'Danh sách lớp học' và bấm nút 'Ứng tuyển' trên trang chi tiết lớp học."
        )),

        // Admin Policies with correct deep-links
        Map.entry(AiSubIntent.ADMIN_VERIFICATION_QUEUE, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.NONE, "/platform/verifications",
            "Hàng đợi xét duyệt hồ sơ gia sư tại trang Quản trị Xác minh."
        )),
        Map.entry(AiSubIntent.ADMIN_WITHDRAWAL_MANAGEMENT, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.NONE, "/platform/withdrawals",
            "Quản lý và duyệt yêu cầu rút tiền tại trang Quản trị Rút tiền."
        )),
        Map.entry(AiSubIntent.ADMIN_DISPUTE_MANAGEMENT, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.NONE, "/platform/disputes",
            "Quản lý và xử lý tranh chấp khiếu nại tại trang Quản trị Tranh chấp."
        )),
        Map.entry(AiSubIntent.ADMIN_CSV_EXPORT, new CapabilityPolicy(
            Set.of("ADMIN_STATS"), true, Set.of("PLATFORM_ADMIN"), true, false,
            CardPolicy.ADMIN_LINK_ONLY, GuardType.NONE, "/platform",
            "Xuất báo cáo dữ liệu CSV tại bảng điều khiển Quản trị viên."
        )),

        // Center Ops Policies
        Map.entry(AiSubIntent.CENTER_TUTOR_MANAGEMENT, new CapabilityPolicy(
            Set.of("FAQ", "CENTER"), true, Set.of("TUTOR_CENTER", "PLATFORM_ADMIN"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/center",
            "Chức năng quản lý gia sư trực thuộc dành cho Quản trị viên Trung tâm tại trang 'Quản lý Trung tâm'."
        )),
        Map.entry(AiSubIntent.CENTER_RECRUITMENT_POST, new CapabilityPolicy(
            Set.of("FAQ", "CENTER"), true, Set.of("TUTOR_CENTER", "PLATFORM_ADMIN"), true, false,
            CardPolicy.FAQ_CARDS, GuardType.NONE, "/center/recruitment",
            "Chức năng đăng bài tuyển dụng gia sư dành cho Trung tâm tại mục 'Tuyển dụng của Trung tâm'."
        ))
    );

    public CapabilityPolicy getPolicy(AiIntent intent) {
        if (intent == null) return DEFAULT_OUT_OF_SCOPE_POLICY;
        return switch (intent) {
            case FIND_TUTOR -> SUB_INTENT_POLICIES.get(AiSubIntent.FIND_TUTOR);
            case FIND_CLASS -> SUB_INTENT_POLICIES.get(AiSubIntent.FIND_CLASS);
            case CREATE_CLASS -> SUB_INTENT_POLICIES.get(AiSubIntent.CREATE_CLASS);
            case PLATFORM_STATS -> SUB_INTENT_POLICIES.get(AiSubIntent.PLATFORM_STATS);
            case ADMIN_DASHBOARD -> SUB_INTENT_POLICIES.get(AiSubIntent.ADMIN_DASHBOARD);
            case PAYMENT_SUPPORT -> DOMAIN_POLICIES.get(AiDomain.FINANCE_WALLET);
            case TICKET_SUPPORT -> DOMAIN_POLICIES.get(AiDomain.MESSAGING_TICKET);
            case TUTOR_VERIFICATION -> DOMAIN_POLICIES.get(AiDomain.VERIFICATION);
            case TUTOR_OPTIMIZATION -> DOMAIN_POLICIES.get(AiDomain.PROFILE_GUARDIAN);
            case CENTER_MANAGEMENT -> DOMAIN_POLICIES.get(AiDomain.CENTER_OPS);
            case FAQ_SUPPORT -> DOMAIN_POLICIES.get(AiDomain.CATALOG_FAQ);
            case OUT_OF_SCOPE -> DEFAULT_OUT_OF_SCOPE_POLICY;
        };
    }

    public CapabilityPolicy getPolicy(AiDomain domain, AiSubIntent subIntent) {
        if (subIntent != null && SUB_INTENT_POLICIES.containsKey(subIntent)) {
            return SUB_INTENT_POLICIES.get(subIntent);
        }
        if (domain != null && DOMAIN_POLICIES.containsKey(domain)) {
            return DOMAIN_POLICIES.get(domain);
        }
        return DEFAULT_OUT_OF_SCOPE_POLICY;
    }
}
