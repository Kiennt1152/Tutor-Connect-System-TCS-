package com.tcs.module.profile.repository;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.profile.entity.Tutor;
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
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findByUser_UserId(Long userId);

    List<Tutor> findByUser_UserIdIn(Collection<Long> userIds);

    List<Tutor> findByUser_StatusAndVerificationStatus(UserStatus userStatus, ProfileVerificationStatus verificationStatus);

    @Query("SELECT COUNT(t) FROM Tutor t WHERE t.user.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(t) FROM Tutor t WHERE t.user.status = :status")
    long countByUserStatus(@Param("status") UserStatus status);

    @Query("SELECT COUNT(t) FROM Tutor t WHERE t.verificationStatus = :status")
    long countByVerificationStatus(@Param("status") ProfileVerificationStatus status);

    @Query("SELECT COUNT(t) FROM Tutor t WHERE t.user.lastLogin >= :since")
    long countByRecentlyActive(@Param("since") LocalDateTime since);

    @Query("SELECT t.user.userId FROM Tutor t")
    List<Long> findAllUserIds();
}
