package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
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
import com.tcs.module.marketplace.service.RescheduleService;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit test module Marketplace — vong doi lop ca nhan (tao / sua / dang lop)
 * va viec gia su nhan hoac tu choi loi moi nhan lop.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: cac sheet mkCreateClass, mkUpdateClass,
 * mkPublishClass, acceptAssignment va declineAssignment.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketplaceServiceImplClassTest {

    private static final Long CLIENT_USER_ID = 100L;
    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;
    private static final Long OTHER_TUTOR_ID = 21L;
    private static final Long CLASS_ID = 500L;
    private static final Long ASSIGNMENT_ID = 700L;
    private static final Long APPLICATION_ID = 600L;

    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private CccdService cccdService;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private TutorRepository tutorRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private EscrowService escrowService;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private RescheduleService rescheduleService;
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
    @Mock private OtpService otpService;
    @Mock private com.tcs.module.notification.service.EmailService contractEmailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private MarketplaceServiceImpl service;

    private User clientUser;
    private User tutorUser;
    private Tutor tutor;
    private TutoringClass tutoringClass;

    @BeforeEach
    void setUp() {
        clientUser = new User();
        clientUser.setUserId(CLIENT_USER_ID);
        clientUser.setEmail("phuhuynh@tcs.vn");

        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("giasu@tcs.vn");

        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia su 1");

        tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setTitle("Toan 9");
        tutoringClass.setCreator(clientUser);
        tutoringClass.setStatus(TutoringClassStatus.DRAFT);
        tutoringClass.setBudget(new BigDecimal("1000000"));

        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(i -> {
            TutoringClass c = i.getArgument(0);
            if (c.getClassId() == null) {
                c.setClassId(CLASS_ID);
            }
            return c;
        });

        com.tcs.module.catalog.entity.Subject subject = new com.tcs.module.catalog.entity.Subject();
        subject.setSubjectId(1L);
        subject.setSubjectName("Toan");
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
    }

    /** Dang nhap bang tai khoan phu huynh co ho so Client. */
    private void loginAsClient() {
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(new Client()));
    }

    /** Dang nhap bang tai khoan gia su. */
    private void loginAsTutor() {
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
    }

    private CreateClassRequest classRequest() {
        CreateClassRequest request = new CreateClassRequest();
        request.setSubjectId(1L);
        request.setTitle("Toan 9 - ca toi");
        request.setBudget(new BigDecimal("1500000"));
        return request;
    }

    // ===================================================================
    //  Sheet: mkCreateClass
    // ===================================================================
    @Nested
    @DisplayName("mkCreateClass")
    class MkCreateClass {

        @Test
        @DisplayName("UTCID01 (N) - CLIENT hop le, co subjectId -> luu lop trang thai DRAFT kem audit log")
        void utcid01_createWithSubjectId() {
            loginAsClient();

            service.createClass(classRequest());

            ArgumentCaptor<TutoringClass> captor = ArgumentCaptor.forClass(TutoringClass.class);
            verify(tutoringClassRepository).save(captor.capture());
            TutoringClass saved = captor.getValue();
            assertEquals(TutoringClassStatus.DRAFT, saved.getStatus());
            assertEquals(CLIENT_USER_ID, saved.getCreator().getUserId());
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq(CLIENT_USER_ID),
                    org.mockito.ArgumentMatchers.eq("CREATE_CLASS"),
                    org.mockito.ArgumentMatchers.eq("TutoringClass"),
                    anyLong(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (N) - subjectId = null nhung co detailsJson -> van tao duoc lop")
        void utcid02_createWithDetailsJsonOnly() {
            loginAsClient();
            CreateClassRequest request = classRequest();
            request.setSubjectId(null);
            request.setDetailsJson("{\"subjectIds\":[\"1\"]}");

            service.createClass(request);

            verify(tutoringClassRepository).save(any(TutoringClass.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - Khong co nguoi dung dang nhap -> 'Không tìm thấy người dùng'")
        void utcid03_noAuthenticatedUser() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.createClass(classRequest()));
            assertEquals("Không tìm thấy người dùng", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - Tai khoan bi han che tinh nang CLASS_POSTING -> ForbiddenException")
        void utcid04_classPostingRestricted() {
            loginAsClient();
            org.mockito.Mockito.doThrow(new ForbiddenException(
                            "Tính năng CLASS_POSTING đang bị hạn chế trên tài khoản này."))
                    .when(penaltyAccessService).requireFeature(CLIENT_USER_ID, "CLASS_POSTING");

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.createClass(classRequest()));
            assertEquals("Tính năng CLASS_POSTING đang bị hạn chế trên tài khoản này.", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (A) - Nguoi dang nhap khong co ho so khach hang -> 'Chỉ phụ huynh/khách hàng mới tạo lớp học'")
        void utcid05_notAClient() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
            when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.empty());

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.createClass(classRequest()));
            assertEquals("Chỉ phụ huynh/khách hàng mới tạo lớp học", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Khong co subjectId lan detailsJson -> 'Vui lòng chọn môn học'")
        void utcid06_noSubjectAtAll() {
            loginAsClient();
            CreateClassRequest request = classRequest();
            request.setSubjectId(null);
            request.setDetailsJson("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createClass(request));
            assertEquals("Vui lòng chọn môn học", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID07 (B) - budget = null -> mac dinh luu 0")
        void utcid07_nullBudgetDefaultsToZero() {
            loginAsClient();
            CreateClassRequest request = classRequest();
            request.setBudget(null);

            service.createClass(request);

            ArgumentCaptor<TutoringClass> captor = ArgumentCaptor.forClass(TutoringClass.class);
            verify(tutoringClassRepository).save(captor.capture());
            assertEquals(BigDecimal.ZERO, captor.getValue().getBudget());
        }

        @Test
        @DisplayName("UTCID08 (B) - budget = 0 (dung can duoi) -> luu dung 0")
        void utcid08_zeroBudgetIsKept() {
            loginAsClient();
            CreateClassRequest request = classRequest();
            request.setBudget(BigDecimal.ZERO);

            service.createClass(request);

            ArgumentCaptor<TutoringClass> captor = ArgumentCaptor.forClass(TutoringClass.class);
            verify(tutoringClassRepository).save(captor.capture());
            assertEquals(BigDecimal.ZERO, captor.getValue().getBudget());
        }
    }

    // ===================================================================
    //  Sheet: mkUpdateClass
    // ===================================================================
    @Nested
    @DisplayName("mkUpdateClass")
    class MkUpdateClass {

        @BeforeEach
        void loginAsOwner() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        }

        private void givenApplicationCount(long count) {
            when(tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                    CLASS_ID, TutorApplicationStatus.REJECTED)).thenReturn(count);
        }

        @Test
        @DisplayName("UTCID01 (N) - Chu lop sua lop dang DRAFT -> ap dung thay doi va luu")
        void utcid01_updateDraftClass() {
            givenApplicationCount(0);
            CreateClassRequest request = classRequest();
            request.setTitle("Toan 9 - ca sang");

            service.updateClass(CLASS_ID, request);

            assertEquals("Toan 9 - ca sang", tutoringClass.getTitle());
            verify(tutoringClassRepository).save(tutoringClass);
        }

        @Test
        @DisplayName("UTCID02 (N) - Lop OPEN va chua co don ung tuyen nao -> van sua duoc")
        void utcid02_updateOpenClassWithoutApplications() {
            tutoringClass.setStatus(TutoringClassStatus.OPEN);
            givenApplicationCount(0);

            service.updateClass(CLASS_ID, classRequest());

            verify(tutoringClassRepository).save(tutoringClass);
        }

        @Test
        @DisplayName("UTCID03 (A) - classId khong khop lop nao -> ResourceNotFoundException")
        void utcid03_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateClass(CLASS_ID, classRequest()));
        }

        @Test
        @DisplayName("UTCID04 (A) - Nguoi goi khong phai chu lop -> 'Không có quyền sửa lớp này'")
        void utcid04_notOwner() {
            when(authHelper.currentUserId()).thenReturn(999L);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.updateClass(CLASS_ID, classRequest()));
            assertEquals("Không có quyền sửa lớp này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Lop OPEN da co don chua bi tu choi -> 'Lớp đã có gia sư ứng tuyển nên không thể sửa nữa'")
        void utcid05_openClassWithLiveApplication() {
            tutoringClass.setStatus(TutoringClassStatus.OPEN);
            givenApplicationCount(1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateClass(CLASS_ID, classRequest()));
            assertEquals("Lớp đã có gia sư ứng tuyển nên không thể sửa nữa", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID06 (A) - Lop o trang thai khac DRAFT/OPEN -> 'Lớp ở trạng thái này không thể sửa'")
        void utcid06_statusNotEditable() {
            tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
            givenApplicationCount(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateClass(CLASS_ID, classRequest()));
            assertEquals("Lớp ở trạng thái này không thể sửa", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Khong co subjectId lan detailsJson -> 'Vui lòng chọn môn học'")
        void utcid07_noSubjectAtAll() {
            givenApplicationCount(0);
            CreateClassRequest request = classRequest();
            request.setSubjectId(null);
            request.setDetailsJson("  ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.updateClass(CLASS_ID, request));
            assertEquals("Vui lòng chọn môn học", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (B) - budget = null -> giu nguyen ngan sach cu")
        void utcid08_nullBudgetKeepsExisting() {
            givenApplicationCount(0);
            CreateClassRequest request = classRequest();
            request.setBudget(null);

            service.updateClass(CLASS_ID, request);

            assertEquals(new BigDecimal("1000000"), tutoringClass.getBudget(),
                    "Khong gui budget thi giu nguyen gia tri cu");
        }

        @Test
        @DisplayName("UTCID09 (B) - Lop OPEN chi con don da bi REJECTED -> khong tinh vao khoa sua")
        void utcid09_rejectedApplicationDoesNotLock() {
            tutoringClass.setStatus(TutoringClassStatus.OPEN);
            // countByTutoringClass_ClassIdAndStatusNot(REJECTED) = 0 vi don duy nhat da REJECTED.
            givenApplicationCount(0);

            service.updateClass(CLASS_ID, classRequest());

            verify(tutoringClassRepository).save(tutoringClass);
        }
    }

    // ===================================================================
    //  Sheet: mkPublishClass
    // ===================================================================
    @Nested
    @DisplayName("mkPublishClass")
    class MkPublishClass {

        @BeforeEach
        void loginAsOwner() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        }

        @Test
        @DisplayName("UTCID01 (N) - Chu lop dang lop -> status = OPEN, expiresAt = now + 30 ngay, ghi audit log")
        void utcid01_publishSuccessfully() {
            service.publishClass(CLASS_ID);

            assertEquals(TutoringClassStatus.OPEN, tutoringClass.getStatus());
            assertNotNull(tutoringClass.getExpiresAt());
            assertEquals(java.time.LocalDate.now().plusDays(30),
                    tutoringClass.getExpiresAt().toLocalDate(),
                    "Han hien thi phai la 30 ngay ke tu luc dang");
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq(CLIENT_USER_ID),
                    org.mockito.ArgumentMatchers.eq("PUBLISH_CLASS"),
                    org.mockito.ArgumentMatchers.eq("TutoringClass"),
                    anyLong(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - classId khong khop lop nao -> ResourceNotFoundException")
        void utcid02_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.publishClass(CLASS_ID));
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi khong phai chu lop -> 'Không có quyền đăng lớp này'")
        void utcid03_notOwner() {
            when(authHelper.currentUserId()).thenReturn(999L);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.publishClass(CLASS_ID));
            assertEquals("Không có quyền đăng lớp này", ex.getMessage());
            verify(tutoringClassRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (B) - Dang lai lop dang OPEN -> lam moi han hien thi 30 ngay")
        void utcid04_republishRefreshesDeadline() {
            tutoringClass.setStatus(TutoringClassStatus.OPEN);
            tutoringClass.setExpiresAt(java.time.LocalDateTime.now().plusDays(2));

            service.publishClass(CLASS_ID);

            assertEquals(TutoringClassStatus.OPEN, tutoringClass.getStatus());
            assertEquals(java.time.LocalDate.now().plusDays(30),
                    tutoringClass.getExpiresAt().toLocalDate(),
                    "Dang lai phai lam moi han hien thi");
        }

        @Test
        @DisplayName("UTCID05 (B) - Lop dang DRAFT -> chuyen sang OPEN")
        void utcid05_publishFromDraft() {
            tutoringClass.setStatus(TutoringClassStatus.DRAFT);

            service.publishClass(CLASS_ID);

            assertEquals(TutoringClassStatus.OPEN, tutoringClass.getStatus());
        }
    }

    // ===================================================================
    //  Sheet: acceptAssignment
    // ===================================================================
    @Nested
    @DisplayName("acceptAssignment")
    class AcceptAssignment {

        private ClassAssignment assignment;

        @BeforeEach
        void initAssignment() {
            loginAsTutor();
            assignment = assignmentOf(TUTOR_ID, true);
            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(lessonRepository.countByTutoringClass_ClassId(CLASS_ID)).thenReturn(1L);
        }

        /** Loi moi nhan lop PENDING cua gia su {@code ownerTutorId}. */
        private ClassAssignment assignmentOf(Long ownerTutorId, boolean withApplication) {
            Tutor owner = new Tutor();
            owner.setTutorId(ownerTutorId);
            owner.setUser(tutorUser);

            ClassAssignment a = new ClassAssignment();
            a.setAssignmentId(ASSIGNMENT_ID);
            a.setTutor(owner);
            a.setStatus(ClassAssignmentStatus.PENDING);
            a.setTutorSignedAt(java.time.LocalDateTime.now().minusDays(1));
            a.setClientSignedAt(java.time.LocalDateTime.now().minusDays(1));
            if (withApplication) {
                TutorApplication application = new TutorApplication();
                application.setApplicationId(APPLICATION_ID);
                application.setTutoringClass(tutoringClass);
                application.setTutor(owner);
                a.setApplication(application);
            }
            return a;
        }

        private void givenEscrowExists() {
            EscrowTransaction escrow = new EscrowTransaction();
            escrow.setEscrowId(71L);
            when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                    .thenReturn(Optional.of(escrow));
        }

        @Test
        @DisplayName("UTCID01 (N) - Loi moi PENDING, hai ben da ky, co escrow -> ACTIVE va lop chuyen IN_PROGRESS")
        void utcid01_acceptSuccessfully() {
            givenEscrowExists();

            service.acceptAssignment(ASSIGNMENT_ID);

            assertEquals(ClassAssignmentStatus.ACTIVE, assignment.getStatus());
            assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
            verify(classAssignmentRepository).save(assignment);
            verify(tutoringClassRepository).save(tutoringClass);
        }

        @Test
        @DisplayName("UTCID02 (A) - assignmentId khong khop loi moi nao -> 'Không tìm thấy lời mời nhận lớp'")
        void utcid02_assignmentNotFound() {
            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Không tìm thấy lời mời nhận lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Loi moi cua gia su khac -> 'Không có quyền xử lý lời mời của gia sư khác'")
        void utcid03_assignmentOfAnotherTutor() {
            when(classAssignmentRepository.findById(ASSIGNMENT_ID))
                    .thenReturn(Optional.of(assignmentOf(OTHER_TUTOR_ID, true)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Không có quyền xử lý lời mời của gia sư khác", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Loi moi khong gan don ung tuyen nao -> 'Lời mời không gắn với lớp nào'")
        void utcid04_assignmentWithoutApplication() {
            when(classAssignmentRepository.findById(ASSIGNMENT_ID))
                    .thenReturn(Optional.of(assignmentOf(TUTOR_ID, false)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Lời mời không gắn với lớp nào", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Loi moi khong con PENDING -> 'Lời mời này đã được xử lý trước đó'")
        void utcid05_assignmentNotPending() {
            assignment.setStatus(ClassAssignmentStatus.ACTIVE);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Lời mời này đã được xử lý trước đó", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Gia su chua ky hop dong -> 'Vui lòng ký hợp đồng và thanh toán escrow trước khi nhận lớp'")
        void utcid06_tutorNotSigned() {
            assignment.setTutorSignedAt(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Vui lòng ký hợp đồng và thanh toán escrow trước khi nhận lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Khach hang chua ky hop dong -> cung thong bao chan")
        void utcid07_clientNotSigned() {
            assignment.setClientSignedAt(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Vui lòng ký hợp đồng và thanh toán escrow trước khi nhận lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Hai ben da ky nhung chua co escrow -> 'Chưa có escrow hợp lệ cho lớp này'")
        void utcid08_noEscrow() {
            when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                    .thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Chưa có escrow hợp lệ cho lớp này", ex.getMessage());
            verify(classAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID09 (A) - Nguoi goi khong co ho so gia su -> requireTutor chan lai")
        void utcid09_callerIsNotATutor() {
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.acceptAssignment(ASSIGNMENT_ID));
            assertEquals("Không tìm thấy hồ sơ gia sư", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: declineAssignment
    // ===================================================================
    @Nested
    @DisplayName("declineAssignment")
    class DeclineAssignment {

        private ClassAssignment assignment;
        private TutorApplication declinedApplication;

        @BeforeEach
        void initAssignment() {
            loginAsTutor();
            declinedApplication = new TutorApplication();
            declinedApplication.setApplicationId(APPLICATION_ID);
            declinedApplication.setTutoringClass(tutoringClass);
            declinedApplication.setTutor(tutor);
            declinedApplication.setStatus(TutorApplicationStatus.ACCEPTED);

            assignment = new ClassAssignment();
            assignment.setAssignmentId(ASSIGNMENT_ID);
            assignment.setTutor(tutor);
            assignment.setStatus(ClassAssignmentStatus.PENDING);
            assignment.setApplication(declinedApplication);

            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        }

        private TutorApplication otherApplication(Long id) {
            Tutor other = new Tutor();
            other.setTutorId(OTHER_TUTOR_ID);
            TutorApplication app = new TutorApplication();
            app.setApplicationId(id);
            app.setTutoringClass(tutoringClass);
            app.setTutor(other);
            app.setStatus(TutorApplicationStatus.REJECTED);
            app.setReviewedAt(java.time.LocalDateTime.now().minusDays(1));
            return app;
        }

        @Test
        @DisplayName("UTCID01 (N) - Gia su tu choi loi moi, lop con don khac -> don khac tro lai SUBMITTED, lop mo lai")
        void utcid01_declineWithOtherApplications() {
            TutorApplication other = otherApplication(601L);
            when(tutorApplicationRepository.findByTutoringClass_ClassId(CLASS_ID))
                    .thenReturn(List.of(declinedApplication, other));

            service.declineAssignment(ASSIGNMENT_ID);

            assertEquals(ClassAssignmentStatus.DECLINED, assignment.getStatus());
            assertEquals(TutorApplicationStatus.REJECTED, declinedApplication.getStatus());
            assertEquals(TutorApplicationStatus.SUBMITTED, other.getStatus());
            assertNull(other.getReviewedAt(), "Don khac phai duoc dua ve trang thai chua duyet");
            assertEquals(TutoringClassStatus.OPEN, tutoringClass.getStatus());
            assertEquals(java.time.LocalDate.now().plusDays(30),
                    tutoringClass.getExpiresAt().toLocalDate());
        }

        @Test
        @DisplayName("UTCID02 (A) - assignmentId khong khop loi moi nao -> 'Không tìm thấy lời mời nhận lớp'")
        void utcid02_assignmentNotFound() {
            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.declineAssignment(ASSIGNMENT_ID));
            assertEquals("Không tìm thấy lời mời nhận lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Loi moi cua gia su khac -> 'Không có quyền xử lý lời mời của gia sư khác'")
        void utcid03_assignmentOfAnotherTutor() {
            Tutor other = new Tutor();
            other.setTutorId(OTHER_TUTOR_ID);
            assignment.setTutor(other);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.declineAssignment(ASSIGNMENT_ID));
            assertEquals("Không có quyền xử lý lời mời của gia sư khác", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Loi moi khong gan don ung tuyen nao -> 'Lời mời không gắn với lớp nào'")
        void utcid04_assignmentWithoutApplication() {
            assignment.setApplication(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.declineAssignment(ASSIGNMENT_ID));
            assertEquals("Lời mời không gắn với lớp nào", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Loi moi khong con PENDING -> 'Lời mời này đã được xử lý trước đó'")
        void utcid05_assignmentNotPending() {
            assignment.setStatus(ClassAssignmentStatus.DECLINED);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.declineAssignment(ASSIGNMENT_ID));
            assertEquals("Lời mời này đã được xử lý trước đó", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (B) - Lop chi co dung mot don la don bi tu choi -> lop van mo lai voi han moi")
        void utcid06_onlyOneApplication() {
            when(tutorApplicationRepository.findByTutoringClass_ClassId(CLASS_ID))
                    .thenReturn(List.of(declinedApplication));

            service.declineAssignment(ASSIGNMENT_ID);

            assertEquals(ClassAssignmentStatus.DECLINED, assignment.getStatus());
            assertEquals(TutorApplicationStatus.REJECTED, declinedApplication.getStatus());
            assertEquals(TutoringClassStatus.OPEN, tutoringClass.getStatus());
            assertNotNull(tutoringClass.getExpiresAt());
        }
    }

    // ===================================================================
    //  Sheet: mkConfirmCompletion (gia su xac nhan hoan thanh lop ca nhan)
    // ===================================================================
    @Nested
    @DisplayName("mkConfirmCompletion")
    class MkConfirmCompletion {

        private ClassAssignment activeAssignment;

        @BeforeEach
        void initCompletion() {
            loginAsTutor();
            when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
            tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.PRIVATE);
            tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);

            TutorApplication application = new TutorApplication();
            application.setApplicationId(APPLICATION_ID);
            application.setTutoringClass(tutoringClass);
            application.setTutor(tutor);

            activeAssignment = new ClassAssignment();
            activeAssignment.setAssignmentId(ASSIGNMENT_ID);
            activeAssignment.setTutor(tutor);
            activeAssignment.setApplication(application);
            activeAssignment.setStatus(ClassAssignmentStatus.ACTIVE);

            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(activeAssignment));
            givenLessons(attendedLesson(1, com.tcs.module.marketplace.enums.AttendanceStatus.COMPLETED));
        }

        private com.tcs.module.marketplace.entity.Lesson attendedLesson(
                int seq, com.tcs.module.marketplace.enums.AttendanceStatus status) {
            var lesson = new com.tcs.module.marketplace.entity.Lesson();
            lesson.setLessonId(1000L + seq);
            lesson.setTutoringClass(tutoringClass);
            lesson.setTutor(tutor);
            lesson.setSequenceNo(seq);
            lesson.setLessonDate(java.time.LocalDate.now().minusDays(10 - seq));
            lesson.setAttendanceStatus(status);
            return lesson;
        }

        private void givenLessons(com.tcs.module.marketplace.entity.Lesson... lessons) {
            when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(lessons));
        }

        @Test
        @DisplayName("UTCID01 (N) - Gia su xac nhan, hoc vien chua danh gia -> ghi tutorCompletedAt, lop van IN_PROGRESS")
        void utcid01_tutorConfirmsFirst() {
            when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(false);

            String message = service.confirmClassCompletion(CLASS_ID);

            assertNotNull(activeAssignment.getTutorCompletedAt());
            assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
            assertEquals("Đã gửi yêu cầu tới học viên. Lớp sẽ đóng và giải ngân sau khi học viên đánh giá gia sư.",
                    message);
        }

        @Test
        @DisplayName("UTCID02 (N) - Hoc vien da danh gia -> dong lop va giai ngan ky quy")
        void utcid02_clientAlreadyReviewed() {
            when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(true);
            when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                    .thenReturn(Optional.empty());
            when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            String message = service.confirmClassCompletion(CLASS_ID);

            assertNotNull(activeAssignment.getClientCompletedAt());
            assertEquals(TutoringClassStatus.COMPLETED, tutoringClass.getStatus());
            assertEquals("Lớp đã hoàn thành. Học phí ký quỹ đã được giải ngân cho gia sư.", message);
        }

        @Test
        @DisplayName("UTCID03 (A) - Khong co nguoi dung dang nhap -> 'Không tìm thấy người dùng'")
        void utcid03_noAuthenticatedUser() {
            when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Không tìm thấy người dùng", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - classId khong khop lop nao -> ResourceNotFoundException")
        void utcid04_classNotFound() {
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.confirmClassCompletion(CLASS_ID));
        }

        @Test
        @DisplayName("UTCID05 (A) - Lop thuoc trung tam -> 'Chức năng hoàn thành lớp chỉ áp dụng cho lớp gia sư riêng.'")
        void utcid05_centerClass() {
            tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.CENTER);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Chức năng hoàn thành lớp chỉ áp dụng cho lớp gia sư riêng.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Lop chua co luot gan ACTIVE -> 'Lớp chưa có gia sư nhận, không thể hoàn thành.'")
        void utcid06_noActiveAssignment() {
            when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                    CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Lớp chưa có gia sư nhận, không thể hoàn thành.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Nguoi goi la khach hang chu lop chu khong phai gia su -> chan")
        void utcid07_callerIsTheClient() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Chỉ gia sư mới có thể đánh dấu hoàn thành lớp.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Lop da COMPLETED -> 'Lớp đã hoàn thành.'")
        void utcid08_classAlreadyCompleted() {
            tutoringClass.setStatus(TutoringClassStatus.COMPLETED);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Lớp đã hoàn thành.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Lop khong o trang thai IN_PROGRESS -> 'Chỉ hoàn thành khi lớp đang diễn ra.'")
        void utcid09_classNotInProgress() {
            tutoringClass.setStatus(TutoringClassStatus.MATCHED);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Chỉ hoàn thành khi lớp đang diễn ra.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (A) - Lop chua co buoi hoc nao -> 'Lớp chưa có buổi học nào để xác nhận hoàn thành.'")
        void utcid10_noLessons() {
            when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Lớp chưa có buổi học nào để xác nhận hoàn thành.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID11 (A) - Buoi hoc cuoi chua diem danh -> 'Cần điểm danh buổi học cuối cùng ...'")
        void utcid11_lastLessonNotAttended() {
            givenLessons(
                    attendedLesson(1, com.tcs.module.marketplace.enums.AttendanceStatus.COMPLETED),
                    attendedLesson(2, com.tcs.module.marketplace.enums.AttendanceStatus.PENDING));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Cần điểm danh buổi học cuối cùng trước khi xác nhận hoàn thành lớp.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID12 (A) - Co buoi hoc nhung khong buoi nao COMPLETED -> 'Chưa có buổi nào được điểm danh ...'")
        void utcid12_noCompletedLesson() {
            givenLessons(attendedLesson(1, com.tcs.module.marketplace.enums.AttendanceStatus.ABSENT));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.confirmClassCompletion(CLASS_ID));
            assertEquals("Chưa có buổi nào được điểm danh (đã dạy) nên chưa thể xác nhận hoàn thành lớp.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID13 (B) - tutorCompletedAt da co san -> khong ghi de moc thoi gian cu")
        void utcid13_tutorCompletedAtNotOverwritten() {
            java.time.LocalDateTime firstConfirm = java.time.LocalDateTime.now().minusDays(2);
            activeAssignment.setTutorCompletedAt(firstConfirm);
            when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(false);

            service.confirmClassCompletion(CLASS_ID);

            assertEquals(firstConfirm, activeAssignment.getTutorCompletedAt(),
                    "Moc gia su xac nhan lan dau phai duoc giu nguyen");
        }
    }

    // ===================================================================
    //  Sheet: signAssignmentContract (ky hop dong lop ca nhan bang OTP)
    // ===================================================================
    @Nested
    @DisplayName("signAssignmentContract")
    class SignAssignmentContract {

        private ClassAssignment assignment;

        @BeforeEach
        void initAssignment() {
            TutorApplication application = new TutorApplication();
            application.setApplicationId(APPLICATION_ID);
            application.setTutoringClass(tutoringClass);
            application.setTutor(tutor);

            assignment = new ClassAssignment();
            assignment.setAssignmentId(ASSIGNMENT_ID);
            assignment.setTutor(tutor);
            assignment.setApplication(application);
            assignment.setStatus(ClassAssignmentStatus.PENDING);

            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(cccdService.getByUserId(anyLong())).thenReturn(completeCccd());
        }

        private com.tcs.module.profile.dto.CccdInfoDto completeCccd() {
            return com.tcs.module.profile.dto.CccdInfoDto.builder()
                    .fullName("Nguyen Van A")
                    .cccdNumber("012345678901")
                    .dateOfBirth("01/01/2000")
                    .permanentAddress("Hà Nội")
                    .complete(true)
                    .build();
        }

        @Test
        @DisplayName("UTCID01 (N) - Ben A (chu lop) ky truoc -> ghi clientSignedAt")
        void utcid01_clientSignsFirst() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);

            service.signAssignmentContract(ASSIGNMENT_ID, "123456");

            assertNotNull(assignment.getClientSignedAt());
            assertNull(assignment.getTutorSignedAt());
            verify(classAssignmentRepository).save(assignment);
        }

        @Test
        @DisplayName("UTCID02 (N) - Gia su ky sau khi ben A da ky -> ghi tutorSignedAt")
        void utcid02_tutorSignsAfterClient() {
            when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
            assignment.setClientSignedAt(java.time.LocalDateTime.now().minusHours(1));
            when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());
            when(contractService.generateForAssignment(ASSIGNMENT_ID)).thenAnswer(i -> {
                var contract = new com.tcs.module.contract.entity.Contract();
                contract.setContractId(1100L);
                contract.setContractNo("TCS-20260101-0001");
                return contract;
            });
            when(contractRepository.save(any(com.tcs.module.contract.entity.Contract.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                    .thenReturn(Optional.empty());
            when(paymentTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.signAssignmentContract(ASSIGNMENT_ID, "123456");

            assertNotNull(assignment.getTutorSignedAt());
        }

        @Test
        @DisplayName("UTCID03 (A) - assignmentId khong khop loi moi nao -> 'Không tìm thấy lời mời nhận lớp'")
        void utcid03_assignmentNotFound() {
            when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.signAssignmentContract(ASSIGNMENT_ID, "123456"));
            assertEquals("Không tìm thấy lời mời nhận lớp", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Loi moi khong con PENDING -> 'Lời mời đã được xử lý hoặc hợp đồng đã hoàn tất'")
        void utcid04_assignmentNotPending() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            assignment.setStatus(ClassAssignmentStatus.ACTIVE);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.signAssignmentContract(ASSIGNMENT_ID, "123456"));
            assertEquals("Lời mời đã được xử lý hoặc hợp đồng đã hoàn tất", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Gia su ky khi ben A chua ky -> 'Bên A (phụ huynh/học sinh) phải ký hợp đồng trước ...'")
        void utcid05_tutorSignsBeforeClient() {
            when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.signAssignmentContract(ASSIGNMENT_ID, "123456"));
            assertEquals("Bên A (phụ huynh/học sinh) phải ký hợp đồng trước. Vui lòng chờ Bên A ký.",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Nguoi ky chua co so CCCD trong ho so -> chan ky hop dong")
        void utcid06_signerWithoutCccd() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(cccdService.getByUserId(CLIENT_USER_ID))
                    .thenReturn(com.tcs.module.profile.dto.CccdInfoDto.builder().build());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.signAssignmentContract(ASSIGNMENT_ID, "123456"));
            assertEquals("Bạn cần cập nhật Căn cước công dân (CCCD) trong hồ sơ trước khi ký hợp đồng.",
                    ex.getMessage());
            verify(classAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID07 (A) - OTP sai hoac het han -> loi tu OtpService duoc nem ra")
        void utcid07_wrongOtp() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(otpService.verify(anyString(), any(), anyString(), any()))
                    .thenThrow(new com.tcs.module.identity.service.OtpService.OtpExpiredException(
                            "Mã OTP đã hết hạn. Vui lòng bấm gửi lại mã."));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.signAssignmentContract(ASSIGNMENT_ID, "999999"));
            assertEquals("Mã OTP đã hết hạn. Vui lòng bấm gửi lại mã.", ex.getMessage());
            verify(classAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID08 (A) - Nguoi goi khong phai gia su cung khong phai chu lop -> 'Bạn không thuộc hợp đồng này'")
        void utcid08_callerHasNoRole() {
            when(authHelper.currentUserId()).thenReturn(999L);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.signAssignmentContract(ASSIGNMENT_ID, "123456"));
            assertEquals("Bạn không thuộc hợp đồng này", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: fulfillClassRequest (phu huynh chot gia su tu danh sach trung tam de cu)
    // ===================================================================
    @Nested
    @DisplayName("fulfillClassRequest")
    class FulfillClassRequest {

        private static final String REQUEST_ID = "req-001";
        private static final String DETAILS_JSON = "{\"subjectId\":1,\"title\":\"Toan 9\"}";

        private ClassRequestStore.ClassRequestData requestData(
                Long ownerUserId, String status, String detailsJson, List<Long> candidates) {
            return new ClassRequestStore.ClassRequestData(
                    REQUEST_ID, ownerUserId, 10L, 2L, "Can gia su Toan",
                    new BigDecimal("1000000"), status, null,
                    java.time.LocalDateTime.now().toString(), detailsJson, candidates, null);
        }

        private void givenRequest(ClassRequestStore.ClassRequestData data) {
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.of(data));
            when(classRequestStore.candidatesOf(data))
                    .thenReturn(data.candidateTutorIds() == null ? List.of() : data.candidateTutorIds());
        }

        @Test
        @DisplayName("UTCID01 (N) - Chu yeu cau chot gia su duoc de cu -> tao lop OPEN va don ung tuyen cho gia su do")
        void utcid01_fulfillSuccessfully() {
            loginAsClient();
            givenRequest(requestData(CLIENT_USER_ID, ClassRequestStore.STATUS_SEARCHING,
                    DETAILS_JSON, List.of(TUTOR_ID)));
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.of(tutor));
            when(tutorApplicationRepository.save(any(TutorApplication.class))).thenAnswer(i -> {
                TutorApplication app = i.getArgument(0);
                app.setApplicationId(APPLICATION_ID);
                return app;
            });
            when(tutorApplicationRepository.findById(APPLICATION_ID)).thenAnswer(i -> {
                TutorApplication app = new TutorApplication();
                app.setApplicationId(APPLICATION_ID);
                app.setTutoringClass(tutoringClass);
                app.setTutor(tutor);
                return Optional.of(app);
            });
            when(tutorApplicationRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of());
            when(cccdService.getByUserId(CLIENT_USER_ID))
                    .thenReturn(com.tcs.module.profile.dto.CccdInfoDto.builder()
                            .cccdNumber("012345678901").complete(true).build());
            when(classAssignmentRepository.save(any(ClassAssignment.class))).thenAnswer(i -> i.getArgument(0));
            when(classAssignmentRepository.findByApplication_ApplicationId(APPLICATION_ID))
                    .thenReturn(Optional.empty());
            when(classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdOrderByAssignedDateDesc(CLASS_ID))
                    .thenReturn(Optional.empty());
            when(lessonRepository.findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(anyLong()))
                    .thenReturn(List.of());
            when(lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(anyLong()))
                    .thenReturn(List.of());

            service.fulfillClassRequest(REQUEST_ID, TUTOR_ID);

            assertEquals(TutoringClassStatus.MATCHED, tutoringClass.getStatus(),
                    "Sau khi chon gia su lop chuyen sang MATCHED");
            verify(tutorApplicationRepository).save(any(TutorApplication.class));
            verify(classRequestStore).save(any());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong phai khach hang -> 'Chỉ phụ huynh/khách hàng mới tạo lớp học'")
        void utcid02_notAClient() {
            when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
            when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
            when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.empty());

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Chỉ phụ huynh/khách hàng mới tạo lớp học", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - requestId khong khop yeu cau nao -> 'Không tìm thấy yêu cầu'")
        void utcid03_requestNotFound() {
            loginAsClient();
            when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Không tìm thấy yêu cầu", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Yeu cau thuoc khach hang khac -> 'Không có quyền với yêu cầu này'")
        void utcid04_requestOfAnotherClient() {
            loginAsClient();
            givenRequest(requestData(999L, ClassRequestStore.STATUS_SEARCHING, DETAILS_JSON, List.of(TUTOR_ID)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Không có quyền với yêu cầu này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - Yeu cau da o trang thai ket thuc -> 'Yêu cầu này đã hoàn tất.'")
        void utcid05_requestAlreadyClosed() {
            loginAsClient();
            givenRequest(requestData(CLIENT_USER_ID, ClassRequestStore.STATUS_ACCEPTED,
                    DETAILS_JSON, List.of(TUTOR_ID)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Yêu cầu này đã hoàn tất.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - tutorId khong nam trong danh sach de cu -> chan")
        void utcid06_tutorNotProposed() {
            loginAsClient();
            givenRequest(requestData(CLIENT_USER_ID, ClassRequestStore.STATUS_SEARCHING,
                    DETAILS_JSON, List.of(OTHER_TUTOR_ID)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Gia sư này không nằm trong danh sách trung tâm đề cử.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Yeu cau khong co detailsJson -> 'Yêu cầu thiếu thông tin lớp để tạo.'")
        void utcid07_missingDetailsJson() {
            loginAsClient();
            givenRequest(requestData(CLIENT_USER_ID, ClassRequestStore.STATUS_SEARCHING,
                    "   ", List.of(TUTOR_ID)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Yêu cầu thiếu thông tin lớp để tạo.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - detailsJson khong doc duoc -> 'Không đọc được thông tin yêu cầu.'")
        void utcid08_unparsableDetailsJson() {
            loginAsClient();
            givenRequest(requestData(CLIENT_USER_ID, ClassRequestStore.STATUS_SEARCHING,
                    "{khong-phai-json", List.of(TUTOR_ID)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Không đọc được thông tin yêu cầu.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Khong doc lai duoc lop vua tao -> 'Không tìm thấy lớp vừa tạo'")
        void utcid09_createdClassNotFound() {
            loginAsClient();
            givenRequest(requestData(CLIENT_USER_ID, ClassRequestStore.STATUS_SEARCHING,
                    DETAILS_JSON, List.of(TUTOR_ID)));
            when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.fulfillClassRequest(REQUEST_ID, TUTOR_ID));
            assertEquals("Không tìm thấy lớp vừa tạo", ex.getMessage());
        }
    }
}
