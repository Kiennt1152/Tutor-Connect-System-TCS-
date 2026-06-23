package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.FinancialJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialJournalRepository extends JpaRepository<FinancialJournal, Long> {
}
