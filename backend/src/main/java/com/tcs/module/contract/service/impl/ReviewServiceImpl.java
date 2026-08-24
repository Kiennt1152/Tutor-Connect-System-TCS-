package com.tcs.module.contract.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ReviewService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.security.AuthHelper;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        if (request.getAssignmentId() == null || request.getRevieweeId() == null
                || request.getRating() == null) {
            throw new IllegalArgumentException("assignmentId, revieweeId và rating là bắt buộc");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating phải từ 1 đến 5");
        }
        User reviewer = userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người được đánh giá"));
        ClassAssignment assignment = classAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));
        validateReviewerCanReviewAssignment(reviewer, reviewee, assignment, request);

        Review review = new Review();
        review.setAssignment(assignment);
        review.setTutoringClass(assignment.getApplication().getTutoringClass());
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setReviewType(request.getReviewType());
        review.setRating(java.math.BigDecimal.valueOf(request.getRating()));
        review.setComment(request.getComment());
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForTutor(Long tutorUserId) {
        return reviewRepository.findByReviewee_UserId(tutorUserId).stream()
                .map(this::toResponse)
                .toList();
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

    private void validateReviewerCanReviewAssignment(
            User reviewer,
            User reviewee,
            ClassAssignment assignment,
            CreateReviewRequest request) {

        Long reviewerId = reviewer.getUserId();
        Long revieweeId = reviewee.getUserId();
        if (Objects.equals(reviewerId, revieweeId)) {
            throw new IllegalArgumentException("Bạn không thể tự đánh giá chính mình");
        }

        Long classOwnerId = assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null
                && assignment.getApplication().getTutoringClass().getCreator() != null
                ? assignment.getApplication().getTutoringClass().getCreator().getUserId()
                : null;
        Long tutorUserId = assignment.getTutor() != null && assignment.getTutor().getUser() != null
                ? assignment.getTutor().getUser().getUserId()
                : null;
        boolean participant = Objects.equals(reviewerId, classOwnerId) || Objects.equals(reviewerId, tutorUserId);
        if (!participant) {
            throw new IllegalArgumentException("Chỉ người tham gia lớp mới được đánh giá");
        }

        boolean alreadyReviewed = reviewRepository.findByReviewer_UserId(reviewerId).stream()
                .anyMatch(existing -> existing.getAssignment() != null
                        && Objects.equals(existing.getAssignment().getAssignmentId(), request.getAssignmentId()));
        if (alreadyReviewed) {
            throw new IllegalArgumentException("Bạn đã đánh giá phân công này rồi");
        }
    }
}
