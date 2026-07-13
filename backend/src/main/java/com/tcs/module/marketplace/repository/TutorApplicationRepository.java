package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutorApplication;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorApplicationRepository extends JpaRepository<TutorApplication, Long> {

    boolean existsByTutoringClass_ClassIdAndTutor_TutorId(Long classId, Long tutorId);

    Optional<TutorApplication> findFirstByTutoringClass_ClassIdAndTutor_TutorId(Long classId, Long tutorId);
}
