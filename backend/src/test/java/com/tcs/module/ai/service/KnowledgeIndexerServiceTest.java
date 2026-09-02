package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeIndexerServiceTest {

    @Mock
    private AiKnowledgeChunkRepository chunkRepository;
    @Mock
    private FaqEntryRepository faqEntryRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutoringClassRepository tutoringClassRepository;
    @Mock
    private TutorCertificateRepository certificateRepository;
    @Mock
    private TutorEducationRepository educationRepository;
    @Mock
    private TutorExperienceRepository experienceRepository;
    @Mock
    private EmbeddingService embeddingService;

    private ObjectMapper objectMapper;
    private KnowledgeIndexerService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new KnowledgeIndexerService(
                chunkRepository,
                faqEntryRepository,
                tutorRepository,
                tutoringClassRepository,
                certificateRepository,
                educationRepository,
                experienceRepository,
                embeddingService,
                objectMapper
        );
    }

    @Test
    @DisplayName("reindexAll successfully indexes FAQs and system policies into chunks")
    void shouldReindexAllKnowledgeChunks() {
        FaqEntry faq = new FaqEntry();
        faq.setFaqId(1L);
        faq.setQuestion("Làm sao đăng ký tài khoản?");
        faq.setAnswer("Bấm Đăng ký góc trên bên phải.");
        faq.setCategory("AUTH_PROFILE");
        faq.setPublished(true);

        when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc()).thenReturn(List.of(faq));
        when(tutorRepository.findAll()).thenReturn(List.of());
        when(tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN)).thenReturn(List.of());
        when(embeddingService.getEmbedding(anyString())).thenReturn(Optional.of(new double[]{0.1, 0.2, 0.3}));
        when(chunkRepository.findBySourceTypeAndSourceId(any(), anyString())).thenReturn(Optional.empty());

        Map<String, Integer> stats = service.reindexAll();

        assertThat(stats).isNotNull();
        assertThat(stats.get("indexed")).isGreaterThan(10);
        verify(chunkRepository, atLeast(10)).save(any(AiKnowledgeChunk.class));
    }
}
