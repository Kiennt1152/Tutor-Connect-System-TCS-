package com.tcs.verification.repository;

import com.tcs.verification.entity.VerificationDocument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, UUID> {
}
