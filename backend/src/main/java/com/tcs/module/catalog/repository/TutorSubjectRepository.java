package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.TutorSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorSubjectRepository extends JpaRepository<TutorSubject, Long> {

    boolean existsByCategory_CategoryId(Long categoryId);
}
