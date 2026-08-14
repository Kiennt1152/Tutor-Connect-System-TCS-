package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByReferenceCode(String referenceCode);

    Optional<PaymentTransaction> findByExternalTransactionId(String externalTransactionId);

    List<PaymentTransaction> findByTypeAndStatusAndAmount(
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            BigDecimal amount);

    List<PaymentTransaction> findByTypeAndStatusAndCreatedAtBefore(
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            LocalDateTime createdAt);

    List<PaymentTransaction> findByWallet_WalletIdAndTypeAndStatusAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long walletId,
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            BigDecimal amount,
            LocalDateTime from,
            LocalDateTime to);

    List<PaymentTransaction> findByWallet_WalletIdAndTypeAndAmountAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long walletId,
            PaymentTransactionType type,
            BigDecimal amount,
            LocalDateTime from,
            LocalDateTime to);

    List<PaymentTransaction> findByWallet_WalletIdAndTypeAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long walletId,
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            LocalDateTime from,
            LocalDateTime to);

    List<PaymentTransaction> findByWallet_WalletIdAndTypeInAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long walletId,
            Collection<PaymentTransactionType> types,
            PaymentTransactionStatus status,
            LocalDateTime from,
            LocalDateTime to);

    Page<PaymentTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Page<PaymentTransaction> findByWallet_WalletIdAndTypeOrderByCreatedAtDesc(
            Long walletId, PaymentTransactionType type, Pageable pageable);

    Page<PaymentTransaction> findByWallet_WalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long walletId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<PaymentTransaction> findByWallet_WalletIdAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long walletId, PaymentTransactionType type, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.wallet.walletId = :walletId " +
            "AND (:type IS NULL OR pt.type = :type) " +
            "AND (:from IS NULL OR pt.createdAt >= :from) " +
            "AND (:to IS NULL OR pt.createdAt <= :to) " +
            "ORDER BY pt.createdAt DESC")
    Page<PaymentTransaction> findByWalletIdWithFilters(
            @Param("walletId") Long walletId,
            @Param("type") PaymentTransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt " +
           "WHERE pt.status = :status AND pt.type = :type " +
           "AND (:from IS NULL OR pt.createdAt >= :from) " +
           "AND (:to IS NULL OR pt.createdAt <= :to)")
    BigDecimal sumAmountByStatusAndTypeAndCreatedAtBetween(
            @Param("status") PaymentTransactionStatus status,
            @Param("type") PaymentTransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt " +
           "WHERE pt.status = :status AND pt.type IN :types " +
           "AND (:from IS NULL OR pt.createdAt >= :from) " +
           "AND (:to IS NULL OR pt.createdAt <= :to)")
    BigDecimal sumAmountByStatusAndTypeInAndCreatedAtBetween(
            @Param("status") PaymentTransactionStatus status,
            @Param("types") Collection<PaymentTransactionType> types,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<PaymentTransaction> findByStatusAndCreatedAtBetween(
            PaymentTransactionStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt " +
           "WHERE pt.status = :status AND pt.type = :type " +
           "AND (:from IS NULL OR pt.createdAt >= :from) " +
           "AND (:to IS NULL OR pt.createdAt <= :to)")
    long countByStatusAndTypeAndDateRange(
            @Param("status") PaymentTransactionStatus status,
            @Param("type") PaymentTransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<PaymentTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<PaymentTransaction> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, org.springframework.data.domain.Pageable pageable);

    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusAndTypeAndCreatedAtBetween(
            PaymentTransactionStatus status, PaymentTransactionType type, LocalDateTime from, LocalDateTime to);
}
