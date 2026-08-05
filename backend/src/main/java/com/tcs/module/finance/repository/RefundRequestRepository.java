package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.enums.RefundRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    long countByStatus(RefundRequestStatus status);
    List<RefundRequest> findByStatusOrderByRequestedAtAsc(RefundRequestStatus status);
}
