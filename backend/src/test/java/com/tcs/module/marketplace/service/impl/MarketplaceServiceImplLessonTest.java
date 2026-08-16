package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
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
import com.tcs.module.marketplace.service.impl.LessonReminderService;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.entity.Tutor;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit test module Marketplace — phần buổi học và vòng đời lớp chưa được phủ bởi
 * MarketplaceServiceImplTest sẵn có. Bám bộ test case trong Report_5.1_UnitTest:
 * các sheet checkInLesson, checkOutLesson, unpublishClass, rejectApplicant, registerToClass.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketplaceServiceImplLessonTest {

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

    @InjectMocks private MarketplaceServiceImpl service;

    private Tutor tutor;
    private User clientUser;
    private TutoringClass tutoringClass;

    @BeforeEach
    void setUp() {
        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia su 1");

        clientUser = new User();
        clientUser.setUserId(CLIENT_USER_ID);

        tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setTitle("Toan 9");
        tutoringClass.setCreator(clientUser);
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
    }

    private void loginAsTutor() {
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
    }

    private void loginAsClient() {
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
    }

    /** Buổi học của gia sư đang đăng nhập, diễn ra hôm nay. */
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

    // ===================================================================
    //  Sheet: checkInLesson
    // ===================================================================
    @Nested
    @DisplayName("checkInLesson")
    class CheckInLesson {

        @Test
        @DisplayName("UTCID01 (N) - Buổi hôm nay, chưa check-in -> ghi nhận giờ vào")
        void utcid01_checkInSuccessfully() {
            loginAsTutor();
            Lesson lesson = lessonToday(TUTOR_ID);
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            service.checkInLesson(LESSON_ID);

            org.junit.jupiter.api.Assertions.assertNotNull(lesson.getTutorCheckInAt());
            verify(lessonRepository).save(lesson);
        }

        @Test
        @DisplayName("UTCID02 (A) - Buổi đã điểm danh xong -> 'Buổi học này đã điểm danh xong'")
        void utcid02_alreadyCompleted() {
            loginAsTutor();
            Lesson lesson = lessonToday(TUTOR_ID);
            lesson.setAttendanceStatus(AttendanceStatus.COMPLETED);
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.checkInLesson(LESSON_ID));
            assertEquals("Buổi học này đã điểm danh xong", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Đã check-in rồi -> 'Bạn đã điểm danh vào buổi này rồi'")
        void utcid03_alreadyCheckedIn() {
            loginAsTutor();
            Lesson lesson = lessonToday(TUTOR_ID);
            lesson.setTutorCheckInAt(LocalDateTime.now().minusHours(1));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.checkInLesson(LESSON_ID));
            assertEquals("Bạn đã điểm danh vào buổi này rồi", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Buổi của gia sư khác -> ForbiddenException")
        void utcid04_lessonOfAnotherTutor() {
            loginAsTutor();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lessonToday(OTHER_TUTOR_ID)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.checkInLesson(LESSON_ID));
            assertEquals("Không có quyền điểm danh buổi học của gia sư khác", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Buổi không tồn tại -> ResourceNotFoundException")
        void utcid05_lessonNotFound() {
            loginAsTutor();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.checkInLesson(LESSON_ID));
            assertEquals("Không tìm thấy buổi học", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (B) - Buổi của ngày khác -> chặn theo ngày")
        void utcid06_lessonNotToday() {
            loginAsTutor();
            Lesson lesson = lessonToday(TUTOR_ID);
            lesson.setLessonDate(LocalDate.now().minusDays(1));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.checkInLesson(LESSON_ID));
            assertTrue(ex.getMessage().contains("Chỉ điểm danh được trong ngày diễn ra buổi học"),
                    "Thông báo phải nêu rõ ngày buổi học: " + ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: checkOutLesson
    // ===================================================================
    @Nested
    @DisplayName("checkOutLesson")
    class CheckOutLesson {

        @Test
        @DisplayName("UTCID01 (N) - Đã check-in -> check-out, buổi chuyển COMPLETED")
        void utcid01_checkOutSuccessfully() {
            loginAsTutor();
            Lesson lesson = lessonToday(TUTOR_ID);
            lesson.setTutorCheckInAt(LocalDateTime.now().minusHours(1));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            service.checkOutLesson(LESSON_ID);

            assertEquals(AttendanceStatus.COMPLETED, lesson.getAttendanceStatus());
            org.junit.jupiter.api.Assertions.assertNotNull(lesson.getTutorCheckOutAt());
        }

        @Test
        @DisplayName("UTCID02 (A) - Chưa check-in -> 'Cần điểm danh vào buổi trước khi kết thúc buổi'")
        void utcid02_notCheckedIn() {
            loginAsTutor();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lessonToday(TUTOR_ID)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.checkOutLesson(LESSON_ID));
            assertEquals("Cần điểm danh vào buổi trước khi kết thúc buổi", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Đã check-out rồi -> 'Buổi học này đã kết thúc rồi'")
        void utcid03_alreadyCheckedOut() {
            loginAsTutor();
            Lesson lesson = lessonToday(TUTOR_ID);
            lesson.setTutorCheckInAt(LocalDateTime.now().minusHours(2));
            lesson.setTutorCheckOutAt(LocalDateTime.now().minusHours(1));
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.checkOutLesson(LESSON_ID));
            assertEquals("Buổi học này đã kết thúc rồi", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Buổi của gia sư khác -> ForbiddenException")
        void utcid04_lessonOfAnotherTutor() {
            loginAsTutor();
            when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lessonToday(OTHER_TUTOR_ID)));

            assertThrows(ForbiddenException.class, () -> service.checkOutLesson(LESSON_ID));
        }
    }

    // ===================================================================
    //  Sheet: unpublishClass
    // ===================================================================
    @Nested
    @DisplayName("unpublishClass")
    class UnpublishClass {

        @Test
        @DisplayName("UTCID01 (N) - Lớp OPEN, chưa ai ứng tuyển -> gỡ đăng thành công")
        void utcid01_unpublishSuccessfully() {
            loginAsClient();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                    CLASS_ID, com.tcs.module.marketplace.enums.TutorApplicationStatus.REJECTED))
                    .thenReturn(0L);
            when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> i.getArgument(0));

            service.unpublishClass(CLASS_ID);

            assertEquals(TutoringClassStatus.DRAFT, tutoringClass.getStatus());
        }

        @Test
        @DisplayName("UTCID02 (A) - Không phải chủ lớp -> ForbiddenException")
        void utcid02_notOwner() {
            User other = new User();
            other.setUserId(999L);
            tutoringClass.setCreator(other);
            loginAsClient();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.unpublishClass(CLASS_ID));
            assertEquals("Không có quyền gỡ đăng lớp này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Lớp không ở trạng thái OPEN -> 'Chỉ có thể gỡ đăng lớp đang mở'")
        void utcid03_classNotOpen() {
            tutoringClass.setStatus(TutoringClassStatus.MATCHED);
            loginAsClient();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.unpublishClass(CLASS_ID));
            assertEquals("Chỉ có thể gỡ đăng lớp đang mở", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Đã có gia sư ứng tuyển -> không cho gỡ đăng")
        void utcid04_hasApplicants() {
            loginAsClient();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                    CLASS_ID, com.tcs.module.marketplace.enums.TutorApplicationStatus.REJECTED))
                    .thenReturn(1L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.unpublishClass(CLASS_ID));
            assertEquals("Lớp đã có gia sư ứng tuyển nên không thể gỡ đăng", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: searchTutors  (tìm kiếm gia sư)
    // ===================================================================
    @Nested
    @DisplayName("searchTutors")
    class SearchTutors {

        private Tutor tutorNamed(Long id, String name, String bio) {
            User u = new User();
            u.setUserId(1000L + id);
            Tutor t = new Tutor();
            t.setTutorId(id);
            t.setUser(u);
            t.setFullName(name);
            t.setBio(bio);
            t.setExperienceYears(1);
            t.setHourlyRate(java.math.BigDecimal.valueOf(100000));
            t.setRatingAvg(java.math.BigDecimal.valueOf(4.5));
            t.setVerificationStatus(com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            return t;
        }

        @Test
        @DisplayName("UTCID01 (N) - Tìm theo từ khoá khớp họ tên -> trả về gia sư tương ứng")
        void utcid01_searchByName() {
            when(tutorRepository.findAll()).thenReturn(List.of(
                    tutorNamed(1L, "Nguyen Van Toan", "Day Toan THCS"),
                    tutorNamed(2L, "Tran Thi Anh", "Day Tieng Anh")));

            var result = service.searchTutors("toan", null);

            assertEquals(1, result.size());
            assertEquals("Nguyen Van Toan", result.get(0).getFullName());
        }

        @Test
        @DisplayName("UTCID02 (N) - Từ khoá rỗng -> trả về toàn bộ gia sư")
        void utcid02_emptyKeywordReturnsAll() {
            when(tutorRepository.findAll()).thenReturn(List.of(
                    tutorNamed(1L, "Nguyen Van Toan", "Day Toan"),
                    tutorNamed(2L, "Tran Thi Anh", "Day Tieng Anh")));

            var result = service.searchTutors("", null);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("UTCID03 (A) - Lọc theo môn học (subjectId) -> chỉ trả gia sư dạy môn đó")
        void utcid03_filterBySubject() {
            // Đặc tả: API nhận tham số subjectId để lọc theo môn.
            // Dữ liệu: chỉ 1 trong 2 gia sư dạy môn có id = 1.
            when(tutorRepository.findAll()).thenReturn(List.of(
                    tutorNamed(1L, "Nguyen Van Toan", "Day Toan THCS"),
                    tutorNamed(2L, "Tran Thi Anh", "Day Tieng Anh")));

            var result = service.searchTutors(null, 1L);

            assertEquals(1, result.size(),
                    "Lọc theo subjectId phải thu hẹp kết quả, nhưng hiện trả về toàn bộ gia sư");
        }
    }

    // ===================================================================
    //  Sheet: registerToClass
    // ===================================================================
    @Nested
    @DisplayName("registerToClass")
    class RegisterToClass {

        @Test
        @DisplayName("UTCID01 (A) - Lớp chưa mở đăng ký -> 'Lớp chưa mở đăng ký'")
        void utcid01_classNotOpen() {
            tutoringClass.setStatus(TutoringClassStatus.DRAFT);
            loginAsClient();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerToClass(CLASS_ID));
            assertEquals("Lớp chưa mở đăng ký", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID02 (A) - Tài khoản không phải gia sư/phụ huynh -> ForbiddenException")
        void utcid02_neitherTutorNorClient() {
            loginAsClient();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(tutorRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.empty());
            when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.empty());

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.registerToClass(CLASS_ID));
            assertEquals("Chỉ gia sư hoặc phụ huynh/học viên mới đăng ký lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Lớp không tồn tại -> ResourceNotFoundException")
        void utcid03_classNotFound() {
            loginAsClient();
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.registerToClass(CLASS_ID));
        }

        @Test
        @DisplayName("UTCID04 (A) - Gia sư chưa xác minh hồ sơ -> VerificationRequiredException")
        void utcid04_tutorNotVerified() {
            tutor.setVerificationStatus(ProfileVerificationStatus.UNDER_VERIFY);
            tutoringClass.setClassType(ClassType.PRIVATE);
            when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
            User tutorUser = tutor.getUser();
            when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));

            assertThrows(com.tcs.exception.VerificationRequiredException.class,
                    () -> service.registerToClass(CLASS_ID));
        }

        @Test
        @DisplayName("UTCID05 (A) - Gia sư đang có đơn chờ duyệt -> 'Bạn đã đăng ký lớp này rồi'")
        void utcid05_tutorAlreadyApplied() {
            tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
            tutoringClass.setClassType(ClassType.PRIVATE);
            when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
            when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutor.getUser()));
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
            when(tutorApplicationRepository
                    .existsByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, TUTOR_ID)).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.registerToClass(CLASS_ID));
            assertEquals("Bạn đã đăng ký lớp này rồi", ex.getMessage());
        }

        /**
         * UTCID06 (A) - DEF-10.
         * Đặc tả (và applyToClass đã làm đúng): đơn bị TỪ CHỐI thì gia sư được nộp lại.
         * applyToClass chặn bằng {@code existing.getStatus() != REJECTED};
         * registerToClass lại chặn bằng existsByTutoringClass_ClassIdAndTutor_TutorId,
         * không phân biệt trạng thái -> gia sư bị từ chối một lần là khoá vĩnh viễn trên đường này.
         */
        @Test
        @DisplayName("UTCID06 (A) - Đơn cũ đã bị TỪ CHỐI -> phải cho nộp lại [DEF-10]")
        void utcid06_rejectedTutorMayReapply() {
            tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
            tutoringClass.setClassType(ClassType.PRIVATE);
            when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
            when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutor.getUser()));
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
            // Chỉ tồn tại một đơn cũ và nó đã bị REJECTED.
            when(tutorApplicationRepository
                    .existsByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, TUTOR_ID)).thenReturn(true);
            TutorApplication rejected = new TutorApplication();
            rejected.setStatus(TutorApplicationStatus.REJECTED);
            when(tutorApplicationRepository
                    .findFirstByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, TUTOR_ID))
                    .thenReturn(Optional.of(rejected));
            when(tutorApplicationRepository.save(any(TutorApplication.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String result = org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> service.registerToClass(CLASS_ID),
                    "Đơn cũ đã bị từ chối thì phải cho nộp lại như applyToClass, "
                            + "nhưng registerToClass vẫn báo 'Bạn đã đăng ký lớp này rồi'");
            assertEquals("Đã gửi đơn ứng tuyển dạy lớp. Vui lòng chờ trung tâm/phụ huynh duyệt.", result);
        }
    }
}
