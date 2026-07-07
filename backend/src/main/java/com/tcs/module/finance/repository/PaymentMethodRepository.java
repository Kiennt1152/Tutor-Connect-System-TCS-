package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.PaymentMethod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByWallet_WalletId(Long walletId);

    List<PaymentMethod> findByWallet_WalletIdAndStatus(Long walletId, String status);
}
