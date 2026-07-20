package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.Grade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findFirstByGradeNameIgnoreCase(String gradeName);
}
