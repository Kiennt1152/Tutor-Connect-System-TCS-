package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
}
