package com.tcs.module.tutor.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.center.dto.request.MarkAttendanceRequest;
import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.MarketplaceService;
import com.tcs.module.marketplace.service.RescheduleService;
import com.tcs.module.marketplace.service.SubstitutionService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module Tutor (BF-04 bước 10-11: dạy học + điểm danh).
 * Bám bộ test case trong Report_5.1_UnitTest: các sheet markAttendance, markAttendanceBatch,
 * getClassSession, requestReschedule, requestSubstitute.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TutorServiceImplAttendanceTest {

    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;
    private static final Long OTHER_TUTOR_ID = 21L;
    private static final Long CLASS_ID = 500L;
    private static final Long CLASS_STUDENT_ID = 700L;

    @Mock private AuthHelper authHelper;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private RescheduleService rescheduleService;
    @Mock private SubstitutionService substitutionService;
    @Mock private CenterEscrowAutoSettlementService centerEscrowAutoSettlementService;
    @Mock private MarketplaceService marketplaceService;

    @InjectMocks private TutorServiceImpl service;

    private Tutor tutor;
    private TutoringClass tutoringClass;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia su 1");

        tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setTitle("Toan 9");
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setStartDate(today.minusDays(7));
        tutoringClass.setEndDate(today.plusDays(30));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(substitutionService.find(anyLong(), any())).thenReturn(Optional.empty());
        when(rescheduleService.find(anyLong(), any())).thenReturn(Optional.empty());
        when(rescheduleService.listByClassIds(any())).thenReturn(List.of());
        when(rescheduleService.listApprovedByClassIds(any())).thenReturn(List.of());
    }

    /** Tạo bản ghi điểm danh hàng loạt. */
    private List<MarkAttendanceRequest> records(Long studentId, LessonAttendanceStatus status) {
        MarkAttendanceRequest r = new MarkAttendanceRequest();
        r.setClassStudentId(studentId);
        r.setStatus(status);
        return List.of(r);
    }

    /** Gán gia sư chính đang ACTIVE cho lớp. */
    private void assignMainTutor(Long tutorId) {
        Tutor t = new Tutor();
        t.setTutorId(tutorId);
        t.setFullName("GS " + tutorId);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setTutor(t);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        when(classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
    }

    /** Lớp có buổi học đúng vào thứ của ngày {@code date}. */
    private void haveSlotOn(LocalDate date) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(1L);
        slot.setTutoringClass(tutoringClass);
        slot.setDayOfWeek(date.getDayOfWeek().getValue());
        slot.setStartTime(LocalTime.of(18, 0));
        slot.setEndTime(LocalTime.of(20, 0));
        when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(slot));
    }

    /** Lớp không có buổi nào (không slot). */
    private void haveNoSlot() {
        when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of());
    }

    private ClassStudent studentOfClass(TutoringClass owner) {
        ClassStudent s = new ClassStudent();
        s.setClassStudentId(CLASS_STUDENT_ID);
        s.setTutoringClass(owner);
        s.setStudentName("Hoc sinh A");
        return s;
    }

    // ===================================================================
    //  Sheet: markAttendance
    // ===================================================================
    @Nested
    @DisplayName("markAttendance")
    class MarkAttendance {

        @Test
        @DisplayName("UTCID01 (A) - classStudentId null -> 'Thiếu thông tin điểm danh'")
        void utcid01_missingStudentId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendance(CLASS_ID, today, null, LessonAttendanceStatus.PRESENT));
            assertEquals("Thiếu thông tin điểm danh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID02 (A) - status null -> 'Thiếu thông tin điểm danh'")
        void utcid02_missingStatus() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, null));
            assertEquals("Thiếu thông tin điểm danh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Lớp không tồn tại -> ResourceNotFoundException")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));
            assertEquals("Không tìm thấy lớp học", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Gia sư không phụ trách lớp -> ForbiddenException")
        void utcid04_notAssignedTutor() {
            assignMainTutor(OTHER_TUTOR_ID);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));
            assertEquals("Bạn không phụ trách lớp này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Lớp chưa gán gia sư nào -> ForbiddenException")
        void utcid05_noAssignment() {
            when(classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(CLASS_ID, ClassAssignmentStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThrows(ForbiddenException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));
        }

        @Test
        @DisplayName("UTCID06 (A) - Ngày không có buổi học -> 'Lớp không có buổi học vào ngày này'")
        void utcid06_noSessionOnDate() {
            assignMainTutor(TUTOR_ID);
            haveNoSlot();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));
            assertEquals("Lớp không có buổi học vào ngày này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Học sinh không tồn tại -> ResourceNotFoundException")
        void utcid07_studentNotFound() {
            assignMainTutor(TUTOR_ID);
            haveSlotOn(today);
            when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));
            assertEquals("Không tìm thấy học sinh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Học sinh thuộc lớp khác -> 'Học sinh không thuộc lớp này'")
        void utcid08_studentOfAnotherClass() {
            assignMainTutor(TUTOR_ID);
            haveSlotOn(today);
            TutoringClass another = new TutoringClass();
            another.setClassId(999L);
            when(classStudentRepository.findById(CLASS_STUDENT_ID))
                    .thenReturn(Optional.of(studentOfClass(another)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));
            assertEquals("Học sinh không thuộc lớp này", ex.getMessage());
            verify(lessonAttendanceRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: markAttendanceBatch
    // ===================================================================
    @Nested
    @DisplayName("markAttendanceBatch")
    class MarkAttendanceBatch {

        @Test
        @DisplayName("UTCID01 (A) - Danh sách rỗng -> 'Chưa có dữ liệu điểm danh'")
        void utcid01_emptyList() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendanceBatch(CLASS_ID, today, List.of()));
            assertEquals("Chưa có dữ liệu điểm danh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID02 (A) - Dữ liệu null -> 'Chưa có dữ liệu điểm danh'")
        void utcid02_nullList() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendanceBatch(CLASS_ID, today, null));
        }

        @Test
        @DisplayName("UTCID03 (A) - Ngày không có buổi học -> 'Lớp không có buổi học vào ngày này'")
        void utcid03_noSessionOnDate() {
            assignMainTutor(TUTOR_ID);
            haveNoSlot();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendanceBatch(CLASS_ID, today,
                            records(CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT)));
            assertEquals("Lớp không có buổi học vào ngày này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Gia sư không phụ trách lớp -> ForbiddenException")
        void utcid04_notAssignedTutor() {
            assignMainTutor(OTHER_TUTOR_ID);

            assertThrows(ForbiddenException.class,
                    () -> service.markAttendanceBatch(CLASS_ID, today,
                            records(CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT)));
        }
    }

    // ===================================================================
    //  Sheet: getClassSession
    // ===================================================================
    @Nested
    @DisplayName("getClassSession")
    class GetClassSession {

        @Test
        @DisplayName("UTCID01 (A) - Ngày không có buổi học -> 'Lớp không có buổi học vào ngày này'")
        void utcid01_noSessionOnDate() {
            assignMainTutor(TUTOR_ID);
            haveNoSlot();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.getClassSession(CLASS_ID, today));
            assertEquals("Lớp không có buổi học vào ngày này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID02 (A) - Gia sư không phụ trách lớp -> ForbiddenException")
        void utcid02_notAssignedTutor() {
            assignMainTutor(OTHER_TUTOR_ID);

            assertThrows(ForbiddenException.class, () -> service.getClassSession(CLASS_ID, today));
        }

        @Test
        @DisplayName("UTCID03 (A) - Lớp không tồn tại -> ResourceNotFoundException")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getClassSession(CLASS_ID, today));
        }
    }
}
