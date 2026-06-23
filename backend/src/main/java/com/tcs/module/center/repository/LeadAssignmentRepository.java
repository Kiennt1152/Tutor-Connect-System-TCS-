package com.tcs.module.center.repository;

import com.tcs.module.center.entity.LeadAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, Long> {
}
