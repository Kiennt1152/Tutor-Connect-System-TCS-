package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.UserPenalty;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    @Query("SELECT p FROM UserPenalty p WHERE (:userId IS NULL OR p.user.userId = :userId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:type IS NULL OR p.penaltyType = :type) " +
           "ORDER BY p.createdAt DESC")
    Page<UserPenalty> search(@Param("userId") Long userId, 
                             @Param("status") UserPenaltyStatus status, 
                             @Param("type") UserPenaltyType type, 
                             Pageable pageable);

    List<UserPenalty> findByUser_UserIdAndStatus(Long userId, UserPenaltyStatus status);

    boolean existsByUser_UserIdAndStatus(Long userId, UserPenaltyStatus status);

    List<UserPenalty> findByStatusAndExpiresAtBefore(UserPenaltyStatus status, LocalDateTime now);

    long countByStatus(UserPenaltyStatus status);
}
