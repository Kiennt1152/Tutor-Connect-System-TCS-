package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.Subject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findFirstBySubjectNameIgnoreCase(String subjectName);
}
