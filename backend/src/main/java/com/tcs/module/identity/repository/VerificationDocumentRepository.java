package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {
}
