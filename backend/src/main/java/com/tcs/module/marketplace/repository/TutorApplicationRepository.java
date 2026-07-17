package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutorApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorApplicationRepository extends JpaRepository<TutorApplication, Long> {

    List<TutorApplication> findByTutoringClass_ClassId(Long classId);

    long countByTutoringClass_ClassId(Long classId);

    /** Mỗi gia sư chỉ được nộp 1 đơn cho 1 lớp (ràng buộc uq_tutor_applications). */
    boolean existsByTutoringClass_ClassIdAndTutor_TutorId(Long classId, Long tutorId);

    List<TutorApplication> findByTutor_TutorId(Long tutorId);
}
