package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorCenterRepository extends JpaRepository<TutorCenter, Long> {
}
