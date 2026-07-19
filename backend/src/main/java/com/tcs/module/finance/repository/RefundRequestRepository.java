package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.RefundRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(Long escrowId);
}
