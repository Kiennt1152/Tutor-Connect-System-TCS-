package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.PaymentReleaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentReleaseRequestRepository extends JpaRepository<PaymentReleaseRequest, Long> {
}
