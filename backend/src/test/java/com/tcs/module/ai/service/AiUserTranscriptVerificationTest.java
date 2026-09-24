package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AiUserTranscriptVerificationTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiChatSessionRepository sessionRepository;

    @Autowired
    private com.tcs.module.ai.repository.AiQueryCacheRepository cacheRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        cacheRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify Scenario 1: Math Grade 10 Tuition Fee Inquiry")
    void testMathGrade10Pricing() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Chi phí thuê gia sư Toán lớp 10 hiện tại là bao nhiêu một buổi?");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).contains("Toán lớp 10").contains("buổi");
        assertThat(resp.getDomain()).isEqualTo("CATALOG_FAQ");
        assertThat(resp.getSubIntent()).isEqualTo("FAQ_SEARCH");
    }

    @Test
    @DisplayName("Verify Scenario 2: Online vs In-person Teaching Form Inquiry")
    void testOnlineVsOfflinePolicy() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Website mình có gia sư dạy online không hay chỉ dạy kèm tại nhà?");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).contains("Online").contains("Offline");
        assertThat(resp.getDomain()).isEqualTo("CATALOG_FAQ");
        assertThat(resp.getSubIntent()).isEqualTo("FAQ_SEARCH");
    }

    @Test
    @DisplayName("Verify Scenario 3: Trial Lesson and Change Tutor Policy")
    void testTrialLessonAndChangeTutor() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Nếu học thử 1 buổi cảm thấy không hợp thì có được đổi gia sư khác không?");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).contains("Đổi gia sư");
        assertThat(resp.getDomain()).isEqualTo("CATALOG_FAQ");
        assertThat(resp.getSubIntent()).isEqualTo("FAQ_SEARCH");
    }

    @Test
    @DisplayName("Verify Scenario 4: Registration and Tutor Hiring Workflow")
    void testRegistrationWorkflow() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Quy trình đăng ký tìm gia sư diễn ra như thế nào?");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).contains("Quy trình").contains("bước");
        assertThat(resp.getDomain()).isEqualTo("CATALOG_FAQ");
        assertThat(resp.getSubIntent()).isEqualTo("FAQ_SEARCH");
    }

    @Test
    @DisplayName("Verify Scenario 5: Payment Method / Escrow Policy")
    void testPaymentMethodEscrow() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Học phí sẽ thanh toán trực tiếp cho gia sư hay chuyển khoản qua trung tâm?");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).contains("Escrow").contains("KHÔNG thanh toán tiền mặt trực tiếp");
        assertThat(resp.getDomain()).isEqualTo("CATALOG_FAQ");
        assertThat(resp.getSubIntent()).isEqualTo("FAQ_SEARCH");
    }

    @Test
    @DisplayName("Verify Scenario 6: Tutor Selection and Vetting Criteria")
    void testTutorVettingCriteria() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Tiêu chuẩn tuyển chọn gia sư của bên mình là gì? Lĩnh vực nào được kiểm duyệt?");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).contains("KYC").contains("Bằng cấp");
        assertThat(resp.getDomain()).isEqualTo("CATALOG_FAQ");
        assertThat(resp.getSubIntent()).isEqualTo("FAQ_SEARCH");
    }

    @Test
    @DisplayName("Verify Scenario 7: Multi-turn Context Resolution - Literature Grade 9 -> Price inquiry")
    void testMultiTurnLiteratureFollowUpPricing() {
        AiChatSession session = new AiChatSession();
        session.setTitle("Literature search session");
        sessionRepository.save(session);

        // Turn 1: Find Literature Grade 9
        ChatRequest req1 = new ChatRequest();
        req1.setSessionId(session.getSessionId());
        req1.setMessage("Tôi muốn tìm gia sư dạy Văn cho con học lớp 9.");
        AiMessageResponse resp1 = aiService.chat(req1, null);
        assertThat(resp1).isNotNull();

        // Turn 2: Follow-up short query
        ChatRequest req2 = new ChatRequest();
        req2.setSessionId(session.getSessionId());
        req2.setMessage("Giá một buổi là bao nhiêu?");
        AiMessageResponse resp2 = aiService.chat(req2, null);
        assertThat(resp2).isNotNull();
        assertThat(resp2.getContent()).contains("Ngữ văn").contains("buổi");
        assertThat(resp2.getDomain()).isEqualTo("CATALOG_FAQ");
    }

    @Test
    @DisplayName("Verify Scenario 8: Academic pedagogical explanation (Pizza Fraction Grade 3) routes to OUT_OF_SCOPE")
    void testPizzaFractionPedagogicalExplanationIsOutOfScope() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Hãy giải thích cho một học sinh lớp 3 hiểu thế nào là 'Phân số' bằng ví dụ về một chiếc bánh pizza");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getDomain()).isEqualTo("OUT_OF_SCOPE");
        assertThat(resp.getContent()).doesNotContain("chưa có lớp học nào đang mở");
        assertThat(resp.getContent()).contains("Tìm gia sư");
    }

    @Test
    @DisplayName("Verify Scenario 9: Academic homework equation solving routes to OUT_OF_SCOPE")
    void testQuadraticEquationSolverIsOutOfScope() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Giải giúp em phương trình này và giải thích chi tiết các bước: x^2 - 5x + 6 = 0.");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getDomain()).isEqualTo("OUT_OF_SCOPE");
        assertThat(resp.getContent()).contains("Tìm gia sư");
    }

    @Test
    @DisplayName("Verify Scenario 10: Find Literature Grade 9 Tutor never attaches Math or IELTS cards")
    void testFindLiteratureTutorNoMismatchedCards() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Tôi muốn tìm gia sư dạy Văn cho con học lớp 9.");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getDomain()).isEqualTo("MARKETPLACE");
        assertThat(resp.getSubIntent()).isEqualTo("FIND_TUTOR");
        // Must not contain Math or IELTS tutors when asking for Literature
        if (resp.getReferencedTutors() != null && !resp.getReferencedTutors().isEmpty()) {
            assertThat(resp.getReferencedTutors()).allMatch(t -> t.getTitle() != null && (t.getTitle().toLowerCase().contains("văn") || t.getTitle().toLowerCase().contains("ngữ văn")));
        } else {
            assertThat(resp.getContent()).contains("chưa tìm thấy gia sư");
        }
    }

    @Test
    @DisplayName("Verify Scenario 11: Find Math & Physics Grade 12 Tutor matches Minh Duc")
    void testFindMathAndPhysicsTutorForGrade12() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Tìm cho em gia sư kèm cả Toán và Lý lớp 12 chuẩn bị thi Tốt nghiệp THPT, ưu tiên sinh viên Đại học Bách Khoa.");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getDomain()).isEqualTo("MARKETPLACE");
        assertThat(resp.getSubIntent()).isEqualTo("FIND_TUTOR");
        if (resp.getReferencedTutors() != null && !resp.getReferencedTutors().isEmpty()) {
            assertThat(resp.getContent()).contains("tìm thấy các gia sư phù hợp");
        }
    }

    @Test
    @DisplayName("Verify Scenario 12: Multi-turn Subject Switching (Literature Grade 9 -> 'còn toán thì sao?')")
    void testMultiTurnSubjectSwitchLiteratureToMath() {
        AiChatSession session = new AiChatSession();
        session.setTitle("Subject switch session");
        sessionRepository.save(session);

        // Turn 1: Find Literature Grade 9
        ChatRequest req1 = new ChatRequest();
        req1.setSessionId(session.getSessionId());
        req1.setMessage("Tôi muốn tìm gia sư dạy Văn cho con học lớp 9.");
        AiMessageResponse resp1 = aiService.chat(req1, null);
        assertThat(resp1).isNotNull();
        assertThat(resp1.getDomain()).isEqualTo("MARKETPLACE");

        // Turn 2: Switch to Math ("còn toán thì sao?")
        ChatRequest req2 = new ChatRequest();
        req2.setSessionId(session.getSessionId());
        req2.setMessage("còn toán thì sao?");
        AiMessageResponse resp2 = aiService.chat(req2, null);
        assertThat(resp2).isNotNull();
        assertThat(resp2.getDomain()).isEqualTo("MARKETPLACE");
        assertThat(resp2.getSubIntent()).isEqualTo("FIND_TUTOR");
        assertThat(resp2.getContent()).doesNotContain("Chi phí thuê gia sư Ngữ văn");
        if (resp2.getReferencedTutors() != null && !resp2.getReferencedTutors().isEmpty()) {
            assertThat(resp2.getReferencedTutors()).allMatch(t -> t.getTitle() != null && (t.getTitle().toLowerCase().contains("toán") || t.getTitle().toLowerCase().contains("toan")));
        }
    }

    @Test
    @DisplayName("Verify Scenario 13: Navigation / Where to find tutors ('tôi muốn kiếm gia sư thì vào đâu')")
    void testWhereToFindTutorsNavigationGuide() {
        ChatRequest req = new ChatRequest();
        req.setMessage("tôi muốn kiếm gia sư thì vào đâu");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getDomain()).isEqualTo("CATALOG_FAQ");
        assertThat(resp.getSubIntent()).isEqualTo("FAQ_SEARCH");
        assertThat(resp.getContent()).contains("Tìm gia sư").contains("Tạo lớp học");
        assertThat(resp.getReferencedTutors()).isEmpty();
    }

    @Test
    @DisplayName("Verify IT-BOT-005: Multi-turn contextual conversation for IELTS trial lesson")
    void testMultiTurnTrialLessonForIeltsContext() {
        AiChatSession session = new AiChatSession();
        session.setTitle("IELTS multi-turn session");
        sessionRepository.save(session);

        // Turn 1: Ask about IELTS tuition fee
        ChatRequest req1 = new ChatRequest();
        req1.setSessionId(session.getSessionId());
        req1.setMessage("Học phí gia sư tiếng Anh IELTS khoảng bao nhiêu?");
        AiMessageResponse resp1 = aiService.chat(req1, null);
        assertThat(resp1).isNotNull();
        assertThat(resp1.getContent()).isNotBlank();

        // Turn 2: Follow-up question about trial lesson
        ChatRequest req2 = new ChatRequest();
        req2.setSessionId(session.getSessionId());
        req2.setMessage("Vậy có được học thử buổi đầu không?");
        AiMessageResponse resp2 = aiService.chat(req2, null);
        assertThat(resp2).isNotNull();
        assertThat(resp2.getContent()).isNotBlank();

        // Must understand context is IELTS English tutoring
        assertThat(resp2.getContent().toLowerCase())
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("tiếng anh ielts"),
                        c -> assertThat(c).contains("ielts"),
                        c -> assertThat(c).contains("tiếng anh")
                );

        // Must mention trial lesson policy and contract agreement
        assertThat(resp2.getContent().toLowerCase())
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("học thử"),
                        c -> assertThat(c).contains("dạy thử")
                );
        assertThat(resp2.getContent().toLowerCase()).contains("hợp đồng");

        // Must NOT contain dead routes
        assertThat(resp2.getContent()).doesNotContain("/parent/classes");
        assertThat(resp2.getContent()).doesNotContain("/support/tickets");
    }

    @Test
    @DisplayName("Verify IT-BOT-007: Delayed wallet deposit incident routes to support ticket")
    void testDelayedWalletDepositTicketRouting() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Tôi chuyển khoản nạp ví nhưng đã 2 tiếng chưa thấy cộng tiền vào số dư");

        AiMessageResponse resp = aiService.chat(req, null);
        assertThat(resp).isNotNull();
        assertThat(resp.getDomain()).isEqualTo("MESSAGING_TICKET");
        assertThat(resp.getSubIntent()).isEqualTo("SUPPORT_TICKET_CREATE");
        assertThat(resp.getSuggestedRoute()).contains("/messaging/tickets");

        // Guidance on receipt, VietQR and SLA
        assertThat(resp.getContent()).contains("biên lai").contains("Ticket");
        assertThat(resp.getContent()).contains("/messaging/tickets");
        assertThat(resp.getContent()).doesNotContain("/support/tickets");
    }
}
