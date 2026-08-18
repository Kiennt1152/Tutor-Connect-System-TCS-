package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 150+ Comprehensive Test Suite covering all 14 TCS business domains and safety intents.
 */
class AiFullIntentCoverageTest {

    private IntentClassifier classifier;
    private AiIntentService intentService;
    private AiFallbackService fallbackService;

    @BeforeEach
    void setUp() {
        classifier = new IntentClassifier();
        intentService = new AiIntentService(classifier);
        fallbackService = new AiFallbackService();
    }

    @Nested
    @DisplayName("1. Conversation & Safety Tests (15 cases)")
    class ConversationSafetyTests {

        @ParameterizedTest
        @CsvSource({
            "'xin chào', GREETING",
            "'chào bot', GREETING",
            "'hello tcs', GREETING",
            "'hi bot', GREETING",
            "'alo', GREETING",
            "'chào bạn', GREETING",
            "'tạm biệt', GOODBYE",
            "'bye bot', GOODBYE",
            "'cảm ơn bạn nhé', THANKS",
            "'thank you', THANKS",
            "'bạn làm được gì', BOT_CAPABILITY_ASK",
            "'bạn là ai', SMALL_TALK",
            "'đm bot ngu', PROFANITY_OR_FRUSTRATION",
            "'vcl lừa đảo', PROFANITY_OR_FRUSTRATION",
            "'cho tôi gặp người hỗ trợ', HUMAN_SUPPORT_REQUEST"
        })
        void shouldClassifyConversationSafety(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.CONVERSATION_SAFETY);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);

            // Verify Level 0 fast path message is deterministic
            var l0 = fallbackService.checkLevel0Safety(expectedSubIntent);
            assertThat(l0).isNotNull();
            assertThat(l0.message()).isNotBlank();
        }

        @Test
        @DisplayName("Gibberish random strings are caught by safety fast-path")
        void shouldClassifyGibberish() {
            var detail = classifier.classifyDetailed("asdfghjklzxcv");
            assertThat(detail.domain()).isEqualTo(AiDomain.CONVERSATION_SAFETY);
            assertThat(detail.subIntent()).isEqualTo(AiSubIntent.GIBBERISH);
        }
    }

    @Nested
    @DisplayName("2. Identity & Auth Tests (15 cases)")
    class IdentityAuthTests {

        @ParameterizedTest
        @CsvSource({
            "'làm sao đăng nhập tài khoản', LOGIN_HELP",
            "'hướng dẫn đăng ký tài khoản phụ huynh', REGISTER_HELP",
            "'tôi quên mật khẩu rồi', PASSWORD_FORGOT_HELP",
            "'không nhận được mã OTP xác thực', OTP_SEND_HELP",
            "'nhập mã OTP báo lỗi', OTP_SEND_HELP",
            "'tài khoản bị khóa phải làm sao', LOGIN_HELP",
            "'đăng nhập bằng google như thế nào', LOGIN_HELP",
            "'đăng ký làm gia sư ở đâu', REGISTER_HELP",
            "'đổi mật khẩu tài khoản', PASSWORD_FORGOT_HELP",
            "'phiên đăng nhập hết hạn', LOGIN_HELP",
            "'hướng dẫn đăng ký trung tâm', REGISTER_HELP",
            "'không có quyền truy cập trang này', LOGIN_HELP",
            "'quên tài khoản đăng nhập', PASSWORD_FORGOT_HELP",
            "'reset password nhu the nao', PASSWORD_FORGOT_HELP",
            "'huong dan dang ky tai khoan', REGISTER_HELP"
        })
        void shouldClassifyIdentityAuth(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.IDENTITY_AUTH);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("3. Profile & Guardian Tests (15 cases)")
    class ProfileGuardianTests {

        @ParameterizedTest
        @CsvSource({
            "'cập nhật hồ sơ cá nhân', PROFILE_UPDATE_HELP",
            "'tải ảnh đại diện avatar ở đâu', PROFILE_UPDATE_HELP",
            "'quét căn cước công dân cccd', PROFILE_UPDATE_HELP",
            "'tạo hồ sơ con học viên', CHILD_PROFILE_CREATE",
            "'thêm hồ sơ con để học', CHILD_PROFILE_CREATE",
            "'liên kết tài khoản phụ huynh', GUARDIAN_LINK_HELP",
            "'xác nhận người giám hộ', GUARDIAN_LINK_HELP",
            "'thêm kinh nghiệm dạy học của gia sư', PROFILE_UPDATE_HELP",
            "'cập nhật lịch rảnh gia sư', PROFILE_UPDATE_HELP",
            "'viết bio gia sư thu hút', PROFILE_UPDATE_HELP",
            "'chỉnh sửa thông tin liên hệ', PROFILE_UPDATE_HELP",
            "'tao ho so con', CHILD_PROFILE_CREATE",
            "'lien ket phu huynh', GUARDIAN_LINK_HELP",
            "'cap nhat ho so gia su', PROFILE_UPDATE_HELP",
            "'them lich ranh', PROFILE_UPDATE_HELP"
        })
        void shouldClassifyProfileGuardian(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.PROFILE_GUARDIAN);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("4. Verification Tests (15 cases)")
    class VerificationTests {

        @ParameterizedTest
        @CsvSource({
            "'quy trình xác minh gia sư', TUTOR_VERIFICATION_HELP",
            "'cần giấy tờ gì để duyệt hồ sơ gia sư', TUTOR_VERIFICATION_HELP",
            "'hồ sơ xác minh bị từ chối vì sao', TUTOR_VERIFICATION_HELP",
            "'kiểm tra trạng thái xác minh', TUTOR_VERIFICATION_HELP",
            "'xác minh bằng cấp chứng chỉ', TUTOR_VERIFICATION_HELP",
            "'xác minh hồ sơ trung tâm gia sư', TUTOR_VERIFICATION_HELP",
            "'giấy tờ cccd không hợp lệ', TUTOR_VERIFICATION_HELP",
            "'bao lâu thì duyệt hồ sơ gia sư', TUTOR_VERIFICATION_HELP",
            "'duyệt hồ sơ gia sư ở đâu', TUTOR_VERIFICATION_HELP",
            "'gui lai giay to xac minh', TUTOR_VERIFICATION_HELP",
            "'ho so bi tu choi', TUTOR_VERIFICATION_HELP",
            "'xac minh bang cap', TUTOR_VERIFICATION_HELP",
            "'trang thai duyet ho so', TUTOR_VERIFICATION_HELP",
            "'tai sao ho so chua duoc duyet', TUTOR_VERIFICATION_HELP",
            "'xac minh cccd', TUTOR_VERIFICATION_HELP"
        })
        void shouldClassifyVerification(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.VERIFICATION);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("5. Marketplace & Search Tests (20 cases)")
    class MarketplaceTests {

        @ParameterizedTest
        @CsvSource({
            "'Tìm cho tôi gia sư môn Toán lớp 12 khu vực Cầu Giấy dưới 250k', FIND_TUTOR",
            "'tìm gia sư tiếng anh giao tiếp', FIND_TUTOR",
            "'thuê gia sư hóa lớp 11 đống đa', FIND_TUTOR",
            "'cần gia sư vật lý luyện thi đại học', FIND_TUTOR",
            "'giáo viên dạy kèm toán lớp 9 tại nhà', FIND_TUTOR",
            "'tìm thầy dạy tin học lập trình', FIND_TUTOR",
            "'tim gia su toan lop 12 cau giay', FIND_TUTOR",
            "'thue gia su tieng anh', FIND_TUTOR",
            "'tìm lớp toán 10 đang mở', FIND_CLASS",
            "'danh sách lớp học đang tuyển gia sư', FIND_CLASS",
            "'lớp học tiếng anh cho người đi làm', FIND_CLASS",
            "'tìm lớp dạy kèm hóa', FIND_CLASS",
            "'tim lop hoc dang mo', FIND_CLASS",
            "'đăng bài tìm gia sư', CREATE_CLASS",
            "'tạo yêu cầu học mới', CREATE_CLASS",
            "'tạo lớp tìm người dạy', CREATE_CLASS",
            "'đăng tin tìm gia sư toán', CREATE_CLASS",
            "'tao lop tim gia su', CREATE_CLASS",
            "'gia sư ứng tuyển lớp như thế nào', FIND_CLASS",
            "'phụ huynh chọn gia sư ứng tuyển ra sao', FIND_CLASS"
        })
        void shouldClassifyMarketplace(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.MARKETPLACE);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }

        @Test
        @DisplayName("Extracts all entities properly for tutor search")
        void shouldExtractAllTutorSearchEntities() {
            var result = intentService.classifyAndExtractDetailed("Tìm cho tôi gia sư môn Toán lớp 12 khu vực Cầu Giấy dưới 250k");
            assertThat(result.domain()).isEqualTo(AiDomain.MARKETPLACE);
            assertThat(result.subIntent()).isEqualTo(AiSubIntent.FIND_TUTOR);
            assertThat(result.entities()).containsEntry("subject", "Toán");
            assertThat(result.entities()).containsEntry("grade", "12");
            assertThat(result.entities()).containsEntry("location", "Cầu Giấy");
            assertThat(result.entities().get("maxFee")).isEqualTo("250000");
        }
    }

    @Nested
    @DisplayName("6. Tutor Operations Tests (15 cases)")
    class TutorOperationsTests {

        @ParameterizedTest
        @CsvSource({
            "'xem lịch dạy của gia sư ở đâu', TUTOR_SCHEDULE_VIEW",
            "'điểm danh học viên sau buổi học', TUTOR_ATTENDANCE_MARK",
            "'cách điểm danh lớp học', TUTOR_ATTENDANCE_MARK",
            "'xin dời lịch buổi học', TUTOR_RESCHEDULE_REQUEST",
            "'xin nghỉ dạy một buổi', TUTOR_RESCHEDULE_REQUEST",
            "'tìm gia sư dạy thay thế', TUTOR_SUBSTITUTE_REQUEST",
            "'yêu cầu dạy thay lớp học', TUTOR_SUBSTITUTE_REQUEST",
            "'quy trình nhận lớp dạy kèm', TUTOR_SCHEDULE_VIEW",
            "'xem lich day gia su', TUTOR_SCHEDULE_VIEW",
            "'diem danh hoc vien', TUTOR_ATTENDANCE_MARK",
            "'xin doi lich day', TUTOR_RESCHEDULE_REQUEST",
            "'tim nguoi day thay', TUTOR_SUBSTITUTE_REQUEST",
            "'huong dan diem danh', TUTOR_ATTENDANCE_MARK",
            "'lich day tuan nay', TUTOR_SCHEDULE_VIEW",
            "'doi gio hoc gia su', TUTOR_RESCHEDULE_REQUEST"
        })
        void shouldClassifyTutorOps(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.TUTOR_OPS);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("7. Center Operations Tests (15 cases)")
    class CenterOperationsTests {

        @ParameterizedTest
        @CsvSource({
            "'trung tâm quản lý gia sư ở đâu', CENTER_TUTOR_MANAGEMENT",
            "'thành viên trung tâm gia sư', CENTER_TUTOR_MANAGEMENT",
            "'đăng bài tuyển dụng gia sư cho trung tâm', CENTER_TUTOR_MANAGEMENT",
            "'duyệt gia sư vào trung tâm', CENTER_TUTOR_MANAGEMENT",
            "'hợp đồng trung tâm gia sư', CENTER_TUTOR_MANAGEMENT",
            "'báo cáo doanh thu trung tâm', CENTER_TUTOR_MANAGEMENT",
            "'tạo lớp nhóm cho trung tâm', CENTER_TUTOR_MANAGEMENT",
            "'quan ly trung tam gia su', CENTER_TUTOR_MANAGEMENT",
            "'tuyen ung vien gia su', CENTER_TUTOR_MANAGEMENT",
            "'thanh vien trung tam', CENTER_TUTOR_MANAGEMENT",
            "'tao bai tuyen dung', CENTER_TUTOR_MANAGEMENT",
            "'hop dong trung tam', CENTER_TUTOR_MANAGEMENT",
            "'danh sach gia su trung tam', CENTER_TUTOR_MANAGEMENT",
            "'xoa gia su khoi trung tam', CENTER_TUTOR_MANAGEMENT",
            "'them gia su vao trung tam', CENTER_TUTOR_MANAGEMENT"
        })
        void shouldClassifyCenterOps(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.CENTER_OPS);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("8. Finance & Escrow Tests (20 cases)")
    class FinanceEscrowTests {

        @ParameterizedTest
        @CsvSource({
            "'xem ví tiền của tôi', WALLET_VIEW",
            "'nạp tiền vào ví qua QR SePay', WALLET_TOPUP",
            "'nạp ví bằng chuyển khoản', WALLET_TOPUP",
            "'yêu cầu rút tiền về tài khoản ngân hàng', WITHDRAWAL_REQUEST",
            "'rút tiền lương gia sư', WITHDRAWAL_REQUEST",
            "'escrow là gì', ESCROW_EXPLAIN",
            "'khi nào tiền escrow được giải ngân', ESCROW_EXPLAIN",
            "'quy định ký quỹ học phí', ESCROW_EXPLAIN",
            "'phí nền tảng là bao nhiêu', PLATFORM_FEE_EXPLAIN",
            "'chính sách phí sàn 10%', PLATFORM_FEE_EXPLAIN",
            "'chính sách hoàn tiền học phí', REFUND_POLICY",
            "'yêu cầu hoàn tiền lớp học', REFUND_POLICY",
            "'lương của tôi tháng này bao nhiêu', WALLET_VIEW",
            "'thu nhập gia sư tính thế nào', WALLET_VIEW",
            "'xem lich su giao dich', WALLET_VIEW",
            "'nap tien vao vi', WALLET_TOPUP",
            "'rut tien ve ngan hang', WITHDRAWAL_REQUEST",
            "'tien escrow khi nao nhan duoc', ESCROW_EXPLAIN",
            "'phi san bao nhieu', PLATFORM_FEE_EXPLAIN",
            "'chinh sach hoan tien', REFUND_POLICY"
        })
        void shouldClassifyFinance(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.FINANCE_WALLET);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("9. Contract & Review Tests (15 cases)")
    class ContractReviewTests {

        @ParameterizedTest
        @CsvSource({
            "'xem danh sách hợp đồng ở đâu', CONTRACT_LIST_HELP",
            "'ký hợp đồng lớp học bằng mã OTP', CONTRACT_SIGN_OTP",
            "'hướng dẫn ký hợp đồng điện tử', CONTRACT_SIGN_OTP",
            "'từ chối hợp đồng gia sư', CONTRACT_LIST_HELP",
            "'đánh giá gia sư sau khóa học', REVIEW_CREATE_HELP",
            "'viết review đánh giá buổi dạy', REVIEW_CREATE_HELP",
            "'độ uy tín gia sư tính thế nào', REPUTATION_VIEW_HELP",
            "'xem reputation của gia sư', REPUTATION_VIEW_HELP",
            "'vi sao review bi an', REVIEW_CREATE_HELP",
            "'ky hop dong bang otp', CONTRACT_SIGN_OTP",
            "'danh gia gia su', REVIEW_CREATE_HELP",
            "'xem hop dong lop hoc', CONTRACT_LIST_HELP",
            "'uy tin gia su', REPUTATION_VIEW_HELP",
            "'sign contract otp', CONTRACT_SIGN_OTP",
            "'xem rating va review', REVIEW_CREATE_HELP"
        })
        void shouldClassifyContractReview(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.CONTRACT_REVIEW);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("10. Messaging & Ticket Tests (15 cases)")
    class MessagingTicketTests {

        @ParameterizedTest
        @CsvSource({
            "'nhắn tin với gia sư ở đâu', MESSAGING_OPEN_HELP",
            "'chat với phụ huynh', MESSAGING_OPEN_HELP",
            "'hướng dẫn tạo ticket khiếu nại gia sư', SUPPORT_TICKET_CREATE",
            "'tạo yêu cầu hỗ trợ mới', SUPPORT_TICKET_CREATE",
            "'kiểm tra trạng thái ticket hỗ trợ', SUPPORT_TICKET_STATUS",
            "'thời gian phản hồi SLA của ticket là bao lâu', SUPPORT_TICKET_SLA",
            "'đóng ticket hỗ trợ', SUPPORT_TICKET_CREATE",
            "'mở lại ticket đã đóng', SUPPORT_TICKET_CREATE",
            "'xem thông báo hệ thống', SUPPORT_TICKET_CREATE",
            "'tao ticket ho tro', SUPPORT_TICKET_CREATE",
            "'kiem tra trang thai ticket', SUPPORT_TICKET_STATUS",
            "'quy dinh sla phan hoi', SUPPORT_TICKET_SLA",
            "'nhan tin voi phu huynh', MESSAGING_OPEN_HELP",
            "'mo lai ticket', SUPPORT_TICKET_CREATE",
            "'xem thong bao', SUPPORT_TICKET_CREATE"
        })
        void shouldClassifyMessagingTicket(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.MESSAGING_TICKET);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("11. Trust & Safety Tests (20 cases)")
    class TrustSafetyTests {

        @ParameterizedTest
        @CsvSource({
            "'Làm sao báo cáo gia sư lách sàn?', REPORT_CIRCUMVENTION",
            "'tố cáo gia sư thu tiền ngoài sàn', REPORT_CIRCUMVENTION",
            "'báo cáo trung tâm lách sàn', REPORT_CIRCUMVENTION",
            "'Khi nào nên mở tranh chấp lớp học?', DISPUTE_OPEN_HELP",
            "'quy trình giải quyết tranh chấp', DISPUTE_OPEN_HELP",
            "'tải bằng chứng tranh chấp lên hệ thống', DISPUTE_OPEN_HELP",
            "'khiếu nại lớp học bị hủy vô lý', DISPUTE_OPEN_HELP",
            "'tài khoản bị phạt cảnh cáo', PENALTY_EXPLAIN",
            "'chế tài khi vi phạm quy định sàn', PENALTY_EXPLAIN",
            "'bị phạt trừ điểm uy tín', PENALTY_EXPLAIN",
            "'bao cao gia su lach san', REPORT_CIRCUMVENTION",
            "'to cao vi pham', REPORT_USER_CREATE",
            "'khi nao nen mo tranh chap', DISPUTE_OPEN_HELP",
            "'tai bang chung tranh chap', DISPUTE_OPEN_HELP",
            "'tai khoan bi phat', PENALTY_EXPLAIN",
            "'che tai lach san', REPORT_CIRCUMVENTION",
            "'khieu nai gia su', DISPUTE_OPEN_HELP",
            "'tranh chap hoc phi', DISPUTE_OPEN_HELP",
            "'bao cao nguoi dung vi pham', REPORT_USER_CREATE",
            "'quy dinh phat vi pham', PENALTY_EXPLAIN"
        })
        void shouldClassifyTrustSafety(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.TRUST_SAFETY);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("12. Catalog & Help Tests (15 cases)")
    class CatalogHelpTests {

        @ParameterizedTest
        @CsvSource({
            "'TCS là gì?', FAQ_SEARCH",
            "'hệ thống TCS hoạt động như thế nào', FAQ_SEARCH",
            "'các vai trò trên nền tảng TCS', FAQ_SEARCH",
            "'trung tâm trợ giúp ở đâu', FAQ_SEARCH",
            "'hệ thống hỗ trợ những môn học nào', FAQ_SEARCH",
            "'có những khối lớp nào', FAQ_SEARCH",
            "'khu vực nào được hỗ trợ', FAQ_SEARCH",
            "'quy trình kết nối gia sư', FAQ_SEARCH",
            "'chính sách bảo mật TCS', FAQ_SEARCH",
            "'tcs la gi', FAQ_SEARCH",
            "'trung tam tro giup', FAQ_SEARCH",
            "'cac mon hoc tren tcs', FAQ_SEARCH",
            "'huong dan su dung tcs', FAQ_SEARCH",
            "'chinh sach nen tang', FAQ_SEARCH",
            "'gioi thieu ve tcs', FAQ_SEARCH"
        })
        void shouldClassifyCatalogHelp(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.CATALOG_FAQ);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("13. Platform Admin Tests (20 cases)")
    class PlatformAdminTests {

        @ParameterizedTest
        @CsvSource({
            "'Xem báo cáo doanh thu nền tảng', ADMIN_REVENUE_REPORT",
            "'bảng điều khiển quản trị admin dashboard', ADMIN_DASHBOARD",
            "'xem báo cáo analytics vận hành', ADMIN_DASHBOARD",
            "'thống kê doanh thu phí sàn', ADMIN_REVENUE_REPORT",
            "'dòng tiền cashflow money in money out', ADMIN_REVENUE_REPORT",
            "'lọc task quá hạn SLA', ADMIN_DASHBOARD",
            "'xem nhật ký hệ thống audit log', ADMIN_AUDIT_LOG",
            "'cấu hình hệ thống platform fee', ADMIN_DASHBOARD",
            "'reindex ai knowledge base', ADMIN_AI_REINDEX",
            "'thống kê kiến thức ai', ADMIN_AI_REINDEX",
            "'xem bao cao doanh thu', ADMIN_REVENUE_REPORT",
            "'dashboard quan tri', ADMIN_DASHBOARD",
            "'thong ke doanh thu', ADMIN_REVENUE_REPORT",
            "'audit log he thong', ADMIN_AUDIT_LOG",
            "'reindex knowledge', ADMIN_AI_REINDEX",
            "'queue xac minh ho so admin', ADMIN_VERIFICATION_QUEUE",
            "'quan ly rut tien admin', ADMIN_WITHDRAWAL_MANAGEMENT",
            "'quan ly dispute admin', ADMIN_DISPUTE_MANAGEMENT",
            "'quan tri vien he thong', ADMIN_DASHBOARD",
            "'xuat bao cao csv admin', ADMIN_CSV_EXPORT"
        })
        void shouldClassifyPlatformAdmin(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.PLATFORM_ADMIN);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }

        @ParameterizedTest
        @CsvSource({
            "'Hệ thống có bao nhiêu người dùng?', PLATFORM_STATS",
            "'có bao nhiêu gia sư trên hệ thống', PLATFORM_STATS",
            "'tổng số học viên đăng ký', PLATFORM_STATS",
            "'số lượng lớp học đang mở', PLATFORM_STATS",
            "'thống kê người dùng hệ thống', PLATFORM_STATS"
        })
        void shouldClassifyPlatformStats(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.PLATFORM_ADMIN);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }

    @Nested
    @DisplayName("14. AI Tutoring Tests (15 cases)")
    class AiTutoringTests {

        @ParameterizedTest
        @CsvSource({
            "'giải phương trình bậc 2', AI_TUTORING_MATH",
            "'hướng dẫn làm bài tập toán hình', AI_TUTORING_MATH",
            "'giai phuong trinh', AI_TUTORING_MATH",
            "'bai tap toan 12', AI_TUTORING_MATH",
            "'giải thích thì hiện tại hoàn thành', AI_TUTORING_ENGLISH",
            "'ngữ pháp tiếng anh cơ bản', AI_TUTORING_ENGLISH",
            "'ngu phap tieng anh', AI_TUTORING_ENGLISH",
            "'lập kế hoạch học ielts 7.0', AI_TUTORING_STUDY_PLAN",
            "'ke hoach hoc tap', AI_TUTORING_STUDY_PLAN",
            "'luyện tập ôn thi đại học', AI_TUTORING_STUDY_PLAN",
            "'định lý pitago phát biểu thế nào', AI_TUTORING_SCIENCE",
            "'công thức tính vận tốc', AI_TUTORING_SCIENCE",
            "'bài tập vật lý 10', AI_TUTORING_SCIENCE",
            "'hướng dẫn lập trình python cơ bản', AI_TUTORING_CODE",
            "'debug code java', AI_TUTORING_CODE"
        })
        void shouldClassifyAiTutoring(String query, AiSubIntent expectedSubIntent) {
            var detail = classifier.classifyDetailed(query);
            assertThat(detail.domain()).isEqualTo(AiDomain.AI_TUTORING);
            assertThat(detail.subIntent()).isEqualTo(expectedSubIntent);
        }
    }
}
