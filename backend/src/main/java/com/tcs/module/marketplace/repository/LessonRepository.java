package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.Lesson;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    Optional<Lesson> findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(
            Long classId, Long slotId, Integer sequenceNo);
}
