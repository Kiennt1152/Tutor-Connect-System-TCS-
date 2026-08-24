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
     * Hydrate reference cards based on high-scoring RAG retrieval sources and user entity constraints.
     */
    public ReferenceCards hydrateCards(AiDomain domain, AiSubIntent subIntent, List<AiSourceResponse> allSources) {
        return hydrateCards(domain, subIntent, allSources, Map.of());
    }

    public ReferenceCards hydrateCards(AiDomain domain, AiSubIntent subIntent, List<AiSourceResponse> allSources, Map<String, String> entities) {
        List<TutorReferenceDto> tutors = new ArrayList<>();
        List<ClassReferenceDto> classes = new ArrayList<>();
        List<FaqReferenceDto> faqs = new ArrayList<>();

        if (domain == AiDomain.CONVERSATION_SAFETY || domain == AiDomain.OUT_OF_SCOPE || allSources == null || allSources.isEmpty()) {
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

        tutors.addAll(hydrateTutorsByIds(addedTutorIds, entities));
        classes.addAll(hydrateClassesByIds(addedClassIds));
        faqs.addAll(hydrateFaqsByIds(addedFaqIds));

        return new ReferenceCards(tutors, classes, faqs);
    }

    public List<TutorReferenceDto> hydrateTutorsByIds(Collection<Long> ids) {
        return hydrateTutorsByIds(ids, Map.of());
    }

    public List<TutorReferenceDto> hydrateTutorsByIds(Collection<Long> ids, Map<String, String> entities) {
        if (ids == null || ids.isEmpty() || tutorRepository == null) return List.of();
        Set<Long> set = new LinkedHashSet<>(ids);
        List<Tutor> tutors = tutorRepository.findAllById(set);

        if (entities != null && entities.containsKey("subject") && entities.get("subject") != null && !entities.get("subject").isBlank()) {
            String requestedSubject = entities.get("subject");
            tutors = tutors.stream()
                    .filter(t -> matchesSubject(t.getBio(), requestedSubject))
                    .toList();
        }

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

    private boolean matchesSubject(String bio, String subject) {
        if (bio == null || bio.isBlank() || subject == null || subject.isBlank()) return false;
        String bioNorm = com.tcs.module.ai.util.VietnameseTextNormalizer.normalize(bio);
        String sNorm = com.tcs.module.ai.util.VietnameseTextNormalizer.normalize(subject);

        return switch (sNorm) {
            case "toan", "toan hoc", "math" -> containsWordOrPhrase(bioNorm, "toan", "toan hoc", "giai tich", "hinh hoc", "dai so", "math", "khoi a", "khoi a1", "khoi b", "khoi d");
            case "ly", "vat ly", "physics" -> containsWordOrPhrase(bioNorm, "vat ly", "mon ly", "day ly", "gia su ly", "physics", "khoi a", "khoi a1");
            case "hoa", "hoa hoc", "chemistry" -> containsWordOrPhrase(bioNorm, "hoa hoc", "mon hoa", "day hoa", "gia su hoa", "chemistry", "khoi a", "khoi b");
            case "anh", "tieng anh", "ngoai ngu", "ielts", "toeic", "english" -> containsWordOrPhrase(bioNorm, "tieng anh", "anh van", "ielts", "toeic", "toefl", "english", "mon anh", "day anh", "gia su anh", "khoi d", "khoi a1");
            case "van", "ngu van", "van hoc", "literature" -> containsWordOrPhrase(bioNorm, "ngu van", "van hoc", "mon van", "day van", "gia su van", "khoi d", "khoi c", "chuyen van", "van cap 2", "van cap 3", "van 10", "van 11", "van 12", "van 9", "van 8", "van 7", "van 6");
            case "tin", "tin hoc", "lap trinh", "python", "lap trinh python", "coding", "scratch", "java", "c++" -> containsWordOrPhrase(bioNorm, "tin hoc", "lap trinh", "scratch", "python", "java", "c++", "coding", "mon tin", "day tin", "gia su tin");
            case "sinh", "sinh hoc", "biology" -> containsWordOrPhrase(bioNorm, "sinh hoc", "mon sinh", "day sinh", "gia su sinh", "biology", "khoi b");
            case "su", "lich su", "history" -> containsWordOrPhrase(bioNorm, "lich su", "mon su", "day su", "gia su su", "khoi c");
            case "dia", "dia ly", "geography" -> containsWordOrPhrase(bioNorm, "dia ly", "mon dia", "day dia", "gia su dia", "khoi c");
            case "gdcd" -> containsWordOrPhrase(bioNorm, "gdcd", "giao duc cong dan", "kinh te va phap luat");
            case "tieng phap", "phap", "french" -> containsWordOrPhrase(bioNorm, "tieng phap", "delf", "dalf", "french");
            case "tieng nhat", "nhat", "japanese" -> containsWordOrPhrase(bioNorm, "tieng nhat", "jlpt", "japanese");
            case "tieng trung", "trung", "chinese" -> containsWordOrPhrase(bioNorm, "tieng trung", "hsk", "chinese");
            case "tieng han", "han", "korean" -> containsWordOrPhrase(bioNorm, "tieng han", "topik", "korean");
            default -> containsWordOrPhrase(bioNorm, sNorm);
        };
    }

    private boolean containsWordOrPhrase(String text, String... candidates) {
        for (String c : candidates) {
            String norm = com.tcs.module.ai.util.VietnameseTextNormalizer.normalize(c);
            if (norm.contains(" ")) {
                if (text.contains(norm)) return true;
            } else {
                if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(norm) + "\\b").matcher(text).find()) {
                    return true;
                }
            }
        }
        return false;
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
