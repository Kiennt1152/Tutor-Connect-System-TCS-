package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
