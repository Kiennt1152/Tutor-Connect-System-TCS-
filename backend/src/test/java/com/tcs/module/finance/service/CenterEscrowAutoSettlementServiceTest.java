package com.tcs.module.finance.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonAttendance;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CenterEscrowAutoSettlementServiceTest {

    private static final Long CLASS_ID = 10L;
    private static final Long CLASS_STUDENT_ID = 20L;
    private static final Long ESCROW_ID = 30L;

    @Mock
    private TutoringClassRepository tutoringClassRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonAttendanceRepository lessonAttendanceRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private ClassTerminationRequestRepository classTerminationRequestRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private EscrowService escrowService;

    @InjectMocks
    private CenterEscrowAutoSettlementService service;

    @Test
    void releasesFundedCenterEscrowWhenClassCompletedWithoutIssue() {
        TutoringClass tutoringClass = centerClass();
        ClassStudent student = enrolledStudent(tutoringClass);
        Lesson lesson = lesson(40L, tutoringClass);
        LessonAttendance attendance = attendance(lesson, student);
        EscrowTransaction escrow = fundedEscrow(student);

        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of(student));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(lesson));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(anyList()))
                .thenReturn(List.of(attendance));
        when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(CLASS_ID))
                .thenReturn(List.of(escrow));

        boolean released = service.trySettleCompletedCenterClass(CLASS_ID);

        assertTrue(released);
        ArgumentCaptor<ReleaseInstruction> captor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(captor.capture());
        ReleaseInstruction instruction = captor.getValue();
        assertTrue(ESCROW_ID.equals(instruction.escrowId()));
        assertTrue(new BigDecimal("100000.00").compareTo(instruction.releaseToBeneficiary()) == 0);
        assertTrue(BigDecimal.ZERO.compareTo(instruction.refundToPayer()) == 0);
        assertTrue(instruction.reason().contains("Tự động giải ngân"));
        assertTrue(tutoringClass.getStatus() == TutoringClassStatus.COMPLETED);
        verify(tutoringClassRepository).save(tutoringClass);
    }

    @Test
    void skipsAutoReleaseWhenClassHasPendingReport() {
        TutoringClass tutoringClass = centerClass();
        ClassStudent student = enrolledStudent(tutoringClass);
        Lesson lesson = lesson(40L, tutoringClass);
        LessonAttendance attendance = attendance(lesson, student);
        EscrowTransaction escrow = fundedEscrow(student);

        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of(student));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(lesson));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(anyList()))
                .thenReturn(List.of(attendance));
        when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(CLASS_ID))
                .thenReturn(List.of(escrow));
        when(reportRepository.existsByTargetTypeAndTargetIdAndStatus(
                ReportTargetType.CLASS,
                CLASS_ID,
                ReportStatus.PENDING))
                .thenReturn(true);

        boolean released = service.trySettleCompletedCenterClass(CLASS_ID);

        assertFalse(released);
        verify(escrowService, never()).apply(org.mockito.ArgumentMatchers.any());
        verify(tutoringClassRepository, never()).save(tutoringClass);
    }

    private TutoringClass centerClass() {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setNumberOfSessions(1);
        tutoringClass.setStartDate(LocalDate.now());
        tutoringClass.setEndDate(LocalDate.now());
        return tutoringClass;
    }

    private ClassStudent enrolledStudent(TutoringClass tutoringClass) {
        ClassStudent student = new ClassStudent();
        student.setClassStudentId(CLASS_STUDENT_ID);
        student.setTutoringClass(tutoringClass);
        student.setStatus(ClassStudentStatus.ENROLLED);
        return student;
    }

    private Lesson lesson(Long lessonId, TutoringClass tutoringClass) {
        Lesson lesson = new Lesson();
        lesson.setLessonId(lessonId);
        lesson.setTutoringClass(tutoringClass);
        lesson.setLessonDate(LocalDate.now());
        lesson.setSequenceNo(0);
        return lesson;
    }

    private LessonAttendance attendance(Lesson lesson, ClassStudent student) {
        LessonAttendance attendance = new LessonAttendance();
        attendance.setLesson(lesson);
        attendance.setClassStudent(student);
        return attendance;
    }

    private EscrowTransaction fundedEscrow(ClassStudent student) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(ESCROW_ID);
        escrow.setClassStudent(student);
        escrow.setStatus(EscrowStatus.FUNDED);
        escrow.setAmount(new BigDecimal("100000.00"));
        return escrow;
    }
}
