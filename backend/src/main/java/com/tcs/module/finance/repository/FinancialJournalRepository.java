package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.FinancialJournal;
import com.tcs.module.finance.enums.JournalEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FinancialJournalRepository extends JpaRepository<FinancialJournal, Long> {

    Page<FinancialJournal> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Page<FinancialJournal> findByWallet_WalletIdAndEntryTypeOrderByCreatedAtDesc(
            Long walletId, JournalEntryType entryType, Pageable pageable);

    Page<FinancialJournal> findByWallet_WalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long walletId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<FinancialJournal> findByWallet_WalletIdAndEntryTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long walletId, JournalEntryType entryType, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT fj FROM FinancialJournal fj WHERE fj.wallet.walletId = :walletId " +
            "AND (:entryType IS NULL OR fj.entryType = :entryType) " +
            "AND (:from IS NULL OR fj.createdAt >= :from) " +
            "AND (:to IS NULL OR fj.createdAt <= :to) " +
            "ORDER BY fj.createdAt DESC")
    Page<FinancialJournal> findByWalletIdWithFilters(
            @Param("walletId") Long walletId,
            @Param("entryType") JournalEntryType entryType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
