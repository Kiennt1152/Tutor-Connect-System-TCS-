package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<User> findByStatusAndEmailContainingIgnoreCase(
            UserStatus status, String email, Pageable pageable);

    Page<User> findByUserIdIn(Collection<Long> userIds, Pageable pageable);

    java.util.Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR u.status = :status)
            AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                u.phone LIKE CONCAT('%', :keyword, '%') OR
                EXISTS (
                    SELECT 1 FROM PlatformAdmin pa
                    WHERE pa.user = u AND LOWER(pa.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                ) OR
                EXISTS (
                    SELECT 1 FROM Client c
                    WHERE c.user = u AND LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                ) OR
                EXISTS (
                    SELECT 1 FROM Tutor t
                    WHERE t.user = u AND LOWER(t.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                ) OR
                EXISTS (
                    SELECT 1 FROM TutorCenter tc
                    WHERE tc.user = u AND LOWER(tc.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            """)
    Page<User> searchUsers(
            @Param("status") UserStatus status, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.userId IN :userIds
            AND (:status IS NULL OR u.status = :status)
            AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                u.phone LIKE CONCAT('%', :keyword, '%') OR
                EXISTS (
                    SELECT 1 FROM PlatformAdmin pa
                    WHERE pa.user = u AND LOWER(pa.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                ) OR
                EXISTS (
                    SELECT 1 FROM Client c
                    WHERE c.user = u AND LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                ) OR
                EXISTS (
                    SELECT 1 FROM Tutor t
                    WHERE t.user = u AND LOWER(t.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                ) OR
                EXISTS (
                    SELECT 1 FROM TutorCenter tc
                    WHERE tc.user = u AND LOWER(tc.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            """)
    Page<User> searchUsersByIds(
            @Param("userIds") Collection<Long> userIds,
            @Param("status") UserStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
