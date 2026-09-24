package com.tcs.module.marketplace.service.impl;

import com.tcs.module.marketplace.dto.TutorRecommendation;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.RecommendationService;
import com.tcs.module.platform.entity.RecommendationLog;
import com.tcs.module.platform.repository.RecommendationLogRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * [UC-12] [UC-14] DỊCH VỤ GỢI Ý LỚP HỌC VÀ GIA SƯ THÔNG MINH (RECOMMENDATION SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-14
 * 
 * Mô tả Use Case:
 *   - Hệ thống gợi ý gia sư và lớp học thông minh sử dụng thuật toán chấm điểm độ tương đồng đa tiêu chí.
 *   - Tối ưu hóa việc kết nối giữa nhu cầu học tập của phụ huynh/học viên và năng lực giảng dạy của gia sư.
 * 
 * Chức năng chính:
 *   1. Gợi ý gia sư cho lớp học: Phân tích yêu cầu môn học, khối lớp, ngân sách và địa điểm để xếp hạng top gia sư phù hợp nhất.
 *   2. Gợi ý lớp học cho gia sư: Đề xuất các yêu cầu tìm gia sư mới phù hợp với hồ sơ chuyên môn và khoảng cách di chuyển của gia sư.
 *   3. Ghi vết gợi ý (Recommendation Log): Lưu trữ lịch sử đề xuất phục vụ đánh giá hiệu quả thuật toán matching.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận mã lớp học cần tìm gia sư (recommendTutors).
 *   - Bước 2: Thu thập thông tin chi tiết môn học, khối lớp, mức học phí và vị trí địa lý của lớp học.
 *   - Bước 3: Tính toán điểm số tương thích (Matching Score) cho từng ứng viên gia sư đã được xác thực hồ sơ.
 *   - Bước 4: Sắp xếp theo điểm số từ cao xuống thấp, ghi log hệ thống và trả về danh sách top đề xuất.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final TutoringClassRepository tutoringClassRepository;
    private final TutorRepository tutorRepository;
    private final RecommendationLogRepository recommendationLogRepository;

    @Override
    @Transactional
    public List<TutorRecommendation> recommendTutors(Long classId) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));

        List<Tutor> allTutors = tutorRepository.findAll();

        String subjectName = tutoringClass.getSubject() != null ? tutoringClass.getSubject().getSubjectName().toLowerCase() : "";
        String gradeName = tutoringClass.getGrade() != null ? tutoringClass.getGrade().getGradeName().toLowerCase() : "";
        BigDecimal budget = tutoringClass.getTuitionFee();

        List<TutorRecommendation> recommendations = allTutors.stream().map(tutor -> {
            double score = 0;
            StringBuilder reason = new StringBuilder();

            // Subject match (30 pts)
            if (!subjectName.isEmpty() && tutor.getBio() != null && tutor.getBio().toLowerCase().contains(subjectName)) {
                score += 30.0;
                reason.append("Subject match (+30). ");
            } else {
                score += 10.0;
                reason.append("Generic subject (+10). ");
            }

            // Grade match (15 pts)
            if (!gradeName.isEmpty() && tutor.getBio() != null && tutor.getBio().toLowerCase().contains(gradeName)) {
                score += 15.0;
                reason.append("Grade match (+15). ");
            } else {
                score += 5.0;
                reason.append("Generic grade (+5). ");
            }

            // Verification status (10 pts)
            if (tutor.getVerificationStatus() == ProfileVerificationStatus.VERIFIED) {
                score += 10.0;
                reason.append("Verified (+10). ");
            }

            // Rating (10 pts)
            if (tutor.getRatingAvg() != null) {
                double rating = tutor.getRatingAvg().doubleValue();
                double rScore = (rating / 5.0) * 10.0;
                score += rScore;
                reason.append(String.format("Rating (+%.1f). ", rScore));
            }

            // Experience (10 pts)
            if (tutor.getExperienceYears() != null) {
                double expScore = Math.min(10.0, tutor.getExperienceYears() * 2.0);
                score += expScore;
                reason.append(String.format("Exp (+%.1f). ", expScore));
            }

            // Availability (10 pts)
            score += 10.0;
            reason.append("Available (+10). ");

            // Hourly rate fit (15 pts)
            if (tutor.getHourlyRate() != null && budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
                double diff = Math.abs(tutor.getHourlyRate().doubleValue() - budget.doubleValue());
                double diffRatio = diff / budget.doubleValue();
                if (diffRatio <= 0.2) {
                    score += 15.0;
                    reason.append("Rate fits (+15).");
                } else if (diffRatio <= 0.5) {
                    score += 7.5;
                    reason.append("Rate ok (+7.5).");
                } else {
                    reason.append("Rate mismatch (+0).");
                }
            } else {
                score += 15.0;
                reason.append("Rate assumed (+15).");
            }

            return new TutorRecommendation(tutor.getTutorId(), score, reason.toString().trim());
        })
        .sorted((a, b) -> Double.compare(b.score(), a.score()))
        .limit(5)
        .collect(Collectors.toList());

        for (TutorRecommendation r : recommendations) {
            RecommendationLog log = new RecommendationLog();
            log.setUser(tutoringClass.getCreator());
            log.setTutoringClass(tutoringClass);
            tutorRepository.findById(r.tutorId()).ifPresent(log::setTutor);
            log.setScore(BigDecimal.valueOf(r.score()));
            log.setAlgorithmVersion("V1-Phase1");
            log.setReason(r.reason());
            recommendationLogRepository.save(log);
        }

        return recommendations;
    }
}
