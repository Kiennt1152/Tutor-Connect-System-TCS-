package com.tcs.module.identity.repository;

import com.tcs.module.identity.entity.VerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationHistoryRepository extends JpaRepository<VerificationHistory, Long> {

    @Modifying
    @Query("DELETE FROM VerificationHistory h WHERE h.verificationRequest.verificationId = :verificationId")
    int deleteAllByVerificationId(@Param("verificationId") Long verificationId);
}