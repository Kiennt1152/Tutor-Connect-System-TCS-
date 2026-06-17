package com.tcs.payment.repository;

import com.tcs.payment.entity.EscrowTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, UUID> {
}
