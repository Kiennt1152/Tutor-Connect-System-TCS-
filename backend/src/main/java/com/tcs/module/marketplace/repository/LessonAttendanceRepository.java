package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.LessonAttendance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonAttendanceRepository extends JpaRepository<LessonAttendance, Long> {

    List<LessonAttendance> findByLesson_LessonIdIn(List<Long> lessonIds);
}