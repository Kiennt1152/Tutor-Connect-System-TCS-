package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorCertificate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorCertificateRepository extends JpaRepository<TutorCertificate, Long> {

    List<TutorCertificate> findByTutor_TutorId(Long tutorId);
}
