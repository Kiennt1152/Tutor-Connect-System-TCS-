package com.tcs.module.ai.service;

import com.tcs.module.ai.constants.AiConstants;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.FaqReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to extract and hydrate reference cards (tutors, classes, FAQs) in batch from the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReferenceCardService {

    private final TutorRepository tutorRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final FaqEntryRepository faqEntryRepository;

    public record ReferenceCards(
        List<TutorReferenceDto> tutors,
        List<ClassReferenceDto> classes,
        List<FaqReferenceDto> faqs
    ) {}

    /**
     * Hydrate reference cards based on high-scoring RAG retrieval sources.
     */
    public ReferenceCards hydrateCards(AiDomain domain, AiSubIntent subIntent, List<AiSourceResponse> allSources) {
        List<TutorReferenceDto> tutors = new ArrayList<>();
        List<ClassReferenceDto> classes = new ArrayList<>();
        List<FaqReferenceDto> faqs = new ArrayList<>();

        if (domain == AiDomain.CONVERSATION_SAFETY || allSources == null || allSources.isEmpty()) {
            return new ReferenceCards(tutors, classes, faqs);
        }

        Set<Long> addedTutorIds = new LinkedHashSet<>();
        Set<Long> addedClassIds = new LinkedHashSet<>();
        Set<Long> addedFaqIds = new LinkedHashSet<>();

        for (AiSourceResponse s : allSources) {
            if (s.getFinalScore() < AiConstants.MIN_REFERENCE_CARD_SCORE) continue;

            if ("TUTOR".equals(s.getSourceType()) && (subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) && addedTutorIds.size() < AiConstants.MAX_REFERENCE_CARDS) {
                try {
                    Long tId = Long.parseLong(s.getSourceId());
                    addedTutorIds.add(tId);
                } catch (NumberFormatException ignored) {}
            } else if ("CLASS".equals(s.getSourceType()) && (subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) && addedClassIds.size() < AiConstants.MAX_REFERENCE_CARDS) {
                try {
                    Long cId = Long.parseLong(s.getSourceId());
                    addedClassIds.add(cId);
                } catch (NumberFormatException ignored) {}
            } else if ("FAQ".equals(s.getSourceType()) && (domain == AiDomain.CATALOG_FAQ || subIntent == AiSubIntent.FAQ_SEARCH) && addedFaqIds.size() < AiConstants.MAX_REFERENCE_CARDS) {
                try {
                    Long fId = Long.parseLong(s.getSourceId());
                    addedFaqIds.add(fId);
                } catch (NumberFormatException ignored) {}
            }
        }

        tutors.addAll(hydrateTutorsByIds(addedTutorIds));
        classes.addAll(hydrateClassesByIds(addedClassIds));
        faqs.addAll(hydrateFaqsByIds(addedFaqIds));

        return new ReferenceCards(tutors, classes, faqs);
    }

    public List<TutorReferenceDto> hydrateTutorsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty() || tutorRepository == null) return List.of();
        Set<Long> set = new LinkedHashSet<>(ids);
        List<Tutor> tutors = tutorRepository.findAllById(set);
        return tutors.stream().map(t -> TutorReferenceDto.builder()
                .tutorId(t.getTutorId())
                .fullName(t.getFullName())
                .avatarUrl(t.getAvatar())
                .title(t.getBio() != null && t.getBio().length() > 60 ? t.getBio().substring(0, 60) + "..." : t.getBio())
                .hourlyRate(t.getHourlyRate())
                .averageRating(t.getRatingAvg() != null ? t.getRatingAvg().doubleValue() : 5.0)
                .teachingAreas(t.getAddress())
                .build()).collect(Collectors.toList());
    }

    public List<ClassReferenceDto> hydrateClassesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty() || tutoringClassRepository == null) return List.of();
        Set<Long> set = new LinkedHashSet<>(ids);
        List<TutoringClass> classes = tutoringClassRepository.findAllById(set);
        return classes.stream().map(c -> ClassReferenceDto.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeLevelName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .tuitionFee(c.getTuitionFee())
                .location(c.getAddress())
                .status(c.getStatus() != null ? c.getStatus().name() : "OPEN")
                .build()).collect(Collectors.toList());
    }

    public List<FaqReferenceDto> hydrateFaqsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty() || faqEntryRepository == null) return List.of();
        Set<Long> set = new LinkedHashSet<>(ids);
        List<FaqEntry> faqs = faqEntryRepository.findAllById(set);
        return faqs.stream().map(f -> FaqReferenceDto.builder()
                .faqId(f.getFaqId())
                .question(f.getQuestion())
                .build()).collect(Collectors.toList());
    }
}
