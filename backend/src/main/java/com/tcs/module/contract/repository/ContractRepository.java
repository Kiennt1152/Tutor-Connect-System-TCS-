package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByContractNo(String contractNo);

    @Query("SELECT c FROM Contract c WHERE c.assignment.assignmentId = :assignmentId")
    Optional<Contract> findByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Query("SELECT c FROM Contract c WHERE c.status = :status")
    List<Contract> findByStatus(@Param("status") ContractStatus status);

    @Query("SELECT COUNT(c) FROM Contract c WHERE FUNCTION('DATE', c.createdAt) = FUNCTION('DATE', CURRENT_DATE)")
    long countTodayContracts();

    long countByStatus(ContractStatus status);
}
