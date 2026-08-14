package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CenterRequestFeeHoldRepository extends JpaRepository<CenterRequestFeeHold, Long> {

    Optional<CenterRequestFeeHold> findByRequestId(String requestId);

    Optional<CenterRequestFeeHold> findByReferenceCode(String referenceCode);

    Optional<CenterRequestFeeHold> findByPaymentTransaction_TransactionId(Long transactionId);

    Optional<CenterRequestFeeHold> findFirstByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);

    Optional<CenterRequestFeeHold> findFirstByClassIdOrderByCreatedAtDesc(Long classId);

    List<CenterRequestFeeHold> findByStatusOrderByCreatedAtDesc(CenterRequestFeeStatus status);
}
