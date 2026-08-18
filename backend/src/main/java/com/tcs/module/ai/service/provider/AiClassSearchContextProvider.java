package com.tcs.module.ai.service.provider;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClassSearchContextProvider {

    private final TutoringClassRepository tutoringClassRepository;

    public List<AiSourceResponse> searchClasses(Map<String, String> entities) {
        List<TutoringClass> openClasses = tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN);
        if (openClasses.isEmpty()) {
            return List.of();
        }

        String filterSubject = entities.get("subject");
        String filterGrade = entities.get("grade");
        String filterLocation = entities.get("location");
        String filterMaxFeeStr = entities.get("maxFee");
        BigDecimal filterMaxFee = null;
        if (filterMaxFeeStr != null) {
            try {
                filterMaxFee = new BigDecimal(filterMaxFeeStr);
            } catch (NumberFormatException ignored) {}
        }

        String normSubject = filterSubject != null ? VietnameseTextNormalizer.normalize(filterSubject) : null;
        String normGrade = filterGrade != null ? VietnameseTextNormalizer.normalize(filterGrade) : null;
        String normLocation = filterLocation != null ? VietnameseTextNormalizer.normalize(filterLocation) : null;

        List<ClassScored> scoredList = new ArrayList<>();
        for (TutoringClass c : openClasses) {
            String titleNorm = VietnameseTextNormalizer.normalize(c.getTitle());
            String descNorm = VietnameseTextNormalizer.normalize(c.getDescription() != null ? c.getDescription() : "");
            String addressNorm = VietnameseTextNormalizer.normalize(c.getAddress() != null ? c.getAddress() : "");
            String subjectNameNorm = c.getSubject() != null ? VietnameseTextNormalizer.normalize(c.getSubject().getSubjectName()) : "";
            String gradeNameNorm = c.getGrade() != null ? VietnameseTextNormalizer.normalize(c.getGrade().getGradeName()) : "";
            String academicTextNorm = titleNorm + " " + descNorm + " " + subjectNameNorm + " " + gradeNameNorm;

            // 1. Mandatory Hard Filter: Subject (matched against academic text only, NOT address)
            if (normSubject != null && !normSubject.isBlank()) {
                if (!matchesSubject(academicTextNorm, normSubject)) {
                    continue; // Skip classes not teaching the requested subject
                }
            }

            // 2. Mandatory Hard Filter: Grade
            if (normGrade != null && !normGrade.isBlank()) {
                if (!academicTextNorm.contains("lop " + normGrade) && !academicTextNorm.contains(normGrade) && !gradeNameNorm.contains(normGrade)) {
                    continue; // Skip classes not for the requested grade
                }
            }

            // 3. Mandatory Hard Filter: Location
            if (normLocation != null && !normLocation.isBlank()) {
                if (!addressNorm.contains(normLocation) && !titleNorm.contains(normLocation)) {
                    continue; // Skip classes in different locations
                }
            }

            // 4. Mandatory Hard Filter: Max Fee
            if (filterMaxFee != null && c.getTuitionFee() != null) {
                if (c.getTuitionFee().compareTo(filterMaxFee) > 0) {
                    continue; // Skip classes exceeding the tuition budget
                }
            }

            // Calculate relevance score
            double score = 50.0;
            if (c.getTuitionFee() != null && filterMaxFee != null) {
                score += Math.min(30.0, Math.max(0.0, filterMaxFee.subtract(c.getTuitionFee()).doubleValue() / 10000.0));
            }

            scoredList.add(new ClassScored(c, score));
        }

        if (scoredList.isEmpty()) {
            return List.of();
        }

        scoredList.sort((a, b) -> Double.compare(b.score, a.score));

        List<AiSourceResponse> results = new ArrayList<>();
        int count = 0;
        for (ClassScored cs : scoredList) {
            if (count >= 3) break;
            TutoringClass c = cs.clazz;
            String subjectName = c.getSubject() != null ? c.getSubject().getSubjectName() : "Tổng hợp";
            String gradeName = c.getGrade() != null ? c.getGrade().getGradeName() : "Tự do";
            String feeStr = c.getTuitionFee() != null ? String.format(Locale.US, "%,d ₫/buổi", c.getTuitionFee().longValue()) : "Thỏa thuận";

            String snippet = String.format("Lớp học: %s | Môn: %s | Khối: %s | Học phí: %s | Địa điểm: %s | Hình thức: %s",
                    c.getTitle(), subjectName, gradeName, feeStr,
                    c.getAddress() != null ? c.getAddress() : "Chưa cập nhật",
                    c.getLessonMode() != null ? c.getLessonMode().name() : "OFFLINE");

            results.add(AiSourceResponse.builder()
                    .sourceId(String.valueOf(c.getClassId()))
                    .sourceType("CLASS")
                    .title(c.getTitle())
                    .snippet(snippet)
                    .finalScore(cs.score / 100.0)
                    .build());
            count++;
        }

        return results;
    }

    private boolean matchesSubject(String text, String sNorm) {
        return switch (sNorm) {
            case "toan", "toan hoc" -> containsWordOrPhrase(text, "toan", "toan hoc", "giai tich", "hinh hoc", "dai so", "math", "khoi a", "khoi a1", "khoi b", "khoi d");
            case "ly", "vat ly" -> containsWordOrPhrase(text, "vat ly", "mon ly", "day ly", "physics", "khoi a", "khoi a1");
            case "hoa", "hoa hoc" -> containsWordOrPhrase(text, "hoa hoc", "mon hoa", "day hoa", "chemistry", "khoi a", "khoi b");
            case "anh", "tieng anh", "ngoai ngu" -> containsWordOrPhrase(text, "tieng anh", "anh van", "ielts", "toeic", "toefl", "english", "mon anh", "day anh", "khoi d", "khoi a1");
            case "van", "ngu van", "van hoc" -> containsWordOrPhrase(text, "ngu van", "van hoc", "mon van", "day van", "khoi d", "khoi c", "chuyen van", "van cap 2", "van cap 3");
            case "tin", "tin hoc", "lap trinh" -> containsWordOrPhrase(text, "tin hoc", "lap trinh", "scratch", "python", "java", "c++", "coding", "mon tin", "day tin");
            case "sinh", "sinh hoc" -> containsWordOrPhrase(text, "sinh hoc", "mon sinh", "day sinh", "biology", "khoi b");
            case "su", "lich su" -> containsWordOrPhrase(text, "lich su", "mon su", "day su", "khoi c");
            case "dia", "dia ly" -> containsWordOrPhrase(text, "dia ly", "mon dia", "day dia", "khoi c");
            default -> containsWordOrPhrase(text, sNorm);
        };
    }

    private boolean containsWordOrPhrase(String text, String... candidates) {
        for (String c : candidates) {
            String norm = VietnameseTextNormalizer.normalize(c);
            if (norm.contains(" ")) {
                if (text.contains(norm)) return true;
            } else {
                if (Pattern.compile("\\b" + Pattern.quote(norm) + "\\b").matcher(text).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String renderDeterministicAnswer(List<ClassReferenceDto> classes) {
        if (classes == null || classes.isEmpty()) {
            return "Hiện tại chưa có lớp học nào đang mở khớp với tiêu chí tìm kiếm của bạn. Bạn có thể theo dõi danh sách lớp mới tại mục /lop-hoc.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Dựa trên hệ thống Tutor Connect System, tôi tìm thấy các lớp học đang mở tuyển gia sư sau:\n\n");
        for (ClassReferenceDto c : classes) {
            sb.append("• **").append(c.getTitle()).append("**");
            if (c.getSubjectName() != null) {
                sb.append(" (").append(c.getSubjectName());
                if (c.getGradeLevelName() != null) sb.append(" - ").append(c.getGradeLevelName());
                sb.append(")");
            }
            if (c.getTuitionFee() != null) {
                sb.append(" — ").append(String.format(Locale.US, "%,d", c.getTuitionFee().longValue())).append(" ₫/buổi");
            }
            if (c.getLocation() != null && !c.getLocation().isBlank()) {
                sb.append(" — Địa điểm: ").append(c.getLocation());
            }
            sb.append("\n");
        }
        sb.append("\nBạn có thể nhấn vào thẻ lớp học bên dưới để xem chi tiết và nộp hồ sơ ứng tuyển.");
        return sb.toString();
    }

    private record ClassScored(TutoringClass clazz, double score) {}
}
