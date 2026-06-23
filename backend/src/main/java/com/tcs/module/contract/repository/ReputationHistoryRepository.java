package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.ReputationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReputationHistoryRepository extends JpaRepository<ReputationHistory, Long> {
}
