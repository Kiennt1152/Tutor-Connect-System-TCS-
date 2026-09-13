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

    @Mock
    private com.tcs.module.catalog.repository.SystemParameterRepository systemParameterRepository;

    @Mock
    private com.tcs.module.messaging.service.NotificationDispatchService notificationDispatchService;

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

    private static final String TUTOR_DONE_KEY = "classtutorcompleted:10";

    /** Stub day du chuoi dieu kien de lop du dieu kien tat toan. */
    private EscrowTransaction givenSettleablePath(TutoringClass c, EscrowStatus escrowStatus) {
        ClassStudent student = enrolledStudent(c);
        Lesson lesson = lesson(40L, c);
        EscrowTransaction escrow = fundedEscrow(student);
        escrow.setStatus(escrowStatus);
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of(student));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(List.of(lesson));
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(anyList()))
                .thenReturn(List.of(attendance(lesson, student)));
        when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(CLASS_ID))
                .thenReturn(List.of(escrow));
        return escrow;
    }

    private TutoringClass privateClass() {
        TutoringClass c = new TutoringClass();
        c.setClassId(CLASS_ID);
        c.setClassType(ClassType.PRIVATE);
        c.setStatus(TutoringClassStatus.IN_PROGRESS);
        return c;
    }

    // ===================================================================
    //  Sheet: ceasTrySettle
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("ceasTrySettle")
    class CeasTrySettle {

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - du dieu kien -> giai ngan, lop COMPLETED, tra true")
        void utcid01_settleSuccessfully() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenSettleablePath(c, EscrowStatus.FUNDED);

            assertTrue(service.trySettleCompletedCenterClass(CLASS_ID));
            verify(escrowService).apply(org.mockito.ArgumentMatchers.any(ReleaseInstruction.class));
            assertTrue(c.getStatus() == TutoringClassStatus.COMPLETED);
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (A) - classId = null -> tra false ngay")
        void utcid02_nullClassId() {
            assertFalse(service.trySettleCompletedCenterClass(null));
            verify(escrowService, never()).apply(org.mockito.ArgumentMatchers.any());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - khong tim thay lop -> tra false")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());
            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - lop khong phai CENTER -> tra false")
        void utcid04_notCenterClass() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(privateClass()));
            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (A) - trang thai lop khong hop le (DRAFT) -> tra false")
        void utcid05_invalidStatus() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.DRAFT);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (A) - khong co hoc sinh ENROLLED -> tra false")
        void utcid06_noEnrolledStudent() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(centerClass()));
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of());
            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID07 (A) - chua diem danh du so buoi -> tra false")
        void utcid07_sessionsNotCompleted() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of(enrolledStudent(c)));
            when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                    .thenReturn(List.of());
            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID08 (A) - dang co bao cao PENDING tren lop -> tra false")
        void utcid08_pendingReportBlocks() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenSettleablePath(c, EscrowStatus.FUNDED);
            when(reportRepository.existsByTargetTypeAndTargetIdAndStatus(
                    ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING)).thenReturn(true);

            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
            verify(escrowService, never()).apply(org.mockito.ArgumentMatchers.any());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID09 (A) - hoc sinh khong co escrow -> tra false")
        void utcid09_studentWithoutEscrow() {
            TutoringClass c = centerClass();
            ClassStudent student = enrolledStudent(c);
            Lesson lesson = lesson(40L, c);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of(student));
            when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                    .thenReturn(List.of(lesson));
            when(lessonAttendanceRepository.findByLesson_LessonIdIn(anyList()))
                    .thenReturn(List.of(attendance(lesson, student)));
            when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(CLASS_ID))
                    .thenReturn(List.of());

            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
            verify(escrowService, never()).apply(org.mockito.ArgumentMatchers.any());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID10 (B) - moi escrow da RELEASED -> khong giai ngan them, lop COMPLETED, tra false")
        void utcid10_allEscrowsAlreadyReleased() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenSettleablePath(c, EscrowStatus.RELEASED);

            assertFalse(service.trySettleCompletedCenterClass(CLASS_ID));
            verify(escrowService, never()).apply(org.mockito.ArgumentMatchers.any());
            assertTrue(c.getStatus() == TutoringClassStatus.COMPLETED);
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID11 (B) - lop da COMPLETED nhung escrow con FUNDED -> van giai ngan, tra true")
        void utcid11_completedClassWithFundedEscrow() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.COMPLETED);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenSettleablePath(c, EscrowStatus.FUNDED);

            assertTrue(service.trySettleCompletedCenterClass(CLASS_ID));
            verify(escrowService).apply(org.mockito.ArgumentMatchers.any(ReleaseInstruction.class));
        }
    }

    // ===================================================================
    //  Sheet: ceasConfirmCompletion
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("ceasConfirmCompletion")
    class CeasConfirmCompletion {

        private void givenTutorConfirmed() {
            when(systemParameterRepository.findByParamKey(TUTOR_DONE_KEY))
                    .thenReturn(Optional.of(new com.tcs.module.catalog.entity.SystemParameter()));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - du dieu kien -> giai ngan, lop COMPLETED, xoa co gia su")
        void utcid01_confirmSuccessfully() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenTutorConfirmed();
            givenSettleablePath(c, EscrowStatus.FUNDED);

            service.confirmCompletion(CLASS_ID);

            verify(escrowService).apply(org.mockito.ArgumentMatchers.any(ReleaseInstruction.class));
            assertTrue(c.getStatus() == TutoringClassStatus.COMPLETED);
            verify(tutoringClassRepository).save(c);
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (A) - khong tim thay lop -> 'Không tìm thấy lớp học.'")
        void utcid02_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());
            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals("Không tìm thấy lớp học.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - khong phai lop CENTER -> 'Chỉ áp dụng cho lớp của trung tâm.'")
        void utcid03_notCenterClass() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(privateClass()));
            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals("Chỉ áp dụng cho lớp của trung tâm.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - lop da COMPLETED -> 'Khóa học đã được xác nhận hoàn thành trước đó.'")
        void utcid04_alreadyCompleted() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.COMPLETED);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));

            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals(
                    "Khóa học đã được xác nhận hoàn thành trước đó.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (A) - gia su chua xac nhan -> 'Gia sư chưa xác nhận hoàn thành khóa học — chưa thể đóng lớp.'")
        void utcid05_tutorNotConfirmed() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(centerClass()));
            when(systemParameterRepository.findByParamKey(TUTOR_DONE_KEY)).thenReturn(Optional.empty());

            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals(
                    "Gia sư chưa xác nhận hoàn thành khóa học — chưa thể đóng lớp.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (A) - trang thai lop khong hop le -> 'Lớp chưa ở trạng thái có thể hoàn thành.'")
        void utcid06_invalidStatus() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.DRAFT);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenTutorConfirmed();

            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals(
                    "Lớp chưa ở trạng thái có thể hoàn thành.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID07 (A) - chua co hoc sinh ghi danh -> 'Lớp chưa có học sinh ghi danh.'")
        void utcid07_noEnrolledStudent() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(centerClass()));
            givenTutorConfirmed();
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of());

            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals("Lớp chưa có học sinh ghi danh.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID08 (A) - chua diem danh du buoi -> 'Chưa điểm danh đủ số buổi học của khóa — không thể xác nhận hoàn thành.'")
        void utcid08_sessionsNotCompleted() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenTutorConfirmed();
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of(enrolledStudent(c)));
            when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                    .thenReturn(List.of());

            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals(
                    "Chưa điểm danh đủ số buổi học của khóa — không thể xác nhận hoàn thành.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID09 (A) - dang co khieu nai -> 'Lớp đang có khiếu nại/tranh chấp đang xử lý — chưa thể xác nhận hoàn thành.'")
        void utcid09_blockingIssue() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenTutorConfirmed();
            givenSettleablePath(c, EscrowStatus.FUNDED);
            when(reportRepository.existsByTargetTypeAndTargetIdAndStatus(
                    ReportTargetType.CLASS, CLASS_ID, ReportStatus.PENDING)).thenReturn(true);

            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.confirmCompletion(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals(
                    "Lớp đang có khiếu nại/tranh chấp đang xử lý — chưa thể xác nhận hoàn thành.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID10 (B) - escrow da RELEASED -> khong goi apply, van dong lop")
        void utcid10_escrowAlreadyReleased() {
            TutoringClass c = centerClass();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            givenTutorConfirmed();
            givenSettleablePath(c, EscrowStatus.RELEASED);

            service.confirmCompletion(CLASS_ID);

            verify(escrowService, never()).apply(org.mockito.ArgumentMatchers.any());
            assertTrue(c.getStatus() == TutoringClassStatus.COMPLETED);
        }
    }

    // ===================================================================
    //  Sheet: ceasMarkTutorConfirmed
    // ===================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("ceasMarkTutorConfirmed")
    class CeasMarkTutorConfirmed {

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - chua du dieu kien tat toan -> luu co, bao trung tam cho xac nhan")
        void utcid01_markOnly() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.MATCHED);
            c.setCreator(new com.tcs.module.identity.entity.User());
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            when(systemParameterRepository.findByParamKey(TUTOR_DONE_KEY)).thenReturn(Optional.empty());
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of());

            service.markTutorConfirmed(CLASS_ID);

            verify(systemParameterRepository).save(org.mockito.ArgumentMatchers
                    .any(com.tcs.module.catalog.entity.SystemParameter.class));
            verify(notificationDispatchService).notifyUserFromTemplate(
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (N) - du dieu kien tat toan ngay -> tu giai ngan va xoa co")
        void utcid02_autoSettles() {
            TutoringClass c = centerClass();
            c.setCreator(new com.tcs.module.identity.entity.User());
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            when(systemParameterRepository.findByParamKey(TUTOR_DONE_KEY))
                    .thenReturn(Optional.empty(), Optional.of(new com.tcs.module.catalog.entity.SystemParameter()));
            givenSettleablePath(c, EscrowStatus.FUNDED);

            service.markTutorConfirmed(CLASS_ID);

            verify(escrowService).apply(org.mockito.ArgumentMatchers.any(ReleaseInstruction.class));
            verify(systemParameterRepository).delete(org.mockito.ArgumentMatchers
                    .any(com.tcs.module.catalog.entity.SystemParameter.class));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - khong tim thay lop -> 'Không tìm thấy lớp học.'")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());
            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.markTutorConfirmed(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals("Không tìm thấy lớp học.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - khong phai lop CENTER -> 'Chỉ áp dụng cho lớp của trung tâm.'")
        void utcid04_notCenterClass() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(privateClass()));
            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.markTutorConfirmed(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals("Chỉ áp dụng cho lớp của trung tâm.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (A) - lop da COMPLETED -> 'Khóa học đã hoàn thành.'")
        void utcid05_alreadyCompleted() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.COMPLETED);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));

            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> service.markTutorConfirmed(CLASS_ID));
            org.junit.jupiter.api.Assertions.assertEquals("Khóa học đã hoàn thành.", ex.getMessage());
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (B) - da co co tu truoc -> ghi de, khong tao ban ghi trung")
        void utcid06_flagAlreadyExists() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.MATCHED);
            com.tcs.module.catalog.entity.SystemParameter existing =
                    new com.tcs.module.catalog.entity.SystemParameter();
            existing.setParameterId(7L);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            when(systemParameterRepository.findByParamKey(TUTOR_DONE_KEY)).thenReturn(Optional.of(existing));
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of());

            service.markTutorConfirmed(CLASS_ID);

            ArgumentCaptor<com.tcs.module.catalog.entity.SystemParameter> captor =
                    ArgumentCaptor.forClass(com.tcs.module.catalog.entity.SystemParameter.class);
            verify(systemParameterRepository).save(captor.capture());
            org.junit.jupiter.api.Assertions.assertEquals(7L, captor.getValue().getParameterId(),
                    "phai ghi de ban ghi cu, khong tao moi");
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.DisplayName("UTCID07 (B) - lop khong co creator -> bo qua buoc gui thong bao")
        void utcid07_noCreatorSkipsNotification() {
            TutoringClass c = centerClass();
            c.setStatus(TutoringClassStatus.MATCHED);
            c.setCreator(null);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(c));
            when(systemParameterRepository.findByParamKey(TUTOR_DONE_KEY)).thenReturn(Optional.empty());
            when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                    .thenReturn(List.of());

            service.markTutorConfirmed(CLASS_ID);

            org.mockito.Mockito.verifyNoInteractions(notificationDispatchService);
        }
    }
}
