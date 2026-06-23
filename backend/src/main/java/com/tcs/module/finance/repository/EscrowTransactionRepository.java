package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.EscrowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {
}
