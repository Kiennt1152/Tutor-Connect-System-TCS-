package com.tcs.module.ai.service.provider;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiTutorSearchContextProvider {

    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public List<AiSourceResponse> searchTutors(Map<String, String> entities) {
        List<AiSourceResponse> results = new ArrayList<>();
        
        List<Tutor> activeTutors = tutorRepository.findByUser_StatusAndVerificationStatus(
                UserStatus.ACTIVE, 
                ProfileVerificationStatus.VERIFIED
        );
        
        if (activeTutors.isEmpty()) {
            return List.of();
        }

        String subject = entities.get("subject");
        String location = entities.get("location");
        String maxFeeStr = entities.get("maxFee");
        String grade = entities.get("grade");
        
        Long maxFee = null;
        if (maxFeeStr != null) {
            try {
                maxFee = Long.parseLong(maxFeeStr);
            } catch (Exception ignored) {}
        }

        List<TutorMatch> matches = new ArrayList<>();
        for (Tutor t : activeTutors) {
            String bio = t.getBio() != null ? t.getBio() : "";
            String address = t.getAddress() != null ? t.getAddress() : "";
            String name = t.getFullName() != null ? t.getFullName() : "";
            String bioNorm = VietnameseTextNormalizer.normalize(bio);
            String addressNorm = VietnameseTextNormalizer.normalize(address);
            String nameNorm = VietnameseTextNormalizer.normalize(name);
            String allTextNorm = nameNorm + " " + bioNorm + " " + addressNorm;

            // 1. Mandatory Hard Filter: Subject
            if (subject != null && !subject.isBlank()) {
                if (!matchesSubject(allTextNorm, subject)) {
                    continue; // Skip tutors not teaching the requested subject
                }
            }

            // 2. Mandatory Hard Filter: Location
            if (location != null && !location.isBlank()) {
                if (!matchesLocation(addressNorm + " " + bioNorm, location)) {
                    continue; // Skip tutors not in requested location
                }
            }

            // 3. Mandatory Hard Filter: Max Fee
            if (maxFee != null && t.getHourlyRate() != null) {
                if (t.getHourlyRate().longValue() > maxFee) {
                    continue; // Skip tutors exceeding the budget
                }
            }

            // Calculate relevance score for ranking
            int score = 50;
            if (grade != null && !grade.isBlank()) {
                String gNorm = VietnameseTextNormalizer.normalize(grade);
                if (allTextNorm.contains("lop " + gNorm) || allTextNorm.contains("toan " + gNorm) || allTextNorm.contains(gNorm)) {
                    score += 20;
                }
            }
            if (t.getRatingAvg() != null) {
                score += (int) (t.getRatingAvg().doubleValue() * 10);
            }
            if (maxFee != null && t.getHourlyRate() != null) {
                // Cheaper rates get bonus points
                score += (int) Math.min(20, Math.max(0, (maxFee - t.getHourlyRate().longValue()) / 10000));
            }
            
            matches.add(new TutorMatch(t, score));
        }

        if (matches.isEmpty()) {
            return List.of();
        }

        // Sort by score desc, rating desc, fee asc
        matches.sort(Comparator.comparingInt(TutorMatch::score).reversed()
                .thenComparing((TutorMatch m) -> m.tutor().getRatingAvg() != null ? m.tutor().getRatingAvg() : java.math.BigDecimal.ZERO).reversed()
                .thenComparing((TutorMatch m) -> m.tutor().getHourlyRate() != null ? m.tutor().getHourlyRate() : java.math.BigDecimal.valueOf(Long.MAX_VALUE)));

        List<TutorMatch> topMatches = matches.stream().limit(3).toList();

        for (TutorMatch m : topMatches) {
            Tutor t = m.tutor();
            String snippet = String.format("Gia sư: %s (ID: %d)\nKhu vực: %s\nHọc phí: %s ₫/buổi\nKinh nghiệm: %s năm\nĐánh giá: %s/5★\nGiới thiệu: %s",
                    t.getFullName(),
                    t.getTutorId(),
                    t.getAddress() != null ? t.getAddress() : "Chưa cập nhật",
                    t.getHourlyRate() != null ? String.format(Locale.US, "%,d", t.getHourlyRate().longValue()) : "Thỏa thuận",
                    t.getExperienceYears() != null ? t.getExperienceYears().toString() : "0",
                    t.getRatingAvg() != null ? t.getRatingAvg().toString() : "5.0",
                    t.getBio() != null ? t.getBio() : "");

            results.add(AiSourceResponse.builder()
                    .sourceId(String.valueOf(t.getTutorId()))
                    .sourceType("TUTOR")
                    .title(t.getFullName())
                    .snippet(snippet)
                    .finalScore(1.0)
                    .visibility("PUBLIC")
                    .build());
        }

        return results;
    }

    private boolean matchesSubject(String allTextNorm, String subject) {
        String sNorm = VietnameseTextNormalizer.normalize(subject);
        if (allTextNorm.contains(sNorm)) return true;

        // Subject alias mappings
        return switch (sNorm) {
            case "toan", "toan hoc" -> allTextNorm.contains("toan") || allTextNorm.contains("math");
            case "ly", "vat ly" -> allTextNorm.contains("vat ly") || allTextNorm.contains("ly") || allTextNorm.contains("physics");
            case "hoa", "hoa hoc" -> allTextNorm.contains("hoa") || allTextNorm.contains("chemistry");
            case "anh", "tieng anh", "ngoai ngu" -> allTextNorm.contains("tieng anh") || allTextNorm.contains("ielts") || allTextNorm.contains("toeic") || allTextNorm.contains("english") || allTextNorm.contains("anh");
            case "van", "ngu van" -> allTextNorm.contains("van") || allTextNorm.contains("ngu van") || allTextNorm.contains("literature");
            case "tin", "tin hoc", "lap trinh" -> allTextNorm.contains("tin") || allTextNorm.contains("lap trinh") || allTextNorm.contains("scratch") || allTextNorm.contains("python") || allTextNorm.contains("coding");
            case "sinh", "sinh hoc" -> allTextNorm.contains("sinh") || allTextNorm.contains("biology");
            case "su", "lich su" -> allTextNorm.contains("lich su") || allTextNorm.contains("su");
            case "dia", "dia ly" -> allTextNorm.contains("dia ly") || allTextNorm.contains("dia");
            default -> false;
        };
    }

    private boolean matchesLocation(String locationTextNorm, String location) {
        String lNorm = VietnameseTextNormalizer.normalize(location);
        if (locationTextNorm.contains(lNorm)) return true;

        // Common location alias mappings
        if (lNorm.contains("cau giay") && locationTextNorm.contains("cau giay")) return true;
        if (lNorm.contains("dong da") && locationTextNorm.contains("dong da")) return true;
        if (lNorm.contains("ba dinh") && locationTextNorm.contains("ba dinh")) return true;
        if (lNorm.contains("ha noi") && (locationTextNorm.contains("ha noi") || locationTextNorm.contains("hn"))) return true;
        if (lNorm.contains("hcm") || lNorm.contains("sai gon") || lNorm.contains("ho chi minh")) {
            return locationTextNorm.contains("ho chi minh") || locationTextNorm.contains("hcm") || locationTextNorm.contains("sai gon");
        }
        return false;
    }

    public String renderDeterministicAnswer(List<TutorReferenceDto> tutors) {
        if (tutors == null || tutors.isEmpty()) {
            return "Hiện tại hệ thống chưa tìm thấy gia sư đã xác minh nào khớp hoàn toàn với tiêu chí tìm kiếm của bạn. Bạn có thể thử điều chỉnh mức học phí, mở rộng khu vực tìm kiếm hoặc đăng bài tìm gia sư tại mục /tao-lop để các gia sư chủ động nộp hồ sơ ứng tuyển nhé.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Dựa trên tiêu chí tìm kiếm của bạn, hệ thống TCS tìm thấy các gia sư phù hợp sau:\n\n");
        for (TutorReferenceDto t : tutors) {
            sb.append("• **").append(t.getFullName()).append("**");
            if (t.getHourlyRate() != null) {
                sb.append(" — ").append(String.format(Locale.US, "%,d", t.getHourlyRate().longValue())).append(" ₫/buổi");
            }
            if (t.getAverageRating() != null && t.getAverageRating() > 0) {
                sb.append(String.format(Locale.US, " (%.1f★)", t.getAverageRating()));
            }
            if (t.getTeachingAreas() != null && !t.getTeachingAreas().isBlank()) {
                sb.append(" — Khu vực: ").append(t.getTeachingAreas());
            }
            sb.append("\n");
        }
        sb.append("\nBạn có thể nhấn vào thẻ gia sư bên dưới để xem chi tiết hồ sơ và gửi yêu cầu học.");
        return sb.toString();
    }

    private record TutorMatch(Tutor tutor, int score) {}
}
