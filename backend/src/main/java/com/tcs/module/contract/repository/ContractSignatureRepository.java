package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.ContractSignature;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {

    List<ContractSignature> findByContract_ContractId(Long contractId);

    boolean existsByContract_ContractIdAndSigner_UserId(Long contractId, Long signerUserId);
}
