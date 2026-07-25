package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {

    Optional<ClassAssignment> findFirstByApplication_TutoringClass_ClassIdAndStatus(
            Long classId, ClassAssignmentStatus status);

    List<ClassAssignment> findByTutor_TutorIdAndStatus(Long tutorId, ClassAssignmentStatus status);

    Optional<ClassAssignment> findFirstByApplication_ApplicationId(Long applicationId);

    // Cac phan cong thuoc lop do mot khach hang (creator) tao ra -> dung cho luong khach danh gia gia su.
    List<ClassAssignment> findByApplication_TutoringClass_Creator_UserId(Long userId);
}
