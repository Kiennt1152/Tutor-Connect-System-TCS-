package com.tcs.marketplace.repository;

import com.tcs.marketplace.entity.Lesson;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
}
