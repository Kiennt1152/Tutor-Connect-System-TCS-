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
}
