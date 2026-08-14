package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.enums.VerificationDocumentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {

    List<VerificationDocument> findByVerificationRequest_VerificationId(Long verificationId);

    List<VerificationDocument> findByVerificationRequest_VerificationIdOrderByDocumentIdAsc(Long verificationId);

    boolean existsByVerificationRequest_VerificationIdAndDocumentType(Long verificationId, VerificationDocumentType documentType);

    /**
     * File có phải là một chứng chỉ (CERTIFICATE) thuộc hồ sơ đã VERIFIED không.
     * Dùng để cho phép trung tâm/phụ huynh xem chứng chỉ của gia sư mà KHÔNG lộ
     * CCCD hay tài liệu chưa duyệt: endpoint chỉ phục vụ file thoả điều kiện này.
     */
    boolean existsByFile_FileIdAndDocumentTypeAndVerificationRequest_Status(
            Long fileId,
            VerificationDocumentType documentType,
            com.tcs.module.identity.enums.VerificationStatus status);

    long deleteByVerificationRequest_VerificationId(Long verificationId);

    @Modifying
    @Query("DELETE FROM VerificationDocument d WHERE d.verificationRequest.verificationId = :verificationId")
    int deleteAllByVerificationId(@Param("verificationId") Long verificationId);
}
