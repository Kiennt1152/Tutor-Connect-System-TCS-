package com.tcs.module.tutor.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.center.dto.request.MarkAttendanceRequest;
import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonAttendance;
import com.tcs.module.center.dto.request.RescheduleRequestBody;
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
    @DisplayName("tuMarkAttendance")
    class MarkAttendance {

        @Test
        @DisplayName("UTCID02 (A) - classStudentId null -> 'Thiếu thông tin điểm danh'")
        void utcid01_missingStudentId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendance(CLASS_ID, today, null, LessonAttendanceStatus.PRESENT));
            assertEquals("Thiếu thông tin điểm danh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - status null -> 'Thiếu thông tin điểm danh'")
        void utcid02_missingStatus() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, null));
            assertEquals("Thiếu thông tin điểm danh", ex.getMessage());
        }

        @Test
        @DisplayName("Bổ sung ngoài các UTCID của sheet tuMarkAttendance - Lớp không tồn tại -> ResourceNotFoundException")
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
        @DisplayName("Bổ sung ngoài các UTCID của sheet tuMarkAttendance - Lớp chưa gán gia sư nào -> ForbiddenException")
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
        @DisplayName("UTCID02 (A) - Danh sách rỗng -> 'Chưa có dữ liệu điểm danh'")
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
        @DisplayName("Bổ sung ngoài các UTCID của sheet markAttendanceBatch - Gia sư không phụ trách lớp -> ForbiddenException")
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
        @DisplayName("UTCID02 (A) - Ngày không có buổi học -> 'Lớp không có buổi học vào ngày này'")
        void utcid01_noSessionOnDate() {
            assignMainTutor(TUTOR_ID);
            haveNoSlot();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.getClassSession(CLASS_ID, today));
            assertEquals("Lớp không có buổi học vào ngày này", ex.getMessage());
        }

        @Test
        @DisplayName("Bổ sung ngoài các UTCID của sheet getClassSession - Gia sư không phụ trách lớp -> ForbiddenException")
        void utcid02_notAssignedTutor() {
            assignMainTutor(OTHER_TUTOR_ID);

            assertThrows(ForbiddenException.class, () -> service.getClassSession(CLASS_ID, today));
        }

        @Test
        @DisplayName("Bổ sung ngoài các UTCID của sheet getClassSession - Lớp không tồn tại -> ResourceNotFoundException")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getClassSession(CLASS_ID, today));
        }
    }

    // ===================================================================
    //  Sheet: tuConfirmCompletion (gia su xac nhan khoa hoc hoan thanh)
    // ===================================================================
    @Nested
    @DisplayName("tuConfirmCompletion")
    class TuConfirmCompletion {

        /** Lượt gán ACTIVE của lớp, do gia sư {@code ownerTutorId} phụ trách. */
        private ClassAssignment activeAssignment(Long ownerTutorId, TutoringClass linkedClass) {
            Tutor owner = new Tutor();
            owner.setTutorId(ownerTutorId);

            ClassAssignment assignment = new ClassAssignment();
            assignment.setAssignmentId(900L);
            assignment.setTutor(owner);
            assignment.setStatus(ClassAssignmentStatus.ACTIVE);
            if (linkedClass != null) {
                com.tcs.module.marketplace.entity.TutorApplication application =
                        new com.tcs.module.marketplace.entity.TutorApplication();
                application.setApplicationId(800L);
                application.setTutoringClass(linkedClass);
                application.setTutor(owner);
                assignment.setApplication(application);
            }
            return assignment;
        }

        private void givenAssignment(ClassAssignment assignment) {
            when(classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(CLASS_ID, ClassAssignmentStatus.ACTIVE))
                    .thenReturn(assignment == null ? Optional.empty() : Optional.of(assignment));
        }

        @Test
        @DisplayName("UTCID01 (N) - Gia su phu trach lop CENTER -> markTutorConfirmed va tra thong bao trung tam")
        void utcid01_centerClass() {
            tutoringClass.setClassType(ClassType.CENTER);
            givenAssignment(activeAssignment(TUTOR_ID, tutoringClass));

            String message = service.confirmClassCompletion(CLASS_ID);

            verify(centerEscrowAutoSettlementService).markTutorConfirmed(CLASS_ID);
            assertTrue(message.startsWith("Đã xác nhận khóa học hoàn thành."),
                    "Phai tra thong bao xac nhan: " + message);
            verify(marketplaceService, never()).confirmClassCompletion(anyLong());
        }

        @Test
        @DisplayName("UTCID02 (N) - Gia su phu trach lop ca nhan -> chuyen cho MarketplaceService")
        void utcid02_privateClass() {
            tutoringClass.setClassType(ClassType.PRIVATE);
            givenAssignment(activeAssignment(TUTOR_ID, tutoringClass));
            when(marketplaceService.confirmClassCompletion(CLASS_ID)).thenReturn("Đã xác nhận hoàn thành lớp");

            String message = service.confirmClassCompletion(CLASS_ID);

            assertEquals("Đã xác nhận hoàn thành lớp", message);
            verify(centerEscrowAutoSettlementService, never()).markTutorConfirmed(anyLong());
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai gia su -> requireTutor chan lai")
        void utcid03_callerIsNotATutor() {
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Không tìm thấy hồ sơ gia sư", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Lop khong co luot gan ACTIVE nao -> 'Bạn không phụ trách lớp này'")
        void utcid04_noActiveAssignment() {
            givenAssignment(null);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Bạn không phụ trách lớp này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Luot gan ACTIVE thuoc gia su khac -> 'Bạn không phụ trách lớp này'")
        void utcid05_assignmentOfAnotherTutor() {
            givenAssignment(activeAssignment(OTHER_TUTOR_ID, tutoringClass));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Bạn không phụ trách lớp này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Luot gan khong co application nen khong xac dinh duoc lop -> 'Không tìm thấy lớp học.'")
        void utcid06_assignmentWithoutApplication() {
            givenAssignment(activeAssignment(TUTOR_ID, null));

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Không tìm thấy lớp học.", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: tuRequestReschedule (gia su xin doi lich buoi hoc)
    // ===================================================================
    @Nested
    @DisplayName("tuRequestReschedule")
    class TuRequestReschedule {

        /** Ngày trong khoảng lớp và có tiết đúng thứ. */
        private LocalDate original;
        private LocalDate next;

        @BeforeEach
        void initDates() {
            original = today.plusDays(1);
            next = today.plusDays(2);
            tutoringClass.setStartDate(today.minusDays(7));
            tutoringClass.setEndDate(today.plusDays(30));
            assignMainTutor(TUTOR_ID);
        }

        private RescheduleRequestBody body(LocalDate originalDate, LocalDate newDate, String start, String end) {
            RescheduleRequestBody b = new RescheduleRequestBody();
            b.setOriginalDate(originalDate);
            b.setNewDate(newDate);
            b.setNewStartTime(start);
            b.setNewEndTime(end);
            b.setReason("Gia sư bị ốm");
            return b;
        }

        /** Lớp có tiết vào đúng thứ của cả hai ngày, để qua được các bước kiểm tra lịch. */
        private void haveSlotsOnBothDays() {
            ScheduleSlot s1 = new ScheduleSlot();
            s1.setSlotId(1L);
            s1.setTutoringClass(tutoringClass);
            s1.setDayOfWeek(original.getDayOfWeek().getValue());
            s1.setStartTime(LocalTime.of(18, 0));
            s1.setEndTime(LocalTime.of(20, 0));
            when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(s1));
        }

        private void givenRescheduleAccepted() {
            when(rescheduleService.request(anyLong(), any(), any(), any(), any(), anyLong(), any()))
                    .thenAnswer(i -> new com.tcs.module.marketplace.dto.RescheduleEntry(
                            i.getArgument(0), i.getArgument(1), i.getArgument(2),
                            i.getArgument(3), i.getArgument(4),
                            com.tcs.module.marketplace.dto.RescheduleEntry.PENDING,
                            i.getArgument(5), i.getArgument(6)));
            when(classAssignmentRepository.findByTutor_TutorIdAndStatus(TUTOR_ID, ClassAssignmentStatus.ACTIVE))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("UTCID01 (N) - Gia su phu trach lop, hai ngay hop le -> tao yeu cau doi lich")
        void utcid01_requestSuccessfully() {
            haveSlotsOnBothDays();
            givenRescheduleAccepted();

            var response = service.requestReschedule(CLASS_ID, body(original, next, "18:00", "20:00"));

            assertEquals(CLASS_ID, response.getClassId());
            assertEquals(original, response.getOriginalDate());
            assertEquals(next, response.getNewDate());
            verify(rescheduleService).request(eq(CLASS_ID), eq(original), eq(next),
                    eq(LocalTime.of(18, 0)), eq(LocalTime.of(20, 0)), eq(TUTOR_ID), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong phai gia su -> requireTutor chan lai")
        void utcid02_callerIsNotATutor() {
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.requestReschedule(CLASS_ID, body(original, next, "18:00", "20:00")));
            assertEquals("Không tìm thấy hồ sơ gia sư", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - classId khong khop lop nao -> 'Không tìm thấy lớp học'")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.requestReschedule(CLASS_ID, body(original, next, "18:00", "20:00")));
            assertEquals("Không tìm thấy lớp học", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Gia su khong phu trach lop -> 'Bạn không phụ trách lớp này'")
        void utcid04_notAssignedTutor() {
            assignMainTutor(OTHER_TUTOR_ID);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.requestReschedule(CLASS_ID, body(original, next, "18:00", "20:00")));
            assertEquals("Bạn không phụ trách lớp này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - originalDate = null -> 'Vui lòng chọn ngày cần dời và ngày mới'")
        void utcid05_missingOriginalDate() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestReschedule(CLASS_ID, body(null, next, "18:00", "20:00")));
            assertEquals("Vui lòng chọn ngày cần dời và ngày mới", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - newDate = null -> 'Vui lòng chọn ngày cần dời và ngày mới'")
        void utcid06_missingNewDate() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestReschedule(CLASS_ID, body(original, null, "18:00", "20:00")));
            assertEquals("Vui lòng chọn ngày cần dời và ngày mới", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - newDate trung originalDate -> 'Ngày mới phải khác ngày cần dời'")
        void utcid07_sameDate() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestReschedule(CLASS_ID, body(original, original, "18:00", "20:00")));
            assertEquals("Ngày mới phải khác ngày cần dời", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - newDate = hom qua (duoi can duoi) -> 'Ngày mới phải từ hôm nay trở đi'")
        void utcid08_newDateInThePast() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestReschedule(CLASS_ID,
                            body(original, today.minusDays(1), "18:00", "20:00")));
            assertEquals("Ngày mới phải từ hôm nay trở đi", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (B) - newDate = hom nay (dung can duoi) va trong khoang lop -> chap nhan")
        void utcid09_newDateIsToday() {
            haveSlotsOnBothDays();
            givenRescheduleAccepted();

            var response = service.requestReschedule(CLASS_ID, body(original, today, "18:00", "20:00"));

            assertEquals(today, response.getNewDate(),
                    "Ngay mới bằng hôm nay là hợp lệ (cận dưới)");
        }

        @Test
        @DisplayName("UTCID10 (A) - originalDate khong phai buoi hoc cua lop -> 'Ngày cần dời không phải buổi học của lớp'")
        void utcid10_originalDateIsNotASession() {
            when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestReschedule(CLASS_ID, body(original, next, "18:00", "20:00")));
            assertEquals("Ngày cần dời không phải buổi học của lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - newDate ngoai khoang ngay cua lop -> 'Ngày mới phải nằm trong khoảng thời gian của lớp'")
        void utcid11_newDateOutsideClassRange() {
            haveSlotsOnBothDays();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestReschedule(CLASS_ID,
                            body(original, today.plusDays(60), "18:00", "20:00")));
            assertEquals("Ngày mới phải nằm trong khoảng thời gian của lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID12 (A) - Gio ket thuc khong sau gio bat dau -> 'Khung giờ mới không hợp lệ ...'")
        void utcid12_invalidTimeWindow() {
            haveSlotsOnBothDays();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestReschedule(CLASS_ID, body(original, next, "20:00", "18:00")));
            assertEquals("Khung giờ mới không hợp lệ (giờ kết thúc phải sau giờ bắt đầu)", ex.getMessage());
            verify(rescheduleService, never()).request(anyLong(), any(), any(), any(), any(), anyLong(), any());
        }
    }
    // ===================================================================
    //  Sheet: markAttendanceBatch - cac ca con lai
    // ===================================================================
    @Nested
    @DisplayName("markAttendanceBatch")
    class MarkAttendanceBatchRemaining {

        @org.junit.jupiter.api.BeforeEach
        void givenTeachableSession() {
            assignMainTutor(TUTOR_ID);
            haveSlotOn(today);
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> {
                Lesson lesson = i.getArgument(0);
                lesson.setLessonId(4000L);
                return lesson;
            });
            when(lessonAttendanceRepository.save(any(LessonAttendance.class)))
                    .thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("UTCID01 (N) - Du lieu day du, dung buoi, hoc sinh thuoc lop, chua diem danh -> ghi nhan trang thai cho tung hoc sinh")
        void utcid01_markSuccessfully() {
            when(classStudentRepository.findById(CLASS_STUDENT_ID))
                    .thenReturn(Optional.of(studentOfClass(tutoringClass)));
            when(lessonAttendanceRepository
                    .findFirstByLesson_LessonIdAndClassStudent_ClassStudentId(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());

            service.markAttendanceBatch(CLASS_ID, today,
                    records(CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));

            org.mockito.ArgumentCaptor<LessonAttendance> captor =
                    org.mockito.ArgumentCaptor.forClass(LessonAttendance.class);
            verify(lessonAttendanceRepository).save(captor.capture());
            assertEquals(LessonAttendanceStatus.PRESENT, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("UTCID04 (A) - Buoi hoc da duoc diem danh truoc do -> khong the diem danh lai")
        void utcid04_alreadyMarked() {
            Lesson existing = new Lesson();
            existing.setLessonId(4001L);
            existing.setTutoringClass(tutoringClass);
            existing.setSequenceNo(1);
            existing.setLessonDate(today);
            when(lessonRepository.findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(
                    eq(CLASS_ID), anyLong(), anyInt())).thenReturn(Optional.of(existing));
            when(lessonAttendanceRepository.findByLesson_LessonId(4001L))
                    .thenReturn(List.of(new LessonAttendance()));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendanceBatch(CLASS_ID, today,
                            records(CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT)));
            assertEquals("Buổi học này đã được điểm danh, không thể điểm danh lại", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Hoc sinh khong thuoc lop nay -> chan")
        void utcid05_studentFromAnotherClass() {
            TutoringClass otherClass = new TutoringClass();
            otherClass.setClassId(999L);
            when(classStudentRepository.findById(CLASS_STUDENT_ID))
                    .thenReturn(Optional.of(studentOfClass(otherClass)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.markAttendanceBatch(CLASS_ID, today,
                            records(CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT)));
            assertEquals("Học sinh không thuộc lớp này", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: requestSubstitute
    // ===================================================================
    @Nested
    @DisplayName("requestSubstitute")
    class RequestSubstitute {

        private static final Long ASSISTANT_ID = 33L;

        private com.tcs.module.center.dto.request.SubstituteRequestBody body(LocalDate date) {
            var b = new com.tcs.module.center.dto.request.SubstituteRequestBody();
            b.setDate(date);
            b.setReason("Toi ban viec dot xuat");
            return b;
        }

        @org.junit.jupiter.api.BeforeEach
        void givenMainTutorAndAssistant() {
            assignMainTutor(TUTOR_ID);
            haveSlotOn(today);
            when(substitutionService.findAssistant(CLASS_ID)).thenReturn(Optional.of(ASSISTANT_ID));
        }

        @Test
        @DisplayName("UTCID01 (N) - Buoi hop le, tu hom nay tro di, chua co yeu cau doi lich -> tao yeu cau day thay")
        void utcid01_requestSuccessfully() {
            var entry = new com.tcs.module.marketplace.dto.SubstitutionEntry(
                    CLASS_ID, today, ASSISTANT_ID,
                    com.tcs.module.marketplace.dto.SubstitutionEntry.PENDING, "Toi ban viec dot xuat");
            when(substitutionService.request(eq(CLASS_ID), eq(today), eq(ASSISTANT_ID), anyString()))
                    .thenReturn(entry);

            service.requestSubstitute(CLASS_ID, body(today));

            verify(substitutionService).request(eq(CLASS_ID), eq(today), eq(ASSISTANT_ID), anyString());
        }

        @Test
        @DisplayName("UTCID02 (A) - Khong chon buoi can day thay -> chan")
        void utcid02_noDateSelected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestSubstitute(CLASS_ID, body(null)));
            assertEquals("Vui lòng chọn buổi cần nhờ dạy thay", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Buoi nay von la buoi da duoc doi lich -> chan")
        void utcid03_sessionIsRescheduledTarget() {
            var arriving = new com.tcs.module.marketplace.dto.RescheduleEntry(
                    CLASS_ID, today.minusDays(2), today, null, null,
                    com.tcs.module.marketplace.dto.RescheduleEntry.APPROVED, TUTOR_ID, "Doi lich");
            when(rescheduleService.listApprovedByClassIds(any())).thenReturn(List.of(arriving));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestSubstitute(CLASS_ID, body(today)));
            assertEquals(
                    "Buổi này là buổi đã được dời lịch nên không thể nhờ gia sư phụ dạy thay.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Ngay chon khong phai buoi hoc cua lop -> chan")
        void utcid04_dateIsNotAClassSession() {
            haveNoSlot();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestSubstitute(CLASS_ID, body(today)));
            assertEquals("Ngày cần dạy thay không phải buổi học của lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (B) - Buoi hoc hom qua (ngay duoi nguong 'tu hom nay') -> chan")
        void utcid05_pastSession() {
            LocalDate yesterday = today.minusDays(1);
            haveSlotOn(yesterday);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestSubstitute(CLASS_ID, body(yesterday)));
            assertEquals("Chỉ có thể nhờ dạy thay cho buổi từ hôm nay trở đi", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Buoi da co yeu cau doi lich -> khong the vua doi lich vua nho day thay")
        void utcid06_sessionAlreadyHasReschedule() {
            var pending = new com.tcs.module.marketplace.dto.RescheduleEntry(
                    CLASS_ID, today, today.plusDays(1), null, null,
                    com.tcs.module.marketplace.dto.RescheduleEntry.PENDING, TUTOR_ID, "Doi lich");
            when(rescheduleService.find(CLASS_ID, today)).thenReturn(Optional.of(pending));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.requestSubstitute(CLASS_ID, body(today)));
            assertEquals(
                    "Buổi này đã có yêu cầu đổi lịch. Không thể vừa đổi lịch vừa nhờ dạy thay.",
                    ex.getMessage());
        }
    }
    // ===================================================================
    //  Cac ca con thieu cua tuMarkAttendance / getClassSession
    // ===================================================================

    /**
     * Sheet tuMarkAttendance - UTCID01 (N): dung gia su phu trach, co buoi hoc trong ngay,
     * hoc sinh thuoc lop, du lieu day du -> ghi nhan trang thai diem danh cho hoc sinh.
     */
    @Test
    void markAttendanceRecordsStatusOnHappyPath() {
        assignMainTutor(TUTOR_ID);
        haveSlotOn(today);
        when(classStudentRepository.findById(CLASS_STUDENT_ID))
                .thenReturn(Optional.of(studentOfClass(tutoringClass)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> {
            Lesson lesson = i.getArgument(0);
            lesson.setLessonId(5000L);
            return lesson;
        });
        when(lessonAttendanceRepository
                .findFirstByLesson_LessonIdAndClassStudent_ClassStudentId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(lessonAttendanceRepository.save(any(LessonAttendance.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.markAttendance(CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT);

        org.mockito.ArgumentCaptor<LessonAttendance> captor =
                org.mockito.ArgumentCaptor.forClass(LessonAttendance.class);
        verify(lessonAttendanceRepository).save(captor.capture());
        assertEquals(LessonAttendanceStatus.PRESENT, captor.getValue().getStatus());
    }

    /**
     * Sheet tuMarkAttendance - UTCID05 (A): buoi hoc da duoc nho gia su phu day thay
     * -> gia su chinh khong con quyen diem danh buoi do.
     */
    @Test
    void markAttendanceRejectsSessionHandedToSubstitute() {
        assignMainTutor(TUTOR_ID);
        haveSlotOn(today);
        var approvedSubstitution = new com.tcs.module.marketplace.dto.SubstitutionEntry(
                CLASS_ID, today, 99L,
                com.tcs.module.marketplace.dto.SubstitutionEntry.APPROVED, "Ban dot xuat");
        when(substitutionService.find(CLASS_ID, today)).thenReturn(Optional.of(approvedSubstitution));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> service.markAttendance(
                        CLASS_ID, today, CLASS_STUDENT_ID, LessonAttendanceStatus.PRESENT));
        assertEquals("Buổi này đã được nhờ gia sư phụ dạy thay.", ex.getMessage());
        verify(lessonAttendanceRepository, never()).save(any());
    }

    /**
     * Sheet getClassSession - UTCID01 (N): lop co buoi hoc dung ngay yeu cau
     * -> tra ve thong tin buoi hoc kem danh sach hoc sinh.
     */
    @Test
    void getClassSessionReturnsSessionOnHappyPath() {
        assignMainTutor(TUTOR_ID);
        haveSlotOn(today);
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(anyLong(), any()))
                .thenReturn(List.of(studentOfClass(tutoringClass)));

        var session = service.getClassSession(CLASS_ID, today);

        assertNotNull(session);
        assertEquals(CLASS_ID, session.getClassId());
    }
}
