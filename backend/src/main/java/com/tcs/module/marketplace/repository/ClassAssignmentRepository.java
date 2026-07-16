package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {

    List<ClassAssignment> findByApplication_TutoringClass_ClassIdAndStatus(
            Long classId, ClassAssignmentStatus status);
}
