package com.tcs.profile.repository;

import com.tcs.profile.entity.TutorCenter;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorCenterRepository extends JpaRepository<TutorCenter, UUID> {
}
