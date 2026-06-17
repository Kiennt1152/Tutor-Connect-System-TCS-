package com.tcs.profile.repository;

import com.tcs.profile.entity.PlatformAdmin;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, UUID> {
}
