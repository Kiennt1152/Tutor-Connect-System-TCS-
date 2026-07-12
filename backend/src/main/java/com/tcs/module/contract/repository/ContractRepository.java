package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.Contract;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByAssignment_AssignmentId(Long assignmentId);

    Optional<Contract> findByClassStudent_ClassStudentId(Long classStudentId);

    @Query("SELECT DISTINCT c FROM Contract c WHERE " +
           "c.assignment IS NOT NULL AND c.assignment.tutor.user.userId = :userId")
    java.util.List<Contract> findByAssignment_Tutor_UserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Contract c WHERE " +
           "c.assignment IS NOT NULL AND c.assignment.application.tutoringClass.creator.userId = :userId")
    java.util.List<Contract> findByAssignment_ClassCreator_UserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Contract c WHERE " +
           "c.classStudent IS NOT NULL AND (c.classStudent.tutoringClass.creator.userId = :userId OR c.classStudent.enrolledByUser.userId = :userId)")
    java.util.List<Contract> findByClassStudent_UserId(@Param("userId") Long userId);
}
