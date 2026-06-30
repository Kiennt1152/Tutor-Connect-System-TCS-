package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    java.util.List<Review> findByReviewee_UserId(Long userId);
}
