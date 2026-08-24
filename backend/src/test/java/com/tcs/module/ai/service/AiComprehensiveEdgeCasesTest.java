package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class AiComprehensiveEdgeCasesTest {

    @Autowired
    private AiService aiService;

    public record TestCase(
        String group,
        String query,
        String expectedDomain,
        String expectedSubIntent,
        String description
    ) {}

    @Test
    @DisplayName("Run 42 Comprehensive Multi-Domain Real-world Scenarios")
    void test40DiverseScenarios() {
        List<TestCase> tests = List.of(
            // === 1. TÌM KIẾM LỚP & GIA SƯ (Marketplace) ===
            new TestCase("Marketplace", "tìm gia sư toán lớp 10 tại hà đông", "MARKETPLACE", "FIND_TUTOR", "Tìm gia sư Toán 10 Hà Đông"),
            new TestCase("Marketplace", "có lớp dạy kèm tiếng anh giao tiếp nào đang tuyển không", "MARKETPLACE", "FIND_CLASS", "Tìm lớp Tiếng Anh đang tuyển"),
            new TestCase("Marketplace", "tìm lớp dạy hóa lớp 11 online", "MARKETPLACE", "FIND_CLASS", "Tìm lớp Hóa online"),
            new TestCase("Marketplace", "tôi muốn đăng tin tìm gia sư dạy đàn piano", "MARKETPLACE", "CREATE_CLASS", "Đăng bài tạo lớp mới"),
            new TestCase("Marketplace", "làm sao để tạo bài đăng tìm gia sư", "MARKETPLACE", "CREATE_CLASS", "Cách tạo lớp"),
            new TestCase("Marketplace", "gia sư nộp hồ sơ nhận lớp như thế nào", "MARKETPLACE", "APPLY_TO_CLASS", "Gia sư ứng tuyển lớp"),
            new TestCase("Marketplace", "cách ứng tuyển lớp dạy kèm", "MARKETPLACE", "APPLY_TO_CLASS", "Ứng tuyển lớp dạy"),

            // === 2. TÀI CHÍNH, VÍ & NẠP RÚT (Finance & Escrow) ===
            new TestCase("Finance", "nạp tiền vào ví bằng cách nào", "FINANCE_WALLET", "WALLET_TOPUP", "Hướng dẫn nạp ví"),
            new TestCase("Finance", "hướng dẫn rút tiền về tài khoản vietcombank", "FINANCE_WALLET", "WITHDRAWAL_REQUEST", "Rút tiền về ngân hàng"),
            new TestCase("Finance", "rút tiền tối thiểu là bao nhiêu", "FINANCE_WALLET", "WITHDRAWAL_REQUEST", "Số tiền rút tối thiểu"),
            new TestCase("Finance", "tiền ký quỹ escrow hoạt động thế nào", "FINANCE_WALLET", "ESCROW_EXPLAIN", "Giải thích ký quỹ Escrow"),
            new TestCase("Finance", "chính sách hoàn tiền khi gia sư hủy lớp", "FINANCE_WALLET", "REFUND_POLICY", "Chính sách hoàn tiền"),
            new TestCase("Finance", "phí sàn tcs thu bao nhiêu phần trăm", "FINANCE_WALLET", "PLATFORM_FEE_EXPLAIN", "Phí nền tảng"),
            new TestCase("Finance", "xem số dư ví của tôi", "FINANCE_WALLET", "WALLET_VIEW", "Xem số dư ví cá nhân"),

            // === 3. VẬN HÀNH DẠY HỌC (Tutor Ops) ===
            new TestCase("TutorOps", "quên điểm danh buổi học hôm qua thì phải làm sao", "TUTOR_OPS", "TUTOR_ATTENDANCE_MARK", "Điểm danh buổi học"),
            new TestCase("TutorOps", "hôm nay tôi bị ốm muốn dời lịch dạy sang ngày mai", "TUTOR_OPS", "TUTOR_RESCHEDULE_REQUEST", "Xin dời / đổi lịch"),
            new TestCase("TutorOps", "tôi muốn tìm người dạy thay tuần này", "TUTOR_OPS", "TUTOR_SUBSTITUTE_REQUEST", "Tìm người dạy thay"),
            new TestCase("TutorOps", "xem thời khóa biểu dạy ở đâu", "TUTOR_OPS", "TUTOR_SCHEDULE_VIEW", "Xem lịch dạy"),

            // === 4. TRUNG TÂM GIA SƯ (Center Ops) ===
            new TestCase("CenterOps", "làm sao để thêm gia sư vào trung tâm", "CENTER_OPS", "CENTER_TUTOR_MANAGEMENT", "Thêm gia sư vào trung tâm"),
            new TestCase("CenterOps", "đăng bài tuyển dụng gia sư cho trung tâm ở đâu", "CENTER_OPS", "CENTER_RECRUITMENT_POST", "Tuyển dụng gia sư trung tâm"),

            // === 5. HỢP ĐỒNG & ĐÁNH GIÁ (Contract & Review) ===
            new TestCase("Contract", "ký hợp đồng qua mã otp thế nào", "CONTRACT_REVIEW", "CONTRACT_SIGN_OTP", "Ký hợp đồng bằng OTP"),
            new TestCase("Contract", "xem danh sách hợp đồng đã ký ở đâu", "CONTRACT_REVIEW", "CONTRACT_LIST_HELP", "Danh sách hợp đồng"),
            new TestCase("Contract", "đánh giá nhận xét gia sư sau khi học xong", "CONTRACT_REVIEW", "REVIEW_CREATE_HELP", "Đánh giá gia sư"),
            new TestCase("Contract", "điểm uy tín của gia sư tính như thế nào", "CONTRACT_REVIEW", "REPUTATION_VIEW_HELP", "Điểm uy tín gia sư"),

            // === 6. HỖ TRỢ, KHIẾU NẠI & AN TOÀN (Support, Tickets, Trust & Safety) ===
            new TestCase("Support", "gặp nhân viên tư vấn trực tiếp", "CONVERSATION_SAFETY", "HUMAN_SUPPORT_REQUEST", "Yêu cầu gặp nhân viên"),
            new TestCase("Support", "liên hệ chăm sóc khách hàng", "CONVERSATION_SAFETY", "HUMAN_SUPPORT_REQUEST", "Chăm sóc khách hàng"),
            new TestCase("Support", "làm sao để tạo phiếu khiếu nại", "MESSAGING_TICKET", "SUPPORT_TICKET_CREATE", "Tạo ticket hỗ trợ"),
            new TestCase("TrustSafety", "báo cáo gia sư rủ chuyển tiền ngoài sàn", "TRUST_SAFETY", "REPORT_CIRCUMVENTION", "Báo cáo lách sàn"),
            new TestCase("TrustSafety", "gia sư bỏ dạy giữa chừng tôi muốn mở tranh chấp", "TRUST_SAFETY", "DISPUTE_OPEN_HELP", "Mở tranh chấp lớp"),
            new TestCase("TrustSafety", "quy định xử phạt khi vi phạm quy chế sàn", "TRUST_SAFETY", "PENALTY_EXPLAIN", "Quy định xử phạt"),

            // === 7. TÀI KHOẢN, XÁC MINH & BẢO MẬT (Auth, Verification & Profile) ===
            new TestCase("Auth", "làm sao để đăng ký tài khoản mới", "IDENTITY_AUTH", "REGISTER_HELP", "Đăng ký tài khoản"),
            new TestCase("Auth", "tôi quên mật khẩu đăng nhập", "IDENTITY_AUTH", "PASSWORD_FORGOT_HELP", "Quên mật khẩu"),
            new TestCase("Verification", "xác minh cccd và bằng cấp mất bao lâu", "VERIFICATION", "TUTOR_VERIFICATION_HELP", "Xác minh KYC"),
            new TestCase("Profile", "cách đổi ảnh đại diện cá nhân", "PROFILE_GUARDIAN", "PROFILE_UPDATE_HELP", "Cập nhật hồ sơ"),

            // === 8. THỐNG KÊ NỀN TẢNG (Platform Stats) ===
            new TestCase("Stats", "có bao nhiêu gia sư trên hệ thống", "PLATFORM_ADMIN", "PLATFORM_STATS", "Thống kê gia sư"),
            new TestCase("Stats", "có bao nhiêu lớp học đang mở", "PLATFORM_ADMIN", "PLATFORM_STATS", "Thống kê lớp học"),
            new TestCase("Stats", "có bao nhiêu trung tâm gia sư", "PLATFORM_ADMIN", "PLATFORM_STATS", "Thống kê trung tâm"),

            // === 9. HỎI ĐÁP CHUNG & KHUNG LƯƠNG (FAQ & Catalog) ===
            new TestCase("FAQ", "lương trung bình của gia sư là bao nhiêu", "CATALOG_FAQ", "FAQ_SEARCH", "Mức lương trung bình gia sư"),
            new TestCase("FAQ", "học phí trung bình dạy kèm cấp 2 là bao nhiêu", "CATALOG_FAQ", "FAQ_SEARCH", "Học phí trung bình cấp 2"),

            // === 10. XÃ GIAO & TÍNH NĂNG BOT (Safety & Conversation) ===
            new TestCase("SmallTalk", "xin chào bạn", "CONVERSATION_SAFETY", "GREETING", "Chào hỏi"),
            new TestCase("SmallTalk", "bạn có thể giúp được gì cho tôi", "CONVERSATION_SAFETY", "BOT_CAPABILITY_ASK", "Hỏi năng lực của bot"),
            new TestCase("SmallTalk", "cảm ơn bạn nhiều nhé", "CONVERSATION_SAFETY", "THANKS", "Cảm ơn"),
            new TestCase("SmallTalk", "tạm biệt bot", "CONVERSATION_SAFETY", "GOODBYE", "Tạm biệt")
        );

        System.out.println("=========================================================================================");
        System.out.printf("          KIỂM THỬ BỘ 42+ CÂU HỎI THỰC TẾ TRÊN MỌI NGỮ CẢNH HỆ THỐNG TCS (%d SCENARIOS)\n", tests.size());
        System.out.println("=========================================================================================");

        int passed = 0;
        List<String> failedScenarios = new ArrayList<>();

        for (int i = 0; i < tests.size(); i++) {
            TestCase tc = tests.get(i);
            ChatRequest req = new ChatRequest();
            req.setMessage(tc.query());

            AiMessageResponse resp = aiService.chat(req, null);

            assertThat(resp).isNotNull();
            assertThat(resp.getContent()).isNotBlank();

            boolean domainOk = tc.expectedDomain().equals(resp.getDomain());
            boolean subIntentOk = tc.expectedSubIntent().equals(resp.getSubIntent());

            if (domainOk && subIntentOk) {
                passed++;
                System.out.printf("✅ [%02d] PASS | [%-12s] %-35s -> Domain: %-16s | SubIntent: %s\n",
                    i + 1, tc.group(), tc.description(), resp.getDomain(), resp.getSubIntent());
            } else {
                failedScenarios.add(String.format("[%02d] Query: '%s' -> Expected [%s/%s] but got [%s/%s]",
                    i + 1, tc.query(), tc.expectedDomain(), tc.expectedSubIntent(), resp.getDomain(), resp.getSubIntent()));
                System.out.printf("❌ [%02d] FAIL | [%-12s] %-35s -> Expected [%s/%s] but got [%s/%s]\n",
                    i + 1, tc.group(), tc.description(), tc.expectedDomain(), tc.expectedSubIntent(), resp.getDomain(), resp.getSubIntent());
            }
        }

        System.out.println("=========================================================================================");
        System.out.printf("TỔNG KẾT: %d/%d câu hỏi đạt độ chính xác chuẩn theo kịch bản nghiệp vụ TCS (%.1f%%)\n",
            passed, tests.size(), (passed * 100.0 / tests.size()));
        System.out.println("=========================================================================================");

        if (!failedScenarios.isEmpty()) {
            System.out.println("Danh sách các câu hỏi chưa khớp tuyệt đối:");
            failedScenarios.forEach(System.out::println);
        }

        assertThat(passed).isEqualTo(tests.size());
    }
}
