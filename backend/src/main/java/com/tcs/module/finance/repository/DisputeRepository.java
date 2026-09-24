package com.tcs.module.finance.repository;

import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.enums.DisputeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dispute d where d.disputeId = :id")
    Optional<Dispute> findForUpdate(@Param("id") Long id);

    @Query("""
            select d from Dispute d join d.report r join d.escrowTransaction e
            left join e.assignment a left join a.application app left join app.tutoringClass pc
            left join a.tutor tutor left join tutor.user tutorUser
            left join e.classStudent cs left join cs.tutoringClass cc left join cc.center center
            where (:classId is null or pc.classId = :classId or cc.classId = :classId)
            and (r.reporter.userId = :userId or tutorUser.userId = :userId
                or pc.creator.userId = :userId or cs.enrolledByUser.userId = :userId
                or cc.creator.userId = :userId or center.user.userId = :userId
                or exists (select ca.assignmentId from ClassAssignment ca
                    where ca.application.tutoringClass.classId = cc.classId
                    and ca.tutor.user.userId = :userId and ca.status = 'ACTIVE'))
            order by d.createdAt desc
            """)
    List<Dispute> findForParticipant(@Param("classId") Long classId, @Param("userId") Long userId);

    @Query("""
            select count(d) > 0 from Dispute d
            where d.report.targetType = 'CLASS' and d.report.targetId = :classId
            and d.status <> 'RESOLVED' and d.disputeId <> :excludedId
            """)
    boolean existsOtherOpenForClass(@Param("classId") Long classId, @Param("excludedId") Long excludedId);

    Optional<Dispute> findByReport_ReportId(Long reportId);

    List<Dispute> findByStatus(DisputeStatus status, Sort sort);

    boolean existsByEscrowTransaction_EscrowIdAndStatusNot(Long escrowId, DisputeStatus status);

    long countByStatusIn(List<DisputeStatus> statuses);

    List<Dispute> findByStatusInOrderByCreatedAtAsc(List<DisputeStatus> statuses);
}
