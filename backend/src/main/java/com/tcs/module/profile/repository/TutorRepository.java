package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.Tutor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findByUser_UserId(Long userId);

    List<Tutor> findByUser_UserIdIn(Collection<Long> userIds);
}
