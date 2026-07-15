package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.EscrowTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {

    Optional<EscrowTransaction> findByAssignment_AssignmentId(Long assignmentId);

    Optional<EscrowTransaction> findByClassStudent_ClassStudentId(Long classStudentId);

    List<EscrowTransaction> findByAssignment_Application_TutoringClass_ClassId(Long classId);

    List<EscrowTransaction> findByClassStudent_TutoringClass_ClassId(Long classId);
}
