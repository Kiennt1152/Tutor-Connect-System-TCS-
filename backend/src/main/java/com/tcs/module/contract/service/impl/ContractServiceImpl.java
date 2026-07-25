package com.tcs.module.contract.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.entity.ReputationHistory;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.contract.enums.ReviewType;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final AuthHelper authHelper;
    private final ReviewRepository reviewRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final ReputationHistoryRepository reputationHistoryRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        // UC-65: chi khach hang (CLIENT) moi duoc gui danh gia gia su.
        Long reviewerId = authHelper.requireRole(UserRole.CLIENT).getUserId();

        if (request.getAssignmentId() == null || request.getRating() == null) {
            throw new IllegalArgumentException("assignmentId và rating là bắt buộc");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }

        ClassAssignment assignment = classAssignmentRepository
                .findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        TutoringClass tutoringClass = resolveClass(assignment);

        // Chi duoc danh gia lop do chinh minh tao.
        if (!tutoringClass.getCreator().getUserId().equals(reviewerId)) {
            throw new BusinessException("Bạn chỉ có thể đánh giá lớp do chính mình tạo");
        }
        // Chi duoc danh gia sau khi lop hoan thanh.
        if (tutoringClass.getStatus() != TutoringClassStatus.COMPLETED) {
            throw new BusinessException("Chỉ có thể đánh giá sau khi lớp học đã hoàn thành");
        }

        Tutor tutor = assignment.getTutor();
        User tutorUser = tutor.getUser();

        // Khong cho danh gia trung (mot khach - mot phan cong - mot lan).
        reviewRepository
                .findByAssignment_AssignmentIdAndReviewer_UserIdAndReviewType(
                        assignment.getAssignmentId(), reviewerId, ReviewType.CLIENT_TO_TUTOR)
                .ifPresent(existing -> {
                    throw new BusinessException("Bạn đã đánh giá gia sư cho lớp này rồi");
                });

        User reviewer = userRepository
                .findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Review review = new Review();
        review.setAssignment(assignment);
        review.setTutoringClass(tutoringClass);
        review.setReviewer(reviewer);
        review.setReviewee(tutorUser);
        review.setReviewType(ReviewType.CLIENT_TO_TUTOR);
        review.setRating(request.getRating());
        review.setComment(trimToNull(request.getComment()));
        Review saved = reviewRepository.save(review);

        // Cap nhat diem uy tin (rating trung binh) cua gia su + ghi lich su.
        recomputeTutorReputation(tutor, tutorUser.getUserId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForTutor(Long tutorUserId) {
        return reviewRepository.findByReviewee_UserId(tutorUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewableAssignmentResponse> getMyReviewableAssignments() {
        Long clientId = authHelper.requireRole(UserRole.CLIENT).getUserId();

        Map<Long, Review> reviewByAssignment =
                reviewRepository.findByReviewer_UserId(clientId).stream()
                        .filter(r -> r.getReviewType() == ReviewType.CLIENT_TO_TUTOR)
                        .collect(Collectors.toMap(
                                r -> r.getAssignment().getAssignmentId(), r -> r, (a, b) -> a));

        return classAssignmentRepository
                .findByApplication_TutoringClass_Creator_UserId(clientId).stream()
                .filter(a -> a.getApplication() != null
                        && a.getApplication().getTutoringClass() != null)
                .filter(a -> a.getApplication().getTutoringClass().getStatus()
                        == TutoringClassStatus.COMPLETED)
                .map(a -> {
                    TutoringClass c = a.getApplication().getTutoringClass();
                    Tutor tutor = a.getTutor();
                    Review r = reviewByAssignment.get(a.getAssignmentId());
                    return ReviewableAssignmentResponse.builder()
                            .assignmentId(a.getAssignmentId())
                            .classId(c.getClassId())
                            .classTitle(c.getTitle())
                            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                            .classStatus(c.getStatus().name())
                            .tutorUserId(tutor.getUser().getUserId())
                            .tutorName(tutor.getFullName())
                            .reviewed(r != null)
                            .reviewId(r != null ? r.getReviewId() : null)
                            .rating(r != null ? r.getRating() : null)
                            .comment(r != null ? r.getComment() : null)
                            .reviewedAt(r != null ? r.getCreatedAt() : null)
                            .build();
                })
                .toList();
    }

    private TutoringClass resolveClass(ClassAssignment assignment) {
        if (assignment.getApplication() == null || assignment.getApplication().getTutoringClass() == null) {
            throw new BusinessException("Lớp này chưa hỗ trợ đánh giá");
        }
        return assignment.getApplication().getTutoringClass();
    }

    /** Tinh lai rating trung binh cua gia su tu cac danh gia hien (VISIBLE) va ghi lich su thay doi. */
    private void recomputeTutorReputation(Tutor tutor, Long tutorUserId) {
        List<Review> visible = reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                tutorUserId, ReviewType.CLIENT_TO_TUTOR, ReviewStatus.VISIBLE);
        if (visible.isEmpty()) {
            return;
        }
        double average = visible.stream().mapToInt(Review::getRating).average().orElse(0d);
        BigDecimal newScore = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
        BigDecimal oldScore = tutor.getRatingAvg() == null ? BigDecimal.ZERO : tutor.getRatingAvg();
        if (oldScore.compareTo(newScore) == 0) {
            return;
        }
        tutor.setRatingAvg(newScore);
        tutorRepository.save(tutor);

        ReputationHistory history = new ReputationHistory();
        history.setTutor(tutor);
        history.setOldScore(oldScore);
        history.setNewScore(newScore);
        history.setTriggerType("REVIEW");
        history.setReason("Cập nhật điểm trung bình sau đánh giá mới (" + visible.size() + " lượt)");
        reputationHistoryRepository.save(history);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .assignmentId(review.getAssignment().getAssignmentId())
                .reviewerId(review.getReviewer().getUserId())
                .revieweeId(review.getReviewee().getUserId())
                .reviewType(review.getReviewType())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
