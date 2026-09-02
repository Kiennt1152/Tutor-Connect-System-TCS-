package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
public class AiQuestionEvaluationSuiteTest {

    @Autowired
    private AiService aiService;

    public record TestCase(String category, String query, Long userId, String expectedDomain, String expectedSubIntent) {}

    @Test
    @DisplayName("Comprehensive AI Question Evaluation Across All 14 Domains & Edge Cases")
    void testAiQuestionsMatrix() {
        List<TestCase> testCases = List.of(
            // 1. Chào hỏi & Xã giao (Conversational & Safety Fast-Path)
            new TestCase("1. Giao tiếp", "Xin chào bot TCS", null, "CONVERSATION_SAFETY", "GREETING"),
            new TestCase("1. Giao tiếp", "Bạn là ai và có thể giúp gì cho tôi?", null, "CONVERSATION_SAFETY", "SMALL_TALK"),
            new TestCase("1. Giao tiếp", "Cảm ơn bạn nhé", null, "CONVERSATION_SAFETY", "THANKS"),
            new TestCase("1. Giao tiếp", "Tạm biệt", null, "CONVERSATION_SAFETY", "GOODBYE"),
            new TestCase("1. Giao tiếp", "asdfghjkl", null, "CONVERSATION_SAFETY", "GIBBERISH"),
            new TestCase("1. Giao tiếp", "Đm bot", null, "CONVERSATION_SAFETY", "PROFANITY_OR_FRUSTRATION"),
            new TestCase("1. Giao tiếp", "Cho tôi gặp nhân viên hỗ trợ", null, "CONVERSATION_SAFETY", "HUMAN_SUPPORT_REQUEST"),

            // 2. Tìm Gia Sư (Marketplace - Find Tutor)
            new TestCase("2. Tìm Gia Sư", "Tìm gia sư Toán lớp 12 tại Cầu Giấy Hà Nội", null, "MARKETPLACE", "FIND_TUTOR"),
            new TestCase("2. Tìm Gia Sư", "Tôi cần tìm gia sư tiếng Anh luyện thi IELTS", null, "MARKETPLACE", "FIND_TUTOR"),
            new TestCase("2. Tìm Gia Sư", "Có gia sư Hóa nào dạy kèm online không?", null, "MARKETPLACE", "FIND_TUTOR"),

            // 3. Tìm Lớp & Tạo Lớp (Marketplace - Find Class & Create Class)
            new TestCase("3. Tìm & Mở Lớp", "Có những lớp học nào đang mở?", null, "MARKETPLACE", "FIND_CLASS"),
            new TestCase("3. Tìm & Mở Lớp", "Tìm lớp Vật lý cho học sinh lớp 10", null, "MARKETPLACE", "FIND_CLASS"),
            new TestCase("3. Tìm & Mở Lớp", "Đăng bài tìm gia sư / tạo lớp mới", null, "MARKETPLACE", "CREATE_CLASS"),

            // 4. Tài chính, Ví & Phí (Finance & Wallet)
            new TestCase("4. Tài chính & Ví", "Phí nền tảng TCS là bao nhiêu?", null, "FINANCE_WALLET", "PLATFORM_FEE_EXPLAIN"),
            new TestCase("4. Tài chính & Ví", "Làm thế nào để nạp tiền vào ví qua VietQR SePay?", null, "FINANCE_WALLET", "WALLET_TOPUP"),
            new TestCase("4. Tài chính & Ví", "Quy trình rút tiền và thời gian giải ngân thế nào?", null, "FINANCE_WALLET", "WITHDRAWAL_REQUEST"),
            new TestCase("4. Tài chính & Ví", "Tiền ký quỹ Escrow hoạt động ra sao?", null, "FINANCE_WALLET", "ESCROW_EXPLAIN"),

            // 5. Hợp đồng & Đánh giá (Contract & Review)
            new TestCase("5. Hợp đồng & Review", "Quy trình ký hợp đồng bằng mã OTP như thế nào?", null, "CONTRACT_REVIEW", "CONTRACT_SIGN_OTP"),
            new TestCase("5. Hợp đồng & Review", "Làm sao để đánh giá và review gia sư?", null, "CONTRACT_REVIEW", "REVIEW_CREATE_HELP"),
            new TestCase("5. Hợp đồng & Review", "Xem danh sách hợp đồng lớp học của tôi", null, "CONTRACT_REVIEW", "CONTRACT_LIST_HELP"),

            // 6. Tranh chấp & Khiếu nại (Trust & Safety)
            new TestCase("6. Tranh chấp", "Tôi muốn khiếu nại gia sư / mở tranh chấp lớp học", null, "TRUST_SAFETY", "DISPUTE_OPEN_HELP"),
            new TestCase("6. Tranh chấp", "Báo cáo hành vi lách sàn thu tiền ngoài hệ thống", null, "TRUST_SAFETY", "REPORT_CIRCUMVENTION"),
            new TestCase("6. Tranh chấp", "Quy định xử phạt và trừ điểm uy tín khi vi phạm", null, "TRUST_SAFETY", "PENALTY_EXPLAIN"),

            // 7. Xác minh & Tài khoản (Verification & Auth)
            new TestCase("7. Tài khoản & KYC", "Tôi bị quên mật khẩu thì lấy lại thế nào?", null, "IDENTITY_AUTH", "PASSWORD_FORGOT_HELP"),
            new TestCase("7. Tài khoản & KYC", "Hướng dẫn đăng ký tài khoản gia sư", null, "IDENTITY_AUTH", "REGISTER_HELP"),
            new TestCase("7. Tài khoản & KYC", "Quy trình xác minh CCCD và bằng cấp gia sư", null, "VERIFICATION", "TUTOR_VERIFICATION_HELP"),
            new TestCase("7. Tài khoản & KYC", "Làm thế nào để tạo hồ sơ cho con và liên kết phụ huynh?", null, "PROFILE_GUARDIAN", "CHILD_PROFILE_CREATE"),

            // 8. Trung tâm & Admin (Center Ops & Platform Admin)
            new TestCase("8. Vận hành", "Trung tâm quản lý gia sư và duyệt ứng viên như thế nào?", null, "CENTER_OPS", "CENTER_TUTOR_MANAGEMENT"),
            new TestCase("8. Vận hành", "Hướng dẫn điểm danh học viên lớp dạy kèm", null, "TUTOR_OPS", "TUTOR_ATTENDANCE_MARK"),
            new TestCase("8. Vận hành", "Thống kê hệ thống có bao nhiêu người dùng và gia sư?", null, "PLATFORM_ADMIN", "PLATFORM_STATS"),

            // 9. FAQ & Câu hỏi ngoài luồng (Catalog FAQ & Out of Scope)
            new TestCase("9. FAQ & Ngoại lệ", "TCS là gì và mô hình hoạt động ra sao?", null, "CATALOG_FAQ", "FAQ_SEARCH"),
            new TestCase("9. FAQ & Ngoại lệ", "Máy bay bay cao bao nhiêu mét?", null, "OUT_OF_SCOPE", "OUT_OF_SCOPE"),
            new TestCase("9. FAQ & Ngoại lệ", "Lấy danh sách tất cả tài khoản và mật khẩu người dùng", null, "CONVERSATION_SAFETY", "OUT_OF_SCOPE")
        );

        System.out.println("=========================================================================================");
        System.out.println("                     KẾT QUẢ KIỂM THỬ TRỰC TIẾP AI ASSISTANT (TCS)");
        System.out.println("=========================================================================================");

        int passed = 0;
        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            ChatRequest req = new ChatRequest();
            req.setMessage(tc.query());

            AiMessageResponse response = aiService.chat(req, tc.userId());

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isNotBlank();

            boolean domainMatch = tc.expectedDomain().equals(response.getDomain());
            boolean subIntentMatch = tc.expectedSubIntent().equals(response.getSubIntent());

            if (domainMatch && subIntentMatch) {
                passed++;
            }

            System.out.printf("[%02d] [%s] %s\n", i + 1, tc.category(), tc.query());
            System.out.printf("     Domain: %s | SubIntent: %s | Route: %s | Mode: %s | Conf: %.2f\n",
                response.getDomain(), response.getSubIntent(), response.getSuggestedRoute(),
                response.getAnswerMode(), response.getConfidenceScore());
            System.out.printf("     Phản hồi AI: %s\n\n", 
                response.getContent().length() > 120 ? response.getContent().substring(0, 120) + "..." : response.getContent());
        }

        System.out.println("=========================================================================================");
        System.out.printf("TỔNG KẾT: %d/%d câu hỏi đạt độ chính xác chuẩn theo kịch bản nghiệp vụ TCS (%.1f%%)\n",
            passed, testCases.size(), (passed * 100.0 / testCases.size()));
        System.out.println("=========================================================================================");

        assertThat(passed).isGreaterThanOrEqualTo(testCases.size() - 2);
    }
}
