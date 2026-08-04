package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorApplicationRepository extends JpaRepository<TutorApplication, Long> {

    List<TutorApplication> findByTutoringClass_ClassId(Long classId);

    Optional<TutorApplication> findFirstByTutoringClass_ClassIdAndTutor_TutorId(Long classId, Long tutorId);

    long countByTutoringClass_ClassId(Long classId);

    long countByTutoringClass_ClassIdAndStatusNot(Long classId, TutorApplicationStatus status);

    boolean existsByTutoringClass_ClassIdAndTutor_TutorId(Long classId, Long tutorId);

    List<TutorApplication> findByTutor_TutorId(Long tutorId);
}
