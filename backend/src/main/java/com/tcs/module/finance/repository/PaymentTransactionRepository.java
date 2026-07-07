package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.PaymentTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByReferenceCode(String referenceCode);

    boolean existsByExternalTransactionId(String externalTransactionId);

    List<PaymentTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId);
}
