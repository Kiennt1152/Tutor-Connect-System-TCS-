package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    boolean existsByChildProfile_ChildProfileId(Long childProfileId);
}
