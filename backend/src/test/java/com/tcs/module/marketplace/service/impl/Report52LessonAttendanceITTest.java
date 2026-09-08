package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.LessonRescheduleRequestRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52LessonAttendanceITTest {

    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;
    private static final Long OTHER_TUTOR_ID = 21L;
    private static final Long CLIENT_USER_ID = 100L;
    private static final Long CLASS_ID = 500L;
    private static final Long LESSON_ID = 800L;

    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private CccdService cccdService;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private TutorRepository tutorRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private EscrowService escrowService;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private FavoriteTutorRepository favoriteTutorRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TutorSubjectRepository tutorSubjectRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private LessonRescheduleRequestRepository rescheduleRequestRepository;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private AuditLogService auditLogService;
    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private ContractService contractService;
    @Mock private LessonReminderService lessonReminderService;
    @Mock private EmailOtpRepository emailOtpRepository;
    @Mock private com.tcs.module.notification.service.EmailService contractEmailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    private Tutor tutor;
    private TutoringClass tutoringClass;

    @BeforeEach
    void setUpLessonAttendanceItFixture() {
        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("tutor.lesson.it@tcs.test");
        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư IT");

        User clientUser = new User();
        clientUser.setUserId(CLIENT_USER_ID);

        tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setTitle("Toán 9");
        tutoringClass.setCreator(clientUser);
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        tutoringClass.setClassType(ClassType.PRIVATE);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_001_TutorCanCheckInForOwnLessonOnLessonDate() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        marketplaceService.checkInLesson(LESSON_ID);

        assertNotNull(lesson.getTutorCheckInAt());
        verify(lessonRepository).save(lesson);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_002_ListMyLessonsReturnsOnlyPrivateTutorScheduleRows() {
        loginAsTutor();
        Lesson privateLesson = lessonToday(TUTOR_ID);
        Lesson centerLesson = lessonToday(TUTOR_ID);
        centerLesson.setLessonId(LESSON_ID + 1);
        TutoringClass centerClass = new TutoringClass();
        centerClass.setClassId(CLASS_ID + 1);
        centerClass.setTitle("Lớp trung tâm không vào lịch cá nhân");
        centerClass.setClassType(ClassType.CENTER);
        centerLesson.setTutoringClass(centerClass);

        when(lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(TUTOR_ID))
                .thenReturn(List.of(privateLesson, centerLesson));

        var responses = marketplaceService.listMyLessons();

        assertEquals(1, responses.size());
        assertEquals(LESSON_ID, responses.get(0).getLessonId());
        assertEquals(CLASS_ID, responses.get(0).getClassId());
        assertEquals("PENDING", responses.get(0).getAttendanceStatus());
        assertTrue(responses.get(0).isCanCheckInToday());
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_003_OpenLessonDetailFailsWhenLessonDoesNotExist() {
        loginAsTutor();
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertEquals("Không tìm thấy buổi học", exception.getMessage());
        verifyNoInteractions(lessonAttendanceRepository);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_004_RejectAttendanceWhenLessonIdIsMissing() {
        loginAsTutor();
        when(lessonRepository.findById(null)).thenThrow(new IllegalArgumentException("The given id must not be null"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.checkInLesson(null));

        assertEquals("The given id must not be null", exception.getMessage());
        verifyNoInteractions(lessonAttendanceRepository);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_005_RejectCheckInForCompletedLesson() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        lesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertEquals("Buổi học này đã điểm danh xong", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_006_BlockAnonymousAttendanceAccess() {
        doThrow(new ForbiddenException("Yêu cầu đăng nhập"))
                .when(authHelper)
                .requireRole(UserRole.TUTOR);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verifyNoInteractions(lessonRepository);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_007_BlockWrongRoleFromAttendanceAction() {
        doThrow(new ForbiddenException("Chỉ gia sư mới được điểm danh"))
                .when(authHelper)
                .requireRole(UserRole.TUTOR);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.checkOutLesson(LESSON_ID));

        assertEquals("Chỉ gia sư mới được điểm danh", exception.getMessage());
        verifyNoInteractions(lessonRepository);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_008_RejectCheckInForAnotherTutorLesson() {
        loginAsTutor();
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lessonToday(OTHER_TUTOR_ID)));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertEquals("Không có quyền điểm danh buổi học của gia sư khác", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_009_PreventDuplicateCheckInForTheSameLesson() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        lesson.setTutorCheckInAt(LocalDateTime.now().minusMinutes(15));
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertEquals("Bạn đã điểm danh vào buổi này rồi", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_010_TutorCompletionStoresTutorCompletedAtWhenAllLessonsAttended() {
        loginAsTutor();
        User clientUser = tutoringClass.getCreator();
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = activeAssignment();
        Lesson firstLesson = lessonToday(TUTOR_ID);
        firstLesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
        Lesson lastLesson = lessonToday(TUTOR_ID);
        lastLesson.setLessonId(LESSON_ID + 1);
        lastLesson.setSequenceNo(2);
        lastLesson.setLessonDate(LocalDate.now().plusDays(1));
        lastLesson.setAttendanceStatus(AttendanceStatus.COMPLETED);

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutor.getUser()));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(firstLesson, lastLesson));
        when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(false);

        String message = marketplaceService.confirmClassCompletion(CLASS_ID);

        assertTrue(message.contains("Đã gửi yêu cầu tới học viên"));
        assertNotNull(assignment.getTutorCompletedAt());
        verify(classAssignmentRepository).save(assignment);
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(clientUser),
                eq(com.tcs.module.messaging.enums.NotificationType.REVIEW),
                eq("MARKETPLACE_CLASS_REVIEW_REQUIRED"),
                any(),
                eq("Vui lòng đánh giá gia sư để hoàn thành lớp"),
                anyString(),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_011_CompletionRequestNotifiesClientToReviewBeforeSettlement() {
        loginAsTutor();
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = activeAssignment();
        Lesson completedLesson = lessonToday(TUTOR_ID);
        completedLesson.setAttendanceStatus(AttendanceStatus.COMPLETED);

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutor.getUser()));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(completedLesson));
        when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(false);

        marketplaceService.confirmClassCompletion(CLASS_ID);

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutoringClass.getCreator()),
                eq(com.tcs.module.messaging.enums.NotificationType.REVIEW),
                eq("MARKETPLACE_CLASS_REVIEW_REQUIRED"),
                any(),
                eq("Vui lòng đánh giá gia sư để hoàn thành lớp"),
                anyString(),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
        verify(escrowService, org.mockito.Mockito.never()).apply(any());
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_012_ReloadScheduleKeepsPendingLessonActionState() {
        loginAsTutor();
        Lesson pendingLesson = lessonToday(TUTOR_ID);

        when(lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(TUTOR_ID))
                .thenReturn(List.of(pendingLesson));

        var firstLoad = marketplaceService.listMyLessons();
        var secondLoad = marketplaceService.listMyLessons();

        assertEquals(1, firstLoad.size());
        assertEquals(firstLoad.get(0).getLessonId(), secondLoad.get(0).getLessonId());
        assertEquals("PENDING", secondLoad.get(0).getAttendanceStatus());
        assertTrue(secondLoad.get(0).isCanCheckInToday());
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_013_MarkAttendancePresentCompletesLessonInOneBackendAction() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        marketplaceService.markAttendance(LESSON_ID, true);

        assertEquals(AttendanceStatus.COMPLETED, lesson.getAttendanceStatus());
        assertNotNull(lesson.getTutorCheckInAt());
        assertNotNull(lesson.getTutorCheckOutAt());
        verify(lessonRepository).save(lesson);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_014_MarkAttendanceAbsentStoresAbsentStateWithoutCheckout() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        marketplaceService.markAttendance(LESSON_ID, false);

        assertEquals(AttendanceStatus.ABSENT, lesson.getAttendanceStatus());
        assertNull(lesson.getTutorCheckOutAt());
        verify(lessonRepository).save(lesson);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_015_ListMyLessonsKeepsBackendRowCountAndStatusValuesForTutorSchedule() {
        loginAsTutor();
        List<Lesson> lessons = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> {
                    Lesson lesson = lessonToday(TUTOR_ID);
                    lesson.setLessonId(LESSON_ID + index);
                    lesson.setSequenceNo(index);
                    lesson.setAttendanceStatus(index % 2 == 0 ? AttendanceStatus.COMPLETED : AttendanceStatus.PENDING);
                    return lesson;
                })
                .toList();

        when(lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(TUTOR_ID))
                .thenReturn(lessons);

        var responses = marketplaceService.listMyLessons();

        assertEquals(12, responses.size());
        assertEquals(6, responses.stream().filter(response -> "COMPLETED".equals(response.getAttendanceStatus())).count());
        assertEquals(6, responses.stream().filter(response -> "PENDING".equals(response.getAttendanceStatus())).count());
        verify(lessonRepository).findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(TUTOR_ID);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_018_RejectCheckInOutsideLessonDate() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        lesson.setLessonDate(LocalDate.now().minusDays(1));
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertTrue(exception.getMessage().contains("Chỉ điểm danh được trong đúng ngày diễn ra buổi học"));
        assertTrue(exception.getMessage().contains(lesson.getLessonDate().toString()));
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_019_CompletionNotificationReferencesClassForFrontendNavigation() {
        loginAsTutor();
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = activeAssignment();
        Lesson completedLesson = lessonToday(TUTOR_ID);
        completedLesson.setAttendanceStatus(AttendanceStatus.COMPLETED);

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutor.getUser()));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(completedLesson));
        when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(false);

        marketplaceService.confirmClassCompletion(CLASS_ID);

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutoringClass.getCreator()),
                eq(com.tcs.module.messaging.enums.NotificationType.REVIEW),
                eq("MARKETPLACE_CLASS_REVIEW_REQUIRED"),
                any(),
                eq("Vui lòng đánh giá gia sư để hoàn thành lớp"),
                anyString(),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_020_CheckOutCompletesLessonAfterSuccessfulCheckIn() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        lesson.setTutorCheckInAt(LocalDateTime.now().minusHours(1));
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        marketplaceService.checkOutLesson(LESSON_ID);

        assertEquals(AttendanceStatus.COMPLETED, lesson.getAttendanceStatus());
        assertNotNull(lesson.getTutorCheckOutAt());
        verify(lessonRepository).save(lesson);
    }

    @Test
    @Tag("report52-it")
    void IT_LSN_017_RejectCheckOutBeforeCheckIn() {
        loginAsTutor();
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lessonToday(TUTOR_ID)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.checkOutLesson(LESSON_ID));

        assertEquals("Cần điểm danh vào buổi trước khi kết thúc buổi", exception.getMessage());
    }

    private void loginAsTutor() {
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new com.tcs.security.UserPrincipal(tutor.getUser(), UserRole.TUTOR));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
    }

    private ClassAssignment activeAssignment() {
        TutorApplication application = new TutorApplication();
        application.setApplicationId(700L);
        application.setTutoringClass(tutoringClass);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(900L);
        assignment.setApplication(application);
        assignment.setTutor(tutor);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        return assignment;
    }

    private Lesson lessonToday(Long ownerTutorId) {
        Tutor owner = new Tutor();
        owner.setTutorId(ownerTutorId);
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(1L);
        slot.setStartTime(LocalTime.of(0, 1));
        slot.setEndTime(LocalTime.of(23, 59));
        Lesson lesson = new Lesson();
        lesson.setLessonId(LESSON_ID);
        lesson.setTutor(owner);
        lesson.setTutoringClass(tutoringClass);
        lesson.setSlot(slot);
        lesson.setLessonDate(LocalDate.now());
        lesson.setAttendanceStatus(AttendanceStatus.PENDING);
        return lesson;
    }
}
