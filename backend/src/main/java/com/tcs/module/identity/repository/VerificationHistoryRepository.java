package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.VerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationHistoryRepository extends JpaRepository<VerificationHistory, Long> {
}
