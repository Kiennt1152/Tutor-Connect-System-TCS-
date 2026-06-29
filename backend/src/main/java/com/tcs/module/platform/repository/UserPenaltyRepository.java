package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.UserPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {
}
