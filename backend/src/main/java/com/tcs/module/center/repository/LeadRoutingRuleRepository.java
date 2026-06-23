package com.tcs.module.center.repository;

import com.tcs.module.center.entity.LeadRoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRoutingRuleRepository extends JpaRepository<LeadRoutingRule, Long> {
}
