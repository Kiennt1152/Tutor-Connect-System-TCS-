package com.tcs.verification.repository;

import com.tcs.verification.entity.VerificationHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationHistoryRepository extends JpaRepository<VerificationHistory, UUID> {
}
