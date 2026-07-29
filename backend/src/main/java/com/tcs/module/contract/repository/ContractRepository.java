package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    long countByStatus(ContractStatus status);
}
