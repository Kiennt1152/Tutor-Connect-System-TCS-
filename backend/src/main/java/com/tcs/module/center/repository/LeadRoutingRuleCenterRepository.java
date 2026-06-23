package com.tcs.module.center.repository;

import com.tcs.module.center.entity.LeadRoutingRuleCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRoutingRuleCenterRepository extends JpaRepository<LeadRoutingRuleCenter, Long> {
}
