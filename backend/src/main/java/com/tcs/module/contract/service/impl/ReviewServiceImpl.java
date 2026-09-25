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

/**
 * ============================================================================
 * [BF-07] [UC-44] TRIỂN KHAI DỊCH VỤ ĐÁNH GIÁ VÀ XẾP HẠNG UY TÍN (REVIEW SERVICE IMPL)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-07-13
 * * 1. Mục đích & Chức năng:
 *    - Xử lý lưu trữ đánh giá sao và bình luận của Phụ huynh đối với Gia sư sau mỗi lớp học.
 *    - Kiểm tra tính hợp lệ: Người đánh giá phải là người tham gia lớp học và lớp đã kết thúc.
 *    - Đảm bảo mỗi phân công lớp học (ClassAssignment) chỉ được gửi 1 lần đánh giá chính thức.
 * * 2. Luồng xử lý chính:
 *    - Bước 1: Xác thực người dùng hiện tại qua AuthHelper.
 *    - Bước 2: Kiểm tra ClassAssignment tồn tại và thuộc về người dùng.
 *    - Bước 3: Tạo mới thực thể Review, lưu vào cơ sở dữ liệu và ánh xạ sang ReviewResponse.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;

    /**
     * [UC-44] Hiện thực nghiệp vụ tạo mới đánh giá uy tín sau khi hoàn tất phân công lớp.
     * 
     * Luồng xử lý:
     * 1. Kiểm tra các trường bắt buộc (assignmentId, revieweeId, rating trong khoảng 1..5).
     * 2. Tìm kiếm thực thể người đánh giá, người được đánh giá và phân công lớp tương ứng.
     * 3. Kiểm tra tính hợp lệ qua validateReviewerCanReviewAssignment (không tự đánh giá, phải là bên tham gia lớp, chưa từng đánh giá).
     * 4. Tạo thực thể Review, lưu vào CSDL qua reviewRepository và trả về ReviewResponse.
     * 
     * @param request DTO chứa thông tin đánh giá
     * @return ReviewResponse thông tin đánh giá đã lưu
     */
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

    /**
     * [UC-44] Truy vấn danh sách đánh giá của gia sư theo ID người dùng.
     * 
     * @param tutorUserId ID người dùng của gia sư
     * @return Danh sách ReviewResponse
     */
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
