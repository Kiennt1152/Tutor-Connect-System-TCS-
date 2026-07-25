package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** Tim buoi hoc theo lop + slot + so thu tu buoi; dung khi doi lich/them buoi (khoi phuc sau merge). */
    Optional<Lesson> findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(
            Long classId, Long slotId, int sequenceNo);

    /** Lịch dạy của gia sư, xếp theo thứ tự buổi diễn ra. */
    List<Lesson> findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(Long tutorId);

    /** Lịch học của Client — mọi buổi thuộc các lớp do người này tạo. */
    List<Lesson> findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(Long creatorUserId);

    long countByTutoringClass_ClassId(Long classId);

    /** Mọi buổi của một lớp — dùng khi đánh lại sequence_no sau khi đổi lịch/thêm buổi. */
    List<Lesson> findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(Long classId);
}
