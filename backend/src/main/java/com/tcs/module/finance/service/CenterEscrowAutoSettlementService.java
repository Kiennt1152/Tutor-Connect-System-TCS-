package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonAttendance;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CenterEscrowAutoSettlementService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String AUTO_RELEASE_REASON =
            "Tự động giải ngân học phí lớp center hoàn thành, không có khiếu nại đang xử lý";

    private final TutoringClassRepository tutoringClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final DisputeRepository disputeRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final ClassTerminationRequestRepository classTerminationRequestRepository;
    private final ReportRepository reportRepository;
    private final EscrowService escrowService;

    @Transactional
    public boolean trySettleCompletedCenterClass(Long classId) {
        if (classId == null) {
            return false;
        }
        TutoringClass tutoringClass = tutoringClassRepository.findById(classId).orElse(null);
        if (!isCenterClassReadyForAutoSettlement(tutoringClass)) {
            return false;
        }

        List<ClassStudent> enrolledStudents = classStudentRepository
                .findByTutoringClass_ClassIdAndStatus(classId, ClassStudentStatus.ENROLLED);
        if (enrolledStudents.isEmpty() || !hasCompletedRequiredSessions(tutoringClass, enrolledStudents)) {
            return false;
        }

        List<EscrowTransaction> escrows = escrowTransactionRepository
                .findByClassStudent_TutoringClass_ClassId(classId);
        Map<Long, EscrowTransaction> escrowByStudent = escrows.stream()
                .filter(escrow -> escrow.getClassStudent() != null
                        && escrow.getClassStudent().getClassStudentId() != null)
                .collect(Collectors.toMap(
                        escrow -> escrow.getClassStudent().getClassStudentId(),
                        escrow -> escrow,
                        (first, ignored) -> first));

        if (hasBlockingClassIssue(tutoringClass, enrolledStudents, escrowByStudent)) {
            return false;
        }

        boolean releasedAny = false;
        boolean allStudentEscrowsSettled = true;
        for (ClassStudent student : enrolledStudents) {
            EscrowTransaction escrow = escrowByStudent.get(student.getClassStudentId());
            if (escrow == null) {
                return false;
            }
            if (escrow.getStatus() == EscrowStatus.FUNDED) {
                escrowService.apply(new ReleaseInstruction(
                        escrow.getEscrowId(),
                        amountOrZero(escrow.getAmount()),
                        ZERO,
                        AUTO_RELEASE_REASON));
                releasedAny = true;
            } else if (escrow.getStatus() != EscrowStatus.RELEASED
                    && escrow.getStatus() != EscrowStatus.REFUNDED) {
                allStudentEscrowsSettled = false;
            }
        }

        if ((releasedAny || allStudentEscrowsSettled)
                && tutoringClass.getStatus() != TutoringClassStatus.COMPLETED) {
            tutoringClass.setStatus(TutoringClassStatus.COMPLETED);
            tutoringClassRepository.save(tutoringClass);
        }
        return releasedAny;
    }

    private boolean isCenterClassReadyForAutoSettlement(TutoringClass tutoringClass) {
        if (tutoringClass == null
                || tutoringClass.getClassId() == null
                || tutoringClass.getClassType() != ClassType.CENTER) {
            return false;
        }
        TutoringClassStatus status = tutoringClass.getStatus();
        return status == TutoringClassStatus.IN_PROGRESS
                || status == TutoringClassStatus.MATCHED
                || status == TutoringClassStatus.ENROLLMENT_CLOSED
                || status == TutoringClassStatus.COMPLETED;
    }

    private boolean hasCompletedRequiredSessions(
            TutoringClass tutoringClass,
            List<ClassStudent> enrolledStudents) {
        int requiredSessions = tutoringClass.getNumberOfSessions() != null
                ? tutoringClass.getNumberOfSessions()
                : 1;
        if (requiredSessions <= 0) {
            return false;
        }

        List<Lesson> lessons = lessonRepository
                .findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(tutoringClass.getClassId());
        if (lessons.isEmpty()) {
            return false;
        }

        Set<Long> enrolledStudentIds = enrolledStudents.stream()
                .map(ClassStudent::getClassStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (enrolledStudentIds.isEmpty()) {
            return false;
        }

        List<Long> lessonIds = lessons.stream()
                .map(Lesson::getLessonId)
                .filter(Objects::nonNull)
                .toList();
        if (lessonIds.isEmpty()) {
            return false;
        }

        Map<Long, Set<Long>> attendedStudentsByLesson = new HashMap<>();
        for (LessonAttendance attendance : lessonAttendanceRepository.findByLesson_LessonIdIn(lessonIds)) {
            if (attendance.getLesson() == null
                    || attendance.getLesson().getLessonId() == null
                    || attendance.getClassStudent() == null
                    || attendance.getClassStudent().getClassStudentId() == null) {
                continue;
            }
            Long studentId = attendance.getClassStudent().getClassStudentId();
            if (!enrolledStudentIds.contains(studentId)) {
                continue;
            }
            attendedStudentsByLesson
                    .computeIfAbsent(attendance.getLesson().getLessonId(), ignored -> new HashSet<>())
                    .add(studentId);
        }

        long completedSessions = lessons.stream()
                .map(Lesson::getLessonId)
                .filter(Objects::nonNull)
                .filter(lessonId -> attendedStudentsByLesson
                        .getOrDefault(lessonId, Collections.emptySet())
                        .containsAll(enrolledStudentIds))
                .count();
        return completedSessions >= requiredSessions;
    }

    private boolean hasBlockingClassIssue(
            TutoringClass tutoringClass,
            List<ClassStudent> enrolledStudents,
            Map<Long, EscrowTransaction> escrowByStudent) {
        if (tutoringClass.getStatus() == TutoringClassStatus.DISPUTED
                || tutoringClass.getStatus() == TutoringClassStatus.CANCELLED) {
            return true;
        }
        if (reportRepository.existsByTargetTypeAndTargetIdAndStatus(
                ReportTargetType.CLASS,
                tutoringClass.getClassId(),
                ReportStatus.PENDING)) {
            return true;
        }

        for (ClassStudent student : enrolledStudents) {
            if (hasBlockingTermination(student)) {
                return true;
            }
            EscrowTransaction escrow = escrowByStudent.get(student.getClassStudentId());
            if (hasBlockingEscrowIssue(escrow)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBlockingTermination(ClassStudent student) {
        Long classStudentId = student.getClassStudentId();
        return classStudentId != null
                && (classTerminationRequestRepository.existsByClassStudent_ClassStudentIdAndStatus(
                        classStudentId,
                        ClassTerminationStatus.PENDING)
                || classTerminationRequestRepository.existsByClassStudent_ClassStudentIdAndStatus(
                        classStudentId,
                        ClassTerminationStatus.APPROVED));
    }

    private boolean hasBlockingEscrowIssue(EscrowTransaction escrow) {
        if (escrow == null || escrow.getEscrowId() == null) {
            return true;
        }
        if (escrow.getStatus() == EscrowStatus.DISPUTED || escrow.getStatus() == EscrowStatus.ON_HOLD) {
            return true;
        }
        Long escrowId = escrow.getEscrowId();
        return disputeRepository.existsByEscrowTransaction_EscrowIdAndStatusNot(escrowId, DisputeStatus.RESOLVED)
                || refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                        escrowId,
                        RefundRequestStatus.PENDING)
                || refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(
                        escrowId,
                        RefundRequestStatus.APPROVED);
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : ZERO;
    }
}
