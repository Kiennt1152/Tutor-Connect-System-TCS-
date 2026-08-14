package com.tcs.module.profile.repository;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorCenterRepository extends JpaRepository<TutorCenter, Long> {

    Optional<TutorCenter> findByUser_UserId(Long userId);

    List<TutorCenter> findByUser_UserIdIn(Collection<Long> userIds);

    @Query("SELECT COUNT(tc) FROM TutorCenter tc WHERE tc.user.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(tc) FROM TutorCenter tc WHERE tc.user.status = :status")
    long countByUserStatus(@Param("status") UserStatus status);

    @Query("SELECT COUNT(tc) FROM TutorCenter tc WHERE tc.verificationStatus = :status")
    long countByVerificationStatus(@Param("status") ProfileVerificationStatus status);

    @Query("SELECT COUNT(tc) FROM TutorCenter tc WHERE tc.user.lastLogin >= :since")
    long countByRecentlyActive(@Param("since") LocalDateTime since);

    @Query("SELECT tc.user.userId FROM TutorCenter tc")
    List<Long> findAllUserIds();
}
