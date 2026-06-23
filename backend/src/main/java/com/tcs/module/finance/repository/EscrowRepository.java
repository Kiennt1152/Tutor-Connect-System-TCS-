package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.Escrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscrowRepository extends JpaRepository<Escrow, Long> {
}
