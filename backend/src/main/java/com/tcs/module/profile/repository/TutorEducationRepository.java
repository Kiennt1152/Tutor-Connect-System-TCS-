package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorEducation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorEducationRepository extends JpaRepository<TutorEducation, Long> {

    List<TutorEducation> findByTutor_TutorId(Long tutorId);
}
