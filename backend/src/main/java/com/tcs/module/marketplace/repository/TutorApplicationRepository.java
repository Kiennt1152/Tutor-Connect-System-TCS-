package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorApplicationRepository extends JpaRepository<TutorApplication, Long> {

    List<TutorApplication> findByTutoringClass_ClassIdOrderByAppliedAtDesc(Long classId);

    List<TutorApplication> findByTutor_TutorIdOrderByAppliedAtDesc(Long tutorId);

    boolean existsByTutoringClass_ClassIdAndTutor_TutorIdAndStatusIn(
            Long classId, Long tutorId, Collection<TutorApplicationStatus> statuses);

    Optional<TutorApplication> findByApplicationIdAndTutoringClass_Creator_UserId(
            Long applicationId, Long userId);

    Optional<TutorApplication> findByApplicationIdAndTutor_User_UserId(
            Long applicationId, Long userId);

    List<TutorApplication> findByTutoringClass_ClassIdAndApplicationIdNotAndStatusIn(
            Long classId, Long applicationId, Collection<TutorApplicationStatus> statuses);
}