package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByWallet_WalletId(Long walletId);

    List<PaymentMethod> findByWallet_WalletIdAndStatusOrderByLastUsedAtDescPaymentMethodIdAsc(Long walletId, String status);

    Optional<PaymentMethod> findByPaymentMethodIdAndWallet_WalletIdAndStatus(
            Long paymentMethodId,
            Long walletId,
            String status);

    Optional<PaymentMethod> findByWallet_WalletIdAndBankNameIgnoreCaseAndAccountNoAndStatus(
            Long walletId,
            String bankName,
            String accountNo,
            String status);
}
