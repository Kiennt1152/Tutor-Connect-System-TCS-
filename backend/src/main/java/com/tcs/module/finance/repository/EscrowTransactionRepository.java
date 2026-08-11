package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.EscrowTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcs.module.finance.enums.EscrowStatus;
import java.time.LocalDateTime;

@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {

    Optional<EscrowTransaction> findByAssignment_AssignmentId(Long assignmentId);

    Optional<EscrowTransaction> findByClassStudent_ClassStudentId(Long classStudentId);

    Optional<EscrowTransaction> findByPayment_TransactionId(Long transactionId);

    List<EscrowTransaction> findByAssignment_Application_TutoringClass_ClassId(Long classId);

    List<EscrowTransaction> findByClassStudent_TutoringClass_ClassId(Long classId);

    @Query("SELECT e FROM EscrowTransaction e JOIN e.payment p JOIN p.wallet w JOIN w.user payer " +
           "LEFT JOIN e.assignment a LEFT JOIN a.tutor tutor LEFT JOIN tutor.user tutorUser " +
           "LEFT JOIN e.classStudent cs LEFT JOIN cs.tutoringClass tc LEFT JOIN tc.center center LEFT JOIN center.user centerUser " +
           "WHERE (:status IS NULL OR e.status = :status) " +
           "AND (:from IS NULL OR e.createdAt >= :from) AND (:to IS NULL OR e.createdAt < :to) " +
           "AND (:reference IS NULL OR LOWER(p.referenceCode) LIKE LOWER(CONCAT('%', :reference, '%'))) " +
           "AND (:payer IS NULL OR LOWER(payer.email) LIKE LOWER(CONCAT('%', :payer, '%'))) " +
           "AND (:beneficiary IS NULL OR LOWER(tutorUser.email) LIKE LOWER(CONCAT('%', :beneficiary, '%')) " +
           "OR LOWER(centerUser.email) LIKE LOWER(CONCAT('%', :beneficiary, '%'))) ORDER BY e.createdAt DESC")
    Page<EscrowTransaction> searchAdmin(@Param("status") EscrowStatus status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("reference") String reference, @Param("payer") String payer,
            @Param("beneficiary") String beneficiary, Pageable pageable);
}
