package com.tcs.module.center.repository;

import com.tcs.module.center.entity.RecruitmentApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {

    /** Đơn ứng tuyển của một tin, mới nộp trước. */
    List<RecruitmentApplication> findByRecruitmentPost_RecruitmentIdOrderByAppliedAtDesc(Long recruitmentId);

    /** Đơn của một gia sư, mới nộp trước. */
    List<RecruitmentApplication> findByTutor_TutorIdOrderByAppliedAtDesc(Long tutorId);

    /** Đơn của một gia sư vào các tin của MỘT trung tâm (để hiển thị ở trang quản lý gia sư). */
    List<RecruitmentApplication>
            findByTutor_TutorIdAndRecruitmentPost_Center_CenterIdOrderByAppliedAtDesc(
                    Long tutorId, Long centerId);

    /** Dùng để chặn nộp trùng đơn cho cùng một tin. */
    Optional<RecruitmentApplication> findFirstByRecruitmentPost_RecruitmentIdAndTutor_TutorId(
            Long recruitmentId, Long tutorId);

    long countByRecruitmentPost_RecruitmentId(Long recruitmentId);
}
