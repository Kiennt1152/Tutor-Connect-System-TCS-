package com.tcs.tutorprofile.repository;

import com.tcs.tutorprofile.entity.TutorSubject;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorSubjectRepository extends JpaRepository<TutorSubject, UUID> {
}
