package com.tcs.module.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.catalog.dto.request.ChatbotAskRequest;
import com.tcs.module.catalog.dto.response.ChatbotAskResponse;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.mapper.CatalogMapper;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.DistrictRepository;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.WardRepository;
import com.tcs.module.catalog.service.GeminiService;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceImplChatbotTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private WardRepository wardRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private FaqEntryRepository faqEntryRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private CatalogMapper catalogMapper;
    @Mock private GeminiService geminiService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private CatalogServiceImpl catalogService;

    /** Sheet askChatbot - UTCID02 (N): không FAQ nào đạt điểm > 0 -> dùng câu trả lời từ AI */
    @Test
    void askChatbotUsesAiWhenNoFaqMatches() {
        ChatbotAskRequest request = new ChatbotAskRequest();
        request.setQuestion("1 + 1 bang may?");
        FaqEntry unrelatedFaq = new FaqEntry();
        unrelatedFaq.setQuestion("He thong thanh toan bang hinh thuc nao?");
        unrelatedFaq.setAnswer("He thong ho tro chuyen khoan ngan hang va vi dien tu.");
        when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc()).thenReturn(List.of(unrelatedFaq));
        when(geminiService.askQuestion("1 + 1 bang may?")).thenReturn(Optional.of("1 + 1 bang 2."));

        ChatbotAskResponse response = catalogService.askChatbot(request);

        assertThat(response.isMatched()).isTrue();
        assertThat(response.isAiGenerated()).isTrue();
        assertThat(response.getAnswer()).isEqualTo("1 + 1 bang 2.");
        verify(geminiService).askQuestion("1 + 1 bang may?");
    }

    // ===================================================================
    //  Sheet: askChatbot
    //  Cham diem: khop trong Question x2, khop trong Answer x1;
    //  nguong toi thieu questionMatches >= min(2, so token).
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("askChatbot")
    class AskChatbot {

        private static final String QUESTION = "thanh toan hoc phi";

        private ChatbotAskRequest ask(String question) {
            ChatbotAskRequest r = new ChatbotAskRequest();
            r.setQuestion(question);
            return r;
        }

        private FaqEntry faq(Long id, String question, String answer) {
            FaqEntry f = new FaqEntry();
            f.setFaqId(id);
            f.setQuestion(question);
            f.setAnswer(answer);
            return f;
        }

        private FaqEntry unrelatedFaq() {
            return faq(9L, "Lam sao doi lich buoi day?", "Vao muc lich day roi bam doi lich.");
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - co FAQ dat diem > 0 -> tra cau tra loi FAQ, aiGenerated = false")
        void utcid01_faqMatched() {
            FaqEntry matched = faq(1L, "Thanh toan hoc phi bang cach nao?",
                    "He thong ho tro chuyen khoan va vi dien tu.");
            when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc())
                    .thenReturn(List.of(matched));

            ChatbotAskResponse res = catalogService.askChatbot(ask(QUESTION));

            assertThat(res.isMatched()).isTrue();
            assertThat(res.isAiGenerated()).isFalse();
            assertThat(res.getFaqId()).isEqualTo(1L);
            assertThat(res.getAnswer()).isEqualTo("He thong ho tro chuyen khoan va vi dien tu.");
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (N) - khong FAQ nao khop, AI tra loi duoc -> aiGenerated = true")
        void utcid02_aiAnswers() {
            when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc())
                    .thenReturn(List.of(unrelatedFaq()));
            when(geminiService.askQuestion(QUESTION)).thenReturn(Optional.of("Ban co the chuyen khoan."));

            ChatbotAskResponse res = catalogService.askChatbot(ask(QUESTION));

            assertThat(res.isMatched()).isTrue();
            assertThat(res.isAiGenerated()).isTrue();
            assertThat(res.getAnswer()).isEqualTo("Ban co the chuyen khoan.");
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - request = null -> matched = false kem goi y tao yeu cau ho tro")
        void utcid03_nullRequest() {
            ChatbotAskResponse res = catalogService.askChatbot(null);

            assertThat(res.isMatched()).isFalse();
            assertThat(res.getSuggestion())
                    .isEqualTo("Không tìm thấy câu trả lời. Vui lòng tạo yêu cầu hỗ trợ.");
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - cau hoi rong/toan khoang trang -> matched = false")
        void utcid04_blankQuestion() {
            assertThat(catalogService.askChatbot(ask("   ")).isMatched()).isFalse();
            assertThat(catalogService.askChatbot(ask(null)).isMatched()).isFalse();
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (B) - moi FAQ deu 0 diem va AI khong tra loi -> matched = false")
        void utcid05_fallsThroughToAiThenFails() {
            when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc())
                    .thenReturn(List.of(unrelatedFaq()));
            when(geminiService.askQuestion(QUESTION)).thenReturn(Optional.empty());

            ChatbotAskResponse res = catalogService.askChatbot(ask(QUESTION));

            assertThat(res.isMatched()).isFalse();
            verify(geminiService).askQuestion(QUESTION);
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (B) - hai FAQ cung diem cao nhat -> chon FAQ dau tien (so sanh > chu khong >=)")
        void utcid06_firstHighestScoreWins() {
            FaqEntry first = faq(1L, "Thanh toan hoc phi bang cach nao?", "Chuyen khoan ngan hang.");
            FaqEntry second = faq(2L, "Thanh toan hoc phi bang cach nao?", "Vi dien tu.");
            when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc())
                    .thenReturn(List.of(first, second));

            ChatbotAskResponse res = catalogService.askChatbot(ask(QUESTION));

            assertThat(res.getFaqId()).isEqualTo(1L);
            assertThat(res.getAnswer()).isEqualTo("Chuyen khoan ngan hang.");
        }
    }
}
