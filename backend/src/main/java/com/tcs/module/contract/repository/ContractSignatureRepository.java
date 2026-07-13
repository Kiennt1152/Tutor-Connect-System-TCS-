package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.enums.PartyRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {

    @Query("SELECT cs FROM ContractSignature cs WHERE cs.contract.contractId = :contractId")
    List<ContractSignature> findByContractId(@Param("contractId") Long contractId);

    @Query("SELECT cs FROM ContractSignature cs WHERE cs.contract.contractId = :contractId AND cs.partyRole = :partyRole")
    Optional<ContractSignature> findByContractIdAndPartyRole(
            @Param("contractId") Long contractId,
            @Param("partyRole") PartyRole partyRole);

    @Query("SELECT cs FROM ContractSignature cs WHERE cs.contract.contractId = :contractId AND cs.signer.userId = :userId")
    Optional<ContractSignature> findByContractIdAndSignerId(
            @Param("contractId") Long contractId,
            @Param("userId") Long userId);

    @Query("SELECT COUNT(cs) FROM ContractSignature cs WHERE cs.contract.contractId = :contractId AND cs.signatureStatus = 'SIGNED'")
    int countSignedByContractId(@Param("contractId") Long contractId);
}
