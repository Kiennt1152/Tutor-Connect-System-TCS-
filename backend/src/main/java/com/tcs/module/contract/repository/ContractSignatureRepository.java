package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.ContractSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {
}
