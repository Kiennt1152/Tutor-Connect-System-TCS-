package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.enums.VerificationDocumentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {

    List<VerificationDocument> findByVerificationRequest_VerificationId(Long verificationId);

    boolean existsByVerificationRequest_VerificationIdAndDocumentType(Long verificationId, VerificationDocumentType documentType);
}
