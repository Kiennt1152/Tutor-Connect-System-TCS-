package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    boolean existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(Long classId, Long userId);

    /** Check trùng theo CHÍNH học sinh (email tài khoản đăng ký) — 2 con cùng phụ huynh vẫn đăng ký được. */
    boolean existsByTutoringClass_ClassIdAndStudentEmail(Long classId, String studentEmail);

    /** Đã có bất kỳ học viên nào đăng ký lớp (mọi trạng thái, kể cả chờ ký hợp đồng). */
    boolean existsByTutoringClass_ClassId(Long classId);

    long countByTutoringClass_ClassIdAndStatus(Long classId, ClassStudentStatus status);

    List<ClassStudent> findByTutoringClass_ClassIdAndStatus(Long classId, ClassStudentStatus status);

    boolean existsByChildProfile_ChildProfileId(Long childProfileId);
}
