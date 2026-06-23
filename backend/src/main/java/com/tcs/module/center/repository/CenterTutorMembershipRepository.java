package com.tcs.module.center.repository;

import com.tcs.module.center.entity.CenterTutorMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CenterTutorMembershipRepository extends JpaRepository<CenterTutorMembership, Long> {
}
