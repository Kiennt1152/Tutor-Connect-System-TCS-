package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutorReplacementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorReplacementRequestRepository extends JpaRepository<TutorReplacementRequest, Long> {
}
