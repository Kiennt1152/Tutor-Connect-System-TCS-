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

    Optional<Contract> findByAssignment_AssignmentId(Long assignmentId);

    Optional<Contract> findByClassStudent_ClassStudentId(Long classStudentId);

    @Query("SELECT c FROM Contract c WHERE c.assignment.assignmentId = :assignmentId")
    Optional<Contract> findByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Query("SELECT c FROM Contract c WHERE c.status = :status")
    List<Contract> findByStatus(@Param("status") ContractStatus status);

    @Query("SELECT COUNT(c) FROM Contract c WHERE FUNCTION('DATE', c.createdAt) = FUNCTION('DATE', CURRENT_DATE)")
    long countTodayContracts();

    long countByStatus(ContractStatus status);

    @Query("SELECT c FROM Contract c LEFT JOIN c.assignment a LEFT JOIN a.application app LEFT JOIN app.tutoringClass tc WHERE (app.tutor.user.userId = :userId) OR (tc.creator.userId = :userId)")
    List<Contract> findContractsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Contract c WHERE "
            + "c.assignment IS NOT NULL AND c.assignment.tutor.user.userId = :userId")
    List<Contract> findByAssignment_Tutor_UserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Contract c WHERE "
            + "c.assignment IS NOT NULL AND c.assignment.application.tutoringClass.creator.userId = :userId")
    List<Contract> findByAssignment_ClassCreator_UserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Contract c WHERE "
            + "c.classStudent IS NOT NULL AND "
            + "(c.classStudent.tutoringClass.creator.userId = :userId "
            + "OR c.classStudent.enrolledByUser.userId = :userId)")
    List<Contract> findByClassStudent_UserId(@Param("userId") Long userId);
}
