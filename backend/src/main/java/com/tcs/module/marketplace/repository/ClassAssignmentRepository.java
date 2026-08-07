package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {

    List<ClassAssignment> findByApplication_TutoringClass_ClassIdAndStatus(
            Long classId, ClassAssignmentStatus status);

    List<ClassAssignment> findByTutor_TutorIdOrderByAssignedDateDesc(Long tutorId);

    List<ClassAssignment> findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(
            Long creatorUserId);

    Optional<ClassAssignment> findByApplication_ApplicationId(Long applicationId);

    Optional<ClassAssignment> findFirstByApplication_TutoringClass_ClassIdAndStatus(
            Long classId, ClassAssignmentStatus status);

    List<ClassAssignment> findByTutor_TutorIdAndStatus(Long tutorId, ClassAssignmentStatus status);

    Optional<ClassAssignment> findFirstByApplication_ApplicationId(Long applicationId);

    List<ClassAssignment> findByApplication_TutoringClass_Creator_UserId(Long creatorUserId);
}
