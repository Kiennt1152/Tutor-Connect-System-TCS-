package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByTutoringClass_ClassId(Long classId);

    Optional<Lesson> findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(
            Long classId, Long slotId, int sequenceNo);

    List<Lesson> findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(Long tutorId);

    List<Lesson> findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(Long creatorUserId);

    long countByTutoringClass_ClassId(Long classId);

    List<Lesson> findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(Long classId);

    List<Lesson> findByTutoringClass_ClassIdAndLessonDateOrderBySequenceNoAsc(Long classId, java.time.LocalDate lessonDate);

    /** Buổi học đúng ngày truyền vào và chưa gửi nhắc nhở -> cần gửi thông báo. */
    List<Lesson> findByLessonDateAndReminderSentAtIsNull(java.time.LocalDate lessonDate);
}
