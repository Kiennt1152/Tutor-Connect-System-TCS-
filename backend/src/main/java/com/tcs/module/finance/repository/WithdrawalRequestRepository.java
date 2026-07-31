package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    List<WithdrawalRequest> findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
            WithdrawalRequestStatus status,
            LocalDateTime requestedAt);

    List<WithdrawalRequest> findByWallet_WalletIdAndStatusAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
            Long walletId,
            WithdrawalRequestStatus status,
            BigDecimal amount,
            LocalDateTime from,
            LocalDateTime to);
}
