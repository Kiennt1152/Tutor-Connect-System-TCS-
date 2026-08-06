package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.enums.DisputeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByReport_ReportId(Long reportId);

    List<Dispute> findByStatus(DisputeStatus status, Sort sort);

    boolean existsByEscrowTransaction_EscrowIdAndStatusNot(Long escrowId, DisputeStatus status);

    long countByStatusIn(List<DisputeStatus> statuses);

    List<Dispute> findByStatusInOrderByCreatedAtAsc(List<DisputeStatus> statuses);
}
