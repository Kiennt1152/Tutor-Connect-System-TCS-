package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    List<WithdrawalRequest> findByWallet_WalletIdOrderByRequestedAtDesc(Long walletId);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus status);

    List<WithdrawalRequest> findAllByOrderByRequestedAtDesc();
}
