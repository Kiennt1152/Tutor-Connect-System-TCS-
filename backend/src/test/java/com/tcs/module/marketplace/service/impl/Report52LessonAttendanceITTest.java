package com.tcs.module.marketplace.service.impl;

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
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.messaging.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import com.tcs.module.platform.repository.ReportRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
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

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52LessonAttendanceITTest {



    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;
    private static final Long OTHER_TUTOR_ID = 21L;
    private static final Long CLIENT_USER_ID = 100L;
    private static final Long CLASS_ID = 500L;
    private static final Long LESSON_ID = 800L;
    private static final Long ASSIGNMENT_ID = 90L;

    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private OtpService otpService;

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

    
    /**
     * Test Case: IT-LSN-001
     * Title: Allow the assigned tutor to check in on the lesson date.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Lesson 40 belongs to the current tutor and its lessonDate is today.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_001_TutorCanCheckInForOwnLessonOnLessonDate.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert check-in timestamp and LessonRepository.save.
     * Expected: The lesson receives tutorCheckInAt and is saved.
     * Pre-conditions: Lesson 40 belongs to the current tutor and its lessonDate is today.
     */
    
    /**
     * Test Case: IT-LSN-001
     * Title: Allow the assigned tutor to check in on the lesson date.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Lesson 40 belongs to the current tutor and its lessonDate is today.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_001_TutorCanCheckInForOwnLessonOnLessonDate.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert check-in timestamp and LessonRepository.save.
     * Expected: The lesson receives tutorCheckInAt and is saved.
     * Pre-conditions: Lesson 40 belongs to the current tutor and its lessonDate is today.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-001: Allow the assigned tutor to check in on the lesson date.")
    void IT_LSN_001_TutorCanCheckInForOwnLessonOnLessonDate() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        marketplaceService.checkInLesson(LESSON_ID);

        assertNotNull(lesson.getTutorCheckInAt());
        verify(lessonRepository).save(lesson);
    }

    /**
     * Test Case: IT-LSN-002
     * Title: List the current tutor’s private lessons with today’s check-in availability.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.listMyLessons (GET /api/marketplace/lessons/mine).
     * Input: Authenticated tutor session.
     * Steps:
     *   1. Prepare the fixture: Tutor owns one private lesson scheduled for today.
     *   2. Use the input: Authenticated tutor session.
     *   3. Execute MarketplaceServiceImpl.listMyLessons (GET /api/marketplace/lessons/mine). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_002_ListMyLessonsReturnsOnlyPrivateTutorScheduleRows.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert row id/class/status/availability and owner-scoped query.
     * Expected: Only the tutor’s lesson 40 is returned with class 77, PENDING attendance and canCheckInToday=true.
     * Pre-conditions: Tutor owns one private lesson scheduled for today.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-002: List the current tutor’s private lessons with today’s check-in availability.")
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

    /**
     * Test Case: IT-LSN-003
     * Title: Return not-found when checking in a missing lesson.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: LessonRepository has no row for lesson 40.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_003_OpenLessonDetailFailsWhenLessonDoesNotExist.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify attendance repository is untouched.
     * Expected: The service returns “Không tìm thấy buổi học” and does not create attendance.
     * Pre-conditions: LessonRepository has no row for lesson 40.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-003: Return not-found when checking in a missing lesson.")
    void IT_LSN_003_OpenLessonDetailFailsWhenLessonDoesNotExist() {
        loginAsTutor();
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertEquals("Không tìm thấy buổi học", exception.getMessage());
        verifyNoInteractions(lessonAttendanceRepository);
    }

    /**
     * Test Case: IT-LSN-004
     * Title: Reject attendance when the lesson id is missing.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=null.
     * Steps:
     *   1. Prepare the fixture: Tutor role is authenticated.
     *   2. Use the input: lessonId=null.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_004_RejectAttendanceWhenLessonIdIsMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify no attendance write.
     * Expected: The null-id argument error is returned before attendance persistence.
     * Pre-conditions: Tutor role is authenticated.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-004: Reject attendance when the lesson id is missing.")
    void IT_LSN_004_RejectAttendanceWhenLessonIdIsMissing() {
        loginAsTutor();
        when(lessonRepository.findById(null)).thenThrow(new IllegalArgumentException("The given id must not be null"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.checkInLesson(null));

        assertEquals("The given id must not be null", exception.getMessage());
        verifyNoInteractions(lessonAttendanceRepository);
    }

    /**
     * Test Case: IT-LSN-005
     * Title: Reject check-in for a lesson already marked complete.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Lesson 40 has COMPLETED attendance status.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_005_RejectCheckInForCompletedLesson.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no duplicate check-in.
     * Expected: The service returns “Buổi học này đã điểm danh xong” and leaves the lesson unchanged.
     * Pre-conditions: Lesson 40 has COMPLETED attendance status.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-005: Reject check-in for a lesson already marked complete.")
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

    /**
     * Test Case: IT-LSN-006
     * Title: Block anonymous attendance access.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: No authenticated user.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_006_BlockAnonymousAttendanceAccess.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify LessonRepository is not called.
     * Expected: The service returns “Yêu cầu đăng nhập” before loading a lesson.
     * Pre-conditions: No authenticated user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-006: Block anonymous attendance access.")
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

    /**
     * Test Case: IT-LSN-007
     * Title: Prevent a non-tutor role from using attendance actions.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Authenticated user is not a tutor.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_007_BlockWrongRoleFromAttendanceAction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role error and verify no repository access.
     * Expected: The service returns “Chỉ gia sư mới được điểm danh” and does not load a lesson.
     * Pre-conditions: Authenticated user is not a tutor.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-007: Prevent a non-tutor role from using attendance actions.")
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

    /**
     * Test Case: IT-LSN-008
     * Title: Prevent a tutor from checking in to another tutor’s lesson.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Lesson 40 belongs to another tutor.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_008_RejectCheckInForAnotherTutorLesson.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify no save.
     * Expected: The service returns the ownership error and does not update the lesson.
     * Pre-conditions: Lesson 40 belongs to another tutor.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-008: Prevent a tutor from checking in to another tutor’s lesson.")
    void IT_LSN_008_RejectCheckInForAnotherTutorLesson() {
        loginAsTutor();
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lessonToday(OTHER_TUTOR_ID)));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.checkInLesson(LESSON_ID));

        assertEquals("Không có quyền điểm danh buổi học của gia sư khác", exception.getMessage());
    }

    /**
     * Test Case: IT-LSN-009
     * Title: Reject a duplicate check-in for the same lesson.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Lesson 40 already has a tutor check-in.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_009_PreventDuplicateCheckInForTheSameLesson.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify no duplicate save.
     * Expected: The service returns “Bạn đã điểm danh vào buổi này rồi” and does not save again.
     * Pre-conditions: Lesson 40 already has a tutor check-in.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-009: Reject a duplicate check-in for the same lesson.")
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

    /**
     * Test Case: IT-LSN-010
     * Title: Record tutor completion when all lessons are attended but client review is pending.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: All lessons in class 77 are completed; client has not reviewed.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_010_TutorCompletionStoresTutorCompletedAtWhenAllLessonsAttended.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert message/timestamp and verify client review notification.
     * Expected: The tutorCompletedAt timestamp is saved, the response says the request was sent to the student and the class is not settled yet.
     * Pre-conditions: All lessons in class 77 are completed; client has not reviewed.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-010: Record tutor completion when all lessons are attended but client review is pending.")
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

    /**
     * Test Case: IT-LSN-011
     * Title: Notify the client to review before private-class settlement.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: Private lessons are complete and hasClientReviewedClass(77)=false.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_011_CompletionRequestNotifiesClientToReviewBeforeSettlement.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification and verify no premature release.
     * Expected: The REVIEW notification uses MARKETPLACE_CLASS_REVIEW_REQUIRED and class reference 77; no settlement is applied at this point.
     * Pre-conditions: Private lessons are complete and hasClientReviewedClass(77)=false.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-011: Notify the client to review before private-class settlement.")
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
    }

    /**
     * Test Case: IT-LSN-012
     * Title: Keep a pending lesson action state after reloading the tutor schedule.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.listMyLessons (GET /api/marketplace/lessons/mine).
     * Input: Two consecutive list requests.
     * Steps:
     *   1. Prepare the fixture: Tutor schedule contains one pending lesson for today.
     *   2. Use the input: Two consecutive list requests.
     *   3. Execute MarketplaceServiceImpl.listMyLessons (GET /api/marketplace/lessons/mine). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_012_ReloadScheduleKeepsPendingLessonActionState.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Compare both responses and verify stable lesson id/status.
     * Expected: The same lesson remains PENDING and canCheckInToday=true on the second load.
     * Pre-conditions: Tutor schedule contains one pending lesson for today.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-012: Keep a pending lesson action state after reloading the tutor schedule.")
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

    /**
     * Test Case: IT-LSN-013
     * Title: Mark a lesson PRESENT and complete it in one backend action.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.markAttendance (POST /api/marketplace/lessons/{lessonId}/attend).
     * Input: lessonId=40; present=true.
     * Steps:
     *   1. Prepare the fixture: Tutor owns a pending lesson scheduled for today.
     *   2. Use the input: lessonId=40; present=true.
     *   3. Execute MarketplaceServiceImpl.markAttendance (POST /api/marketplace/lessons/{lessonId}/attend). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_013_MarkAttendancePresentCompletesLessonInOneBackendAction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status/timestamps and LessonRepository.save.
     * Expected: The lesson becomes COMPLETED with both check-in and check-out timestamps and is saved.
     * Pre-conditions: Tutor owns a pending lesson scheduled for today.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-013: Mark a lesson PRESENT and complete it in one backend action.")
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

    /**
     * Test Case: IT-LSN-014
     * Title: Record an ABSENT lesson without a checkout timestamp.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.markAttendance (POST /api/marketplace/lessons/{lessonId}/attend).
     * Input: lessonId=40; present=false.
     * Steps:
     *   1. Prepare the fixture: Tutor owns a pending lesson.
     *   2. Use the input: lessonId=40; present=false.
     *   3. Execute MarketplaceServiceImpl.markAttendance (POST /api/marketplace/lessons/{lessonId}/attend). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_014_MarkAttendanceAbsentStoresAbsentStateWithoutCheckout.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status/null checkout and save.
     * Expected: The lesson becomes ABSENT, checkout remains null and the lesson is saved.
     * Pre-conditions: Tutor owns a pending lesson.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-014: Record an ABSENT lesson without a checkout timestamp.")
    void IT_LSN_014_MarkAttendanceAbsentStoresAbsentStateWithoutCheckout() {
        loginAsTutor();
        Lesson lesson = lessonToday(TUTOR_ID);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        marketplaceService.markAttendance(LESSON_ID, false);

        assertEquals(AttendanceStatus.ABSENT, lesson.getAttendanceStatus());
        assertNull(lesson.getTutorCheckOutAt());
        verify(lessonRepository).save(lesson);
    }

    /**
     * Test Case: IT-LSN-015
     * Title: Preserve lesson row count and attendance status values in the tutor schedule.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.listMyLessons (GET /api/marketplace/lessons/mine).
     * Input: Authenticated tutor schedule request.
     * Steps:
     *   1. Prepare the fixture: Tutor repository returns 12 lessons with the stated status distribution.
     *   2. Use the input: Authenticated tutor schedule request.
     *   3. Execute MarketplaceServiceImpl.listMyLessons (GET /api/marketplace/lessons/mine). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_015_ListMyLessonsKeepsBackendRowCountAndStatusValuesForTutorSchedule.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert total and per-status counts.
     * Expected: The schedule returns 12 lessons: six COMPLETED and six PENDING.
     * Pre-conditions: Tutor repository returns 12 lessons with the stated status distribution.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-015: Preserve lesson row count and attendance status values in the tutor schedule.")
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

    /**
     * Test Case: IT-LSN-016
     * Title: Auto-release the first escrow period of a long private class when completed lesson value covers it.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout).
     * Input: Checkout lesson 3 for assignment 800.
     * Steps:
     *   1. Prepare the fixture: Long private class has enough completed lesson value, funded escrow and no dispute/termination blocker.
     *   2. Use the input: Checkout lesson 3 for assignment 800.
     *   3. Execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_LSN_016_LongPrivateClassAutoReleasesFirstMonthEscrowWhenCompletedLessonValueCoversPaidEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert lesson status and captured escrow release amount.
     * Expected: After the third completed lesson, escrow 95 receives a 100000 release instruction and the lesson is COMPLETED.
     * Pre-conditions: Long private class has enough completed lesson value, funded escrow and no dispute/termination blocker.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-016: Auto-release the first escrow period of a long private class when completed lesson value covers it.")
    void IT_LSN_016_LongPrivateClassAutoReleasesFirstMonthEscrowWhenCompletedLessonValueCoversPaidEscrow() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setStartDate(LocalDate.now().minusDays(10));
        tutoringClass.setEndDate(LocalDate.now().plusMonths(2));
        configurePrivateHourlyDeal(tutoringClass, new BigDecimal("50000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 3, 2);
        lessons.get(2).setLessonDate(LocalDate.now());
        lessons.get(2).setTutorCheckInAt(LocalDateTime.now().minusMinutes(40));
        EscrowTransaction escrow = escrow(95L, new BigDecimal("100000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(lessonRepository.findById(lessons.get(2).getLessonId())).thenReturn(Optional.of(lessons.get(2)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID)).thenReturn(lessons);

        marketplaceService.checkOutLesson(lessons.get(2).getLessonId());

        assertEquals(AttendanceStatus.COMPLETED, lessons.get(2).getAttendanceStatus());
        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        assertEquals(95L, instructionCaptor.getValue().escrowId());
        assertEquals(new BigDecimal("100000.00"), instructionCaptor.getValue().releaseToBeneficiary());
    }

    /**
     * Test Case: IT-LSN-017
     * Title: Reject checkout before check-in.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Today’s lesson has no tutorCheckInAt.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_017_RejectCheckOutBeforeCheckIn.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and no lesson save.
     * Expected: The service returns “Cần điểm danh vào buổi trước khi kết thúc buổi” and does not save.
     * Pre-conditions: Today’s lesson has no tutorCheckInAt.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-017: Reject checkout before check-in.")
    void IT_LSN_017_RejectCheckOutBeforeCheckIn() {
        loginAsTutor();
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lessonToday(TUTOR_ID)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.checkOutLesson(LESSON_ID));

        assertEquals("Cần điểm danh vào buổi trước khi kết thúc buổi", exception.getMessage());
    }

    /**
     * Test Case: IT-LSN-018
     * Title: Reject check-in outside the lesson date.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Lesson date is different from today and tutor owns the lesson.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkInLesson (POST /api/marketplace/lessons/{lessonId}/checkin). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_018_RejectCheckInOutsideLessonDate.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert message contains the stored lesson date.
     * Expected: The service returns the exact-date message including the lesson date and leaves the row unchanged.
     * Pre-conditions: Lesson date is different from today and tutor owns the lesson.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-018: Reject check-in outside the lesson date.")
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

    /**
     * Test Case: IT-LSN-019
     * Title: Include class context in the completion-review notification.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: Tutor completes all lessons; client review is still pending.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_019_CompletionNotificationReferencesClassForFrontendNavigation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification reference fields.
     * Expected: The client notification carries referenceType TUTORING_CLASS and class id 77 for frontend navigation.
     * Pre-conditions: Tutor completes all lessons; client review is still pending.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-019: Include class context in the completion-review notification.")
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

    /**
     * Test Case: IT-LSN-020
     * Title: Complete a lesson after a successful check-in.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout).
     * Input: lessonId=40.
     * Steps:
     *   1. Prepare the fixture: Tutor owns lesson 40 and tutorCheckInAt is already set.
     *   2. Use the input: lessonId=40.
     *   3. Execute MarketplaceServiceImpl.checkOutLesson (POST /api/marketplace/lessons/{lessonId}/checkout). Mapped test: com.tcs.module.marketplace.service.impl.Report52LessonAttendanceITTest#IT_LSN_020_CheckOutCompletesLessonAfterSuccessfulCheckIn.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert final status/timestamp and save.
     * Expected: The lesson becomes COMPLETED with tutorCheckOutAt and is saved.
     * Pre-conditions: Tutor owns lesson 40 and tutorCheckInAt is already set.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-LSN-020: Complete a lesson after a successful check-in.")
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


    

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.test");
        return user;
    }

    private TutoringClass tutoringClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(creator);
        tutoringClass.setTitle("Lớp ôn thi toán");
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStatus(status);
        return tutoringClass;
    }

    private Tutor tutor(User user) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(user.getUserId());
        tutor.setUser(user);
        tutor.setFullName("Gia sư IT");
        return tutor;
    }

    private ClassAssignment assignment(TutoringClass tutoringClass, User tutorUser) {
        Tutor tutor = tutor(tutorUser);

        TutorApplication application = new TutorApplication();
        application.setApplicationId(71L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(ASSIGNMENT_ID);
        assignment.setTutor(tutor);
        assignment.setApplication(application);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        return assignment;
    }

    private EscrowTransaction escrow(Long escrowId, BigDecimal amount) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
    }

    private void configurePrivateHourlyDeal(TutoringClass tutoringClass, BigDecimal hourlyRate) {
        tutoringClass.setTuitionFee(hourlyRate);
        tutoringClass.setDetailsJson("""
                {
                  "subjectIds": ["101"],
                  "subjectFees": {"101": "%s"},
                  "slots": [{"day": "T2", "start": "18:00", "end": "19:00", "subjectId": "101"}]
                }
                """.formatted(hourlyRate.toPlainString()));
    }

    private List<Lesson> lessons(TutoringClass tutoringClass, Tutor tutor, int total, int completed) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(19L);
        slot.setStartTime(LocalTime.of(18, 0));
        slot.setEndTime(LocalTime.of(19, 0));
        return java.util.stream.IntStream.rangeClosed(1, total)
                .mapToObj(sequence -> {
                    Lesson lesson = new Lesson();
                    lesson.setLessonId(1000L + sequence);
                    lesson.setTutoringClass(tutoringClass);
                    lesson.setTutor(tutor);
                    lesson.setSlot(slot);
                    lesson.setSequenceNo(sequence);
                    lesson.setLessonDate(LocalDate.now().plusDays(sequence));
                    lesson.setAttendanceStatus(sequence <= completed ? AttendanceStatus.COMPLETED : AttendanceStatus.PENDING);
                    return lesson;
                })
                .collect(java.util.stream.Collectors.toList());
    }

}
