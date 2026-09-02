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
public class AiDiverseScenariosSuiteTest {

    @Autowired
    private AiService aiService;

    public record ScenarioCase(
        String category,
        String query,
        String scenarioDesc,
        Long userId,
        String expectedDomain,
        String expectedSubIntent
    ) {}

    @Test
    @DisplayName("Evaluate AI on 15 New Realistic and Challenging Questions")
    void testDiverseQuestions() {
        List<ScenarioCase> cases = List.of(
            // 1. Ngoại ngữ & Nhu cầu dạy đặc biệt (Specialized Tutor Match)
            new ScenarioCase(
                "1. Nhu cầu chuyên biệt",
                "Tìm gia sư dạy kèm tiếng Nhật sơ cấp N5 tại quận Đống Đa",
                "Ngoại ngữ tiếng Nhật + Trình độ N5 + Địa điểm Đống Đa",
                null, "MARKETPLACE", "FIND_TUTOR"
            ),
            new ScenarioCase(
                "1. Nhu cầu chuyên biệt",
                "Có gia sư nào nhận dạy học sinh mất gốc môn Toán lớp 8 không?",
                "Học sinh mất gốc + Môn Toán lớp 8",
                null, "MARKETPLACE", "FIND_TUTOR"
            ),
            new ScenarioCase(
                "1. Nhu cầu chuyên biệt",
                "Tìm gia sư Tiếng Hàn giao tiếp cho người đi làm",
                "Tiếng Hàn + Giao tiếp người đi làm",
                null, "MARKETPLACE", "FIND_TUTOR"
            ),

            // 2. Tình huống Nạp tiền & Lỗi giao dịch (Payment & Edge Cases)
            new ScenarioCase(
                "2. Thanh toán & Sự cố",
                "Tôi chuyển khoản nạp tiền ghi sai nội dung chuyển khoản thì tiền có bị mất không?",
                "Ghi sai cú pháp chuyển khoản SePay / VietQR",
                null, "FINANCE_WALLET", "WALLET_TOPUP"
            ),
            new ScenarioCase(
                "2. Thanh toán & Sự cố",
                "Sau khi học xong 1 buổi học thì bao lâu gia sư nhận được tiền?",
                "Thời gian giải ngân ký quỹ Escrow sau buổi học",
                null, "FINANCE_WALLET", "ESCROW_EXPLAIN"
            ),
            new ScenarioCase(
                "2. Thanh toán & Sự cố",
                "Tôi có thể thanh toán học phí theo từng buổi hay phải đóng cả tháng?",
                "Hình thức đóng học phí linh hoạt",
                null, "FINANCE_WALLET", "ESCROW_EXPLAIN"
            ),

            // 3. Quy chế Uy tín, Xác minh & Hồ sơ (Trust, KYC & Profile)
            new ScenarioCase(
                "3. Uy tín & KYC",
                "Điểm uy tín của gia sư được tính như thế nào và bao nhiêu điểm thì bị phạt?",
                "Cơ chế tính điểm uy tín và chế tài xử phạt",
                null, "TRUST_SAFETY", "PENALTY_EXPLAIN"
            ),
            new ScenarioCase(
                "3. Uy tín & KYC",
                "Gia sư chưa tốt nghiệp đại học mà chỉ có thẻ sinh viên thì có được xác minh không?",
                "Xác minh bằng thẻ sinh viên thay bằng đại học",
                null, "VERIFICATION", "TUTOR_VERIFICATION_HELP"
            ),
            new ScenarioCase(
                "3. Uy tín & KYC",
                "Ảnh đại diện avatar của tôi tải lên bị lỗi thì hệ thống quy định kích thước thế nào?",
                "Cập nhật ảnh đại diện hồ sơ cá nhân",
                null, "PROFILE_GUARDIAN", "PROFILE_UPDATE_HELP"
            ),

            // 4. Vận hành Trung tâm & Hợp đồng (Center & Contracts)
            new ScenarioCase(
                "4. Vận hành & Hợp đồng",
                "Trung tâm gia sư tuyển dụng và quản lý đội ngũ gia sư thế nào?",
                "Quy trình tuyển dụng và quản lý gia sư trung tâm",
                null, "CENTER_OPS", "CENTER_RECRUITMENT_POST"
            ),
            new ScenarioCase(
                "4. Vận hành & Hợp đồng",
                "Làm thế nào để tạo hợp đồng điện tử khi nhận lớp dạy kèm?",
                "Tạo và quản lý hợp đồng điện tử",
                null, "CONTRACT_REVIEW", "CONTRACT_SIGN_OTP"
            ),

            // 5. Tình huống Khẩn cấp & Nghỉ dạy (Tutor Ops & Attendance)
            new ScenarioCase(
                "5. Xử lý sự cố lớp",
                "Gia sư bị ốm đột xuất muốn xin nghỉ dạy buổi hôm nay thì làm thế nào?",
                "Xin nghỉ dạy / dời lịch khẩn cấp",
                null, "TUTOR_OPS", "TUTOR_RESCHEDULE_REQUEST"
            ),
            new ScenarioCase(
                "5. Xử lý sự cố lớp",
                "Tôi muốn mở khiếu nại vì gia sư đi muộn quá 30 phút",
                "Mở khiếu nại tranh chấp buổi học",
                null, "TRUST_SAFETY", "DISPUTE_OPEN_HELP"
            ),

            // 6. Phương thức Thanh toán & Thống kê Admin (Payment Methods & Platform Stats)
            new ScenarioCase(
                "6. Phương thức & Thống kê",
                "Hệ thống hỗ trợ những phương thức thanh toán nạp tiền nào?",
                "Tra cứu các cổng thanh toán (VietQR, SePay, Chuyển khoản)",
                null, "FINANCE_WALLET", "WALLET_TOPUP"
            ),
            new ScenarioCase(
                "6. Phương thức & Thống kê",
                "Làm sao để xuất dữ liệu thống kê người dùng và gia sư cho Admin?",
                "Thống kê và báo cáo quản trị nền tảng",
                null, "PLATFORM_ADMIN", "PLATFORM_STATS"
            ),
            new ScenarioCase(
                "6. Phương thức & Thống kê",
                "có bao nhiêu lớp đang mở",
                "Thống kê số lượng lớp học đang mở trên sàn",
                null, "PLATFORM_ADMIN", "PLATFORM_STATS"
            ),
            new ScenarioCase(
                "6. Phương thức & Thống kê",
                "có bao nhiêu lớp học đang mở",
                "Thống kê số lượng lớp học đang mở tuyển gia sư",
                null, "PLATFORM_ADMIN", "PLATFORM_STATS"
            ),
            new ScenarioCase(
                "6. Phương thức & Thống kê",
                "có bao nhiêu gia sư trên hệ thống tcs",
                "Thống kê tổng số lượng gia sư trên toàn hệ thống",
                null, "PLATFORM_ADMIN", "PLATFORM_STATS"
            ),
            new ScenarioCase(
                "7. Lương & Ứng tuyển",
                "lương trung bình của gia sư là bao nhiêu",
                "Hỏi khung học phí và mức thu nhập trung bình của gia sư",
                null, "CATALOG_FAQ", "FAQ_SEARCH"
            ),
            new ScenarioCase(
                "7. Lương & Ứng tuyển",
                "gia sư ứng tuyển như nào",
                "Hướng dẫn quy trình gia sư nộp đơn ứng tuyển nhận lớp",
                null, "MARKETPLACE", "APPLY_TO_CLASS"
            )
        );

        System.out.println("=========================================================================================");
        System.out.println("            KẾT QUẢ KIỂM THỬ 15 CÂU HỎI THỰC TẾ & TÌNH HUỐNG MỚI (TCS)");
        System.out.println("=========================================================================================");

        int passed = 0;
        for (int i = 0; i < cases.size(); i++) {
            ScenarioCase sc = cases.get(i);
            ChatRequest req = new ChatRequest();
            req.setMessage(sc.query());

            AiMessageResponse resp = aiService.chat(req, sc.userId());

            assertThat(resp).isNotNull();
            assertThat(resp.getContent()).isNotBlank();

            boolean domainOk = sc.expectedDomain().equals(resp.getDomain());
            boolean subIntentOk = sc.expectedSubIntent().equals(resp.getSubIntent());

            if (domainOk && subIntentOk) {
                passed++;
            }

            System.out.printf("[%02d] [%s] %s\n", i + 1, sc.category(), sc.scenarioDesc());
            System.out.printf("     Câu hỏi: \"%s\"\n", sc.query());
            System.out.printf("     Nhận diện: Domain=%s | SubIntent=%s | Route=%s | Mode=%s | Conf=%.2f\n",
                resp.getDomain(), resp.getSubIntent(), resp.getSuggestedRoute(), resp.getAnswerMode(), resp.getConfidenceScore());
            System.out.printf("     Phản hồi AI: %s\n\n",
                resp.getContent().length() > 130 ? resp.getContent().substring(0, 130) + "..." : resp.getContent());
        }

        System.out.println("=========================================================================================");
        System.out.printf("TỔNG KẾT: %d/%d câu hỏi đạt độ chính xác chuẩn theo kịch bản nghiệp vụ TCS (%.1f%%)\n",
            passed, cases.size(), (passed * 100.0 / cases.size()));
        System.out.println("=========================================================================================");

        assertThat(passed).isGreaterThanOrEqualTo(cases.size() - 2);
    }
}
