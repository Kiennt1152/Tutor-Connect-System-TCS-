package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    long countByStatus(WithdrawalRequestStatus status);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(WithdrawalRequestStatus status);

    List<WithdrawalRequest> findByStatusAndRequestedAtBeforeOrderByRequestedAtAsc(
            WithdrawalRequestStatus status,
            LocalDateTime requestedAt);

    List<WithdrawalRequest> findByWallet_WalletIdAndStatusAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
            Long walletId,
            WithdrawalRequestStatus status,
            BigDecimal amount,
            LocalDateTime from,
            LocalDateTime to);

    List<WithdrawalRequest> findByWallet_WalletIdAndAmountAndRequestedAtBetweenOrderByRequestedAtAsc(
            Long walletId,
            BigDecimal amount,
            LocalDateTime from,
            LocalDateTime to);

    @Query(
            value = """
                    SELECT wr FROM WithdrawalRequest wr
                    JOIN FETCH wr.wallet w
                    JOIN FETCH w.user u
                    JOIN FETCH wr.paymentMethod pm
                    WHERE (:status IS NULL OR wr.status = :status)
                    ORDER BY wr.requestedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(wr) FROM WithdrawalRequest wr
                    WHERE (:status IS NULL OR wr.status = :status)
                    """)
    Page<WithdrawalRequest> findAdminPage(
            @Param("status") WithdrawalRequestStatus status,
            Pageable pageable);

    @Query("""
            SELECT wr FROM WithdrawalRequest wr
            JOIN FETCH wr.wallet w
            JOIN FETCH w.user u
            JOIN FETCH wr.paymentMethod pm
            WHERE (:status IS NULL OR wr.status = :status)
            """)
    List<WithdrawalRequest> findAdminList(@Param("status") WithdrawalRequestStatus status);
}
