package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.contract.enums.ReviewType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByReviewee_UserId(Long userId);

    List<Review> findByReviewer_UserId(Long userId);

    long countByAssignment_AssignmentIdAndReviewer_UserIdAndReviewType(
            Long assignmentId, Long reviewerId, ReviewType reviewType);

    boolean existsByTutoringClass_ClassIdAndReviewType(Long classId, ReviewType reviewType);

    List<Review> findByReviewee_UserIdAndReviewTypeAndStatus(
            Long userId, ReviewType reviewType, ReviewStatus status);

    List<Review> findByReviewTypeOrderByCreatedAtDesc(ReviewType reviewType);

    List<Review> findByReviewTypeAndStatusOrderByCreatedAtDesc(
            ReviewType reviewType, ReviewStatus status);
}
