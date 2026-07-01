package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    java.util.List<PaymentMethod> findByWallet_WalletId(Long walletId);
}
