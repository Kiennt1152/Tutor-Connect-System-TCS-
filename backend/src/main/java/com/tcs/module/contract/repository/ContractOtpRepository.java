package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.ContractOtp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractOtpRepository extends JpaRepository<ContractOtp, Long> {

    Optional<ContractOtp> findFirstByContractIdAndSignerUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(
            Long contractId, Long signerUserId);

    @Modifying
    @Query("DELETE FROM ContractOtp o WHERE o.contractId = :contractId AND o.signerUserId = :signerUserId")
    void deleteByContractIdAndSignerUserId(
            @Param("contractId") Long contractId,
            @Param("signerUserId") Long signerUserId);

    @Modifying
    @Query("DELETE FROM ContractOtp o WHERE o.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
