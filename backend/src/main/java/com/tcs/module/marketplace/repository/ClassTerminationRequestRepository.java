package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassTerminationRequestRepository extends JpaRepository<ClassTerminationRequest, Long> {

    boolean existsByAssignment_AssignmentIdAndStatus(Long assignmentId, ClassTerminationStatus status);
}
