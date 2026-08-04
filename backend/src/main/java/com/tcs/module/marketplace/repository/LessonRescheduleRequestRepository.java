package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.LessonRescheduleRequest;
import com.tcs.module.marketplace.enums.RescheduleRequestStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRescheduleRequestRepository extends JpaRepository<LessonRescheduleRequest, Long> {

    List<LessonRescheduleRequest> findByTutoringClass_ClassIdInOrderByCreatedAtDesc(Collection<Long> classIds);

    boolean existsByLesson_LessonIdAndStatus(Long lessonId, RescheduleRequestStatus status);
}
