package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    long countByStatusIn(java.util.List<com.tcs.module.finance.enums.DisputeStatus> statuses);
    java.util.List<com.tcs.module.finance.entity.Dispute> findByStatusInOrderByCreatedAtAsc(java.util.List<com.tcs.module.finance.enums.DisputeStatus> statuses);
}
