package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.LessonAttendance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonAttendanceRepository extends JpaRepository<LessonAttendance, Long> {

    List<LessonAttendance> findByLesson_LessonId(Long lessonId);

    Optional<LessonAttendance> findFirstByLesson_LessonIdAndClassStudent_ClassStudentId(
            Long lessonId, Long classStudentId);
}
