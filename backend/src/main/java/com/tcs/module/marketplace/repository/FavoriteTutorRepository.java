package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.FavoriteTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteTutorRepository extends JpaRepository<FavoriteTutor, Long> {

    java.util.List<FavoriteTutor> findByUser_UserId(Long userId);

    java.util.Optional<FavoriteTutor> findByUser_UserIdAndTutor_TutorId(Long userId, Long tutorId);

    boolean existsByUser_UserIdAndTutor_TutorId(Long userId, Long tutorId);
}
