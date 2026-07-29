package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    long countByStatus(com.tcs.module.finance.enums.WithdrawalRequestStatus status);
    java.util.List<com.tcs.module.finance.entity.WithdrawalRequest> findByStatusOrderByRequestedAtAsc(com.tcs.module.finance.enums.WithdrawalRequestStatus status);
}
