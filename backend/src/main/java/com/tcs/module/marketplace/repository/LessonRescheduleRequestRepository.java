package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.LessonRescheduleRequest;
import com.tcs.module.marketplace.enums.RescheduleRequestStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRescheduleRequestRepository extends JpaRepository<LessonRescheduleRequest, Long> {

    /** Mọi yêu cầu thuộc các lớp mà người đang đăng nhập tham gia (mới nhất trước). */
    List<LessonRescheduleRequest> findByTutoringClass_ClassIdInOrderByCreatedAtDesc(Collection<Long> classIds);

    /** Chặn gửi trùng: một buổi chỉ có tối đa một yêu cầu đang chờ duyệt. */
    boolean existsByLesson_LessonIdAndStatus(Long lessonId, RescheduleRequestStatus status);
}
