package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorEducationRepository extends JpaRepository<TutorEducation, Long> {
}
