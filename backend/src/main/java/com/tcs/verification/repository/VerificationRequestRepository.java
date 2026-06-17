package com.tcs.verification.repository;

import com.tcs.verification.entity.VerificationRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {
}
