package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.enums.RefundRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(Long escrowId);

    Optional<RefundRequest> findByRefundReferenceCode(String refundReferenceCode);

    boolean existsByEscrowTransaction_EscrowIdAndRequestedBy_UserIdAndStatus(
            Long escrowId,
            Long requestedByUserId,
            RefundRequestStatus status);

    boolean existsByEscrowTransaction_EscrowIdAndStatus(Long escrowId, RefundRequestStatus status);

    List<RefundRequest> findAllByOrderByRequestedAtDesc();

    List<RefundRequest> findByStatusOrderByRequestedAtDesc(RefundRequestStatus status);
}
