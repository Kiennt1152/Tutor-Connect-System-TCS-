package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.Lesson;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** Lịch dạy của gia sư, xếp theo thứ tự buổi diễn ra. */
    List<Lesson> findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(Long tutorId);

    /** Lịch học của Client — mọi buổi thuộc các lớp do người này tạo. */
    List<Lesson> findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(Long creatorUserId);

    long countByTutoringClass_ClassId(Long classId);
}
