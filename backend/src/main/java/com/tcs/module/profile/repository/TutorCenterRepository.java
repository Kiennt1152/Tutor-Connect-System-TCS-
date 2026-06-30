package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorCenter;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorCenterRepository extends JpaRepository<TutorCenter, Long> {

    boolean existsByPhone(String phone);

    Optional<TutorCenter> findByUser_UserId(Long userId);
}
