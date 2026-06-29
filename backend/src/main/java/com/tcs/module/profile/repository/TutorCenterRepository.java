package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorCenter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorCenterRepository extends JpaRepository<TutorCenter, Long> {

    Optional<TutorCenter> findByUser_UserId(Long userId);

    List<TutorCenter> findByUser_UserIdIn(Collection<Long> userIds);
}
