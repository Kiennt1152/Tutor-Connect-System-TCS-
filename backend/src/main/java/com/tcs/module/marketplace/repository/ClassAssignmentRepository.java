package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {

    /** Lời mời + lớp đang dạy của một gia sư (mới nhất trước). */
    List<ClassAssignment> findByTutor_TutorIdOrderByAssignedDateDesc(Long tutorId);

    /** Phân công trên các lớp do một Client tạo — để Client biết ai đang dạy lớp mình. */
    List<ClassAssignment> findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(
            Long creatorUserId);

    /** Mỗi đơn ứng tuyển chỉ sinh ra tối đa 1 phân công (uq_class_assignments_application). */
    Optional<ClassAssignment> findByApplication_ApplicationId(Long applicationId);

    // ----- Method cac module khac (center/tutor/contract) van dung; khoi phuc sau merge. -----

    Optional<ClassAssignment> findFirstByApplication_TutoringClass_ClassIdAndStatus(
            Long classId, ClassAssignmentStatus status);

    List<ClassAssignment> findByTutor_TutorIdAndStatus(Long tutorId, ClassAssignmentStatus status);

    Optional<ClassAssignment> findFirstByApplication_ApplicationId(Long applicationId);

    /** Phan cong theo lop do mot Client tao (khong sap xep) — dung cho luong danh gia UC-65. */
    List<ClassAssignment> findByApplication_TutoringClass_Creator_UserId(Long creatorUserId);
}
