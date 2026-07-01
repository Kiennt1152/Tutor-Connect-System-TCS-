package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.PlatformAdmin;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {

    Optional<PlatformAdmin> findByUser_UserId(Long userId);

    List<PlatformAdmin> findByUser_UserIdIn(Collection<Long> userIds);
}
