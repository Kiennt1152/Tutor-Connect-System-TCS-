package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorExperienceRepository extends JpaRepository<TutorExperience, Long> {

    java.util.List<TutorExperience> findByTutor_TutorId(Long tutorId);
}
