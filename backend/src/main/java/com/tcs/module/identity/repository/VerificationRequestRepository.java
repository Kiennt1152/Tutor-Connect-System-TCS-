package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    Optional<VerificationRequest> findByUser_UserIdAndVerificationType(Long userId, VerificationType verificationType);

    List<VerificationRequest> findByUser_UserIdOrderBySubmittedAtDesc(Long userId);

    List<VerificationRequest> findByStatusOrderBySubmittedAtAsc(VerificationStatus status);

    List<VerificationRequest> findByVerificationTypeAndStatus(VerificationType verificationType, VerificationStatus status);

    @Query("SELECT COUNT(v) FROM VerificationRequest v WHERE v.status = :status")
    long countByStatus(@Param("status") VerificationStatus status);

    boolean existsByUser_UserIdAndVerificationTypeAndStatusIn(
            Long userId,
            VerificationType verificationType,
            List<VerificationStatus> statuses
    );
}
