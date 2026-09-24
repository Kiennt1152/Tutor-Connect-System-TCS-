package com.tcs.module.marketplace.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.WalletStatus;
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
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
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
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52MarketplaceTutorITTest {

    private static final Long CLASS_ID = 5L;
    private static final Long ASSIGNMENT_ID = 7L;
    private static final Long CLASS_STUDENT_ID = 8L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;

    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private ContractService contractService;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private EscrowService escrowService;
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private FavoriteTutorRepository favoriteTutorRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private AuditLogService auditLogService;
    @Mock private CccdService cccdService;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private TutorSubjectRepository tutorSubjectRepository;
    @Mock private LessonRescheduleRequestRepository rescheduleRequestRepository;
    @Mock private LessonReminderService lessonReminderService;
    @Mock private EmailOtpRepository emailOtpRepository;
    @Mock private OtpService otpService;
    @Mock private EmailService contractEmailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ClassRequestStore classRequestStore;

    @Mock private EntityManager entityManager;
    @Mock private Query query;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    private ExpiredClassCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
        cleanupService = new ExpiredClassCleanupService(tutoringClassRepository, transactionManager);
        ReflectionTestUtils.setField(cleanupService, "em", entityManager);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("id"), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
    }

    /**
     * Test Case: IT-MKT-001
     * Title: Create a private class draft for a client and record the creation audit.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes).
     * Input: Class title “Cần gia sư Toán lớp 9”, budget 120000 and valid catalog data.
     * Steps:
     *   1. Prepare the fixture: Authenticated client has a Client profile.
     *   2. Use the input: Class title “Cần gia sư Toán lớp 9”, budget 120000 and valid catalog data.
     *   3. Execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_001_ClientCreatesPrivateClassDraftAndAuditTrail.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture saved class and assert creator, title, DRAFT status, budget and audit call.
     * Expected: A DRAFT class is saved for the current client with the requested title and budget, and CREATE_CLASS is audited.
     * Pre-conditions: Authenticated client has a Client profile.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-001: Create a private class draft for a client and record the creation audit.")
    void IT_MKT_001_ClientCreatesPrivateClassDraftAndAuditTrail() {
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        CreateClassRequest request = createClassRequest();

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> {
            TutoringClass saved = invocation.getArgument(0);
            saved.setClassId(CLASS_ID);
            return saved;
        });

        marketplaceService.createClass(request);

        var classCaptor = ArgumentCaptor.forClass(TutoringClass.class);
        verify(tutoringClassRepository).save(classCaptor.capture());
        TutoringClass saved = classCaptor.getValue();
        assertEquals(clientUser, saved.getCreator());
        assertEquals("Cần gia sư Toán lớp 9", saved.getTitle());
        assertEquals(TutoringClassStatus.DRAFT, saved.getStatus());
        assertEquals(new BigDecimal("120000.00"), saved.getBudget());
        verify(auditLogService).record(CLIENT_USER_ID, "CREATE_CLASS", "TutoringClass", CLASS_ID, null, request);
    }

    /**
     * Test Case: IT-MKT-002
     * Title: List marketplace classes using the requested status filter.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.listClasses (GET /api/marketplace/classes?status=OPEN).
     * Input: status=OPEN.
     * Steps:
     *   1. Prepare the fixture: Repository has one OPEN fixture.
     *   2. Use the input: status=OPEN.
     *   3. Execute MarketplaceServiceImpl.listClasses (GET /api/marketplace/classes?status=OPEN). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_002_ListOpenMarketplaceClassesFiltersByStatus.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response count and verify findByStatus(OPEN).
     * Expected: Only OPEN classes from TutoringClassRepository are returned; an unfiltered findAll is not used.
     * Pre-conditions: Repository has one OPEN fixture.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-002: List marketplace classes using the requested status filter.")
    void IT_MKT_002_ListOpenMarketplaceClassesFiltersByStatus() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass openClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);
        when(tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN)).thenReturn(List.of(openClass));

        List<?> responses = marketplaceService.listClasses(TutoringClassStatus.OPEN);

        assertEquals(1, responses.size());
        verify(tutoringClassRepository).findByStatus(TutoringClassStatus.OPEN);
        verify(tutoringClassRepository, never()).findAll();
    }

    /**
     * Test Case: IT-MKT-003
     * Title: Load the selected marketplace class detail.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.getClass (GET /api/marketplace/classes/{classId}).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: Class 77 exists and is OPEN.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.getClass (GET /api/marketplace/classes/{classId}). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_003_GetClassDetailLoadsTargetMarketplaceRecord.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert id/title from the returned DTO.
     * Expected: Class detail returns the requested class id and title “Lớp toán”.
     * Pre-conditions: Class 77 exists and is OPEN.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-003: Load the selected marketplace class detail.")
    void IT_MKT_003_GetClassDetailLoadsTargetMarketplaceRecord() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass openClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(openClass));

        var response = marketplaceService.getClass(CLASS_ID, null, null);

        assertEquals(CLASS_ID, response.getClassId());
        assertEquals("Lớp toán", response.getTitle());
    }

    /**
     * Test Case: IT-MKT-004
     * Title: Reject private-class creation when the subject is missing.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes).
     * Input: CreateClassRequest with blank subject.
     * Steps:
     *   1. Prepare the fixture: Authenticated client has a valid profile.
     *   2. Use the input: CreateClassRequest with blank subject.
     *   3. Execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_004_RejectClassCreationWhenSubjectAndDetailsAreMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify TutoringClassRepository.save is never called.
     * Expected: The service returns “Vui lòng chọn môn học” before saving a class.
     * Pre-conditions: Authenticated client has a valid profile.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-004: Reject private-class creation when the subject is missing.")
    void IT_MKT_004_RejectClassCreationWhenSubjectAndDetailsAreMissing() {
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        CreateClassRequest request = new CreateClassRequest();

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.createClass(request));

        assertEquals("Vui lòng chọn môn học", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MKT-005
     * Title: Require a wallet before a verified tutor can apply to a class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply).
     * Input: Application for class 77.
     * Steps:
     *   1. Prepare the fixture: Tutor is VERIFIED but WalletRepository returns empty.
     *   2. Use the input: Application for class 77.
     *   3. Execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_005_RejectVerifiedTutorApplicationWhenTutorWalletIsMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exact wallet message and verify class/application writes are skipped.
     * Expected: The service asks the tutor to create a wallet and does not save an application when the wallet is absent.
     * Pre-conditions: Tutor is VERIFIED but WalletRepository returns empty.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-005: Require a wallet before a verified tutor can apply to a class.")
    void IT_MKT_005_RejectVerifiedTutorApplicationWhenTutorWalletIsMissing() {
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

        BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, new ApplyClassRequest()));

        assertEquals("Bạn cần tạo ví trước khi tiếp tục. Vui lòng vào Ví của tôi để tạo ví.", exception.getMessage());
        verify(tutoringClassRepository, never()).findById(CLASS_ID);
        verify(tutorApplicationRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MKT-006
     * Title: Block anonymous private-class creation before repository mutation.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes).
     * Input: Valid create-class request.
     * Steps:
     *   1. Prepare the fixture: No authenticated user.
     *   2. Use the input: Valid create-class request.
     *   3. Execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_006_BlockAnonymousClassCreationBeforeRepositoryMutation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify no class save.
     * Expected: The service returns “Yêu cầu đăng nhập” and does not save a class.
     * Pre-conditions: No authenticated user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-006: Block anonymous private-class creation before repository mutation.")
    void IT_MKT_006_BlockAnonymousClassCreationBeforeRepositoryMutation() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClass(createClassRequest()));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MKT-007
     * Title: Prevent a tutor from posting a client private class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes).
     * Input: Valid create-class request.
     * Steps:
     *   1. Prepare the fixture: Authenticated user has TUTOR role and no Client profile.
     *   2. Use the input: Valid create-class request.
     *   3. Execute MarketplaceServiceImpl.createClass (POST /api/marketplace/classes). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_007_BlockNonClientRoleFromPostingPrivateClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role error and verify no class save.
     * Expected: The service returns “Chỉ phụ huynh/khách hàng mới tạo lớp học” and does not save a class.
     * Pre-conditions: Authenticated user has TUTOR role and no Client profile.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-007: Prevent a tutor from posting a client private class.")
    void IT_MKT_007_BlockNonClientRoleFromPostingPrivateClass() {
        User tutorUser = user(TUTOR_USER_ID);
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClass(createClassRequest()));

        assertEquals("Chỉ phụ huynh/khách hàng mới tạo lớp học", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MKT-008
     * Title: Prevent a client from unpublishing another client’s class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.unpublishClass (POST /api/marketplace/classes/{classId}/unpublish).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: Class 77 belongs to client A; client B is authenticated.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.unpublishClass (POST /api/marketplace/classes/{classId}/unpublish). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_008_PreventClientFromUnpublishingAnotherClientsOpenClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify no status save.
     * Expected: The service returns “Không có quyền gỡ đăng lớp này” and leaves the class unchanged.
     * Pre-conditions: Class 77 belongs to client A; client B is authenticated.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-008: Prevent a client from unpublishing another client’s class.")
    void IT_MKT_008_PreventClientFromUnpublishingAnotherClientsOpenClass() {
        User owner = user(CLIENT_USER_ID);
        User otherClient = user(333L);
        TutoringClass tutoringClass = tutoringClass(owner, TutoringClassStatus.OPEN);

        when(authHelper.currentUserId()).thenReturn(otherClient.getUserId());
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.unpublishClass(CLASS_ID));

        assertEquals("Không có quyền gỡ đăng lớp này", exception.getMessage());
        verify(tutorApplicationRepository, never()).countByTutoringClass_ClassIdAndStatusNot(any(), any());
        verify(tutoringClassRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MKT-009
     * Title: Reject a duplicate tutor application for the same open class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply).
     * Input: Second application for class 77.
     * Steps:
     *   1. Prepare the fixture: Verified tutor has an existing SUBMITTED application for class 77 and an active wallet.
     *   2. Use the input: Second application for class 77.
     *   3. Execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_009_RejectDuplicateTutorApplicationForSameOpenClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify TutorApplicationRepository.save is never called.
     * Expected: The service returns the one-application-per-class message and does not save another application.
     * Pre-conditions: Verified tutor has an existing SUBMITTED application for class 77 and an active wallet.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-009: Reject a duplicate tutor application for the same open class.")
    void IT_MKT_009_RejectDuplicateTutorApplicationForSameOpenClass() {
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        TutoringClass tutoringClass = tutoringClass(user(CLIENT_USER_ID), TutoringClassStatus.OPEN);
        TutorApplication existing = tutorApplication(tutoringClass, tutor);
        existing.setStatus(com.tcs.module.marketplace.enums.TutorApplicationStatus.SUBMITTED);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(activeWallet(tutorUser)));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.findFirstByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, tutor.getTutorId()))
                .thenReturn(Optional.of(existing));

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, applyClassRequest()));

        assertEquals("Bạn đã ứng tuyển lớp này rồi. Mỗi lớp chỉ nộp được một đơn.", exception.getMessage());
        verify(tutorApplicationRepository, never()).save(any());
    }

    /**
     * Test Case: IT-MKT-010
     * Title: Publish a draft class and set its marketplace expiry.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.publishClass (POST /api/marketplace/classes/{classId}/publish).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: Current client owns class 77 in DRAFT state.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.publishClass (POST /api/marketplace/classes/{classId}/publish). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_010_PublishClassChangesStatusAndRecordsAudit.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status/expiry and audit arguments.
     * Expected: Status changes to OPEN, expiresAt is in the future and PUBLISH_CLASS is audited.
     * Pre-conditions: Current client owns class 77 in DRAFT state.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-010: Publish a draft class and set its marketplace expiry.")
    void IT_MKT_010_PublishClassChangesStatusAndRecordsAudit() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.DRAFT);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        marketplaceService.publishClass(CLASS_ID);

        assertEquals(TutoringClassStatus.OPEN, tutoringClass.getStatus());
        assertTrue(tutoringClass.getExpiresAt().isAfter(LocalDateTime.now()));
        verify(auditLogService).record(CLIENT_USER_ID, "PUBLISH_CLASS", "TutoringClass", CLASS_ID, null, null);
    }

    /**
     * Test Case: IT-MKT-011
     * Title: Notify the class owner when a verified tutor applies.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply).
     * Input: Valid application/cover letter.
     * Steps:
     *   1. Prepare the fixture: Verified tutor with an active wallet applies to an OPEN client class.
     *   2. Use the input: Valid application/cover letter.
     *   3. Execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_011_TutorApplicationNotifiesClassOwner.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification recipient, template, reference type TUTORING_CLASS and class id.
     * Expected: A new tutor application is saved and the client receives the MARKETPLACE_NEW_APPLICATION notification.
     * Pre-conditions: Verified tutor with an active wallet applies to an OPEN client class.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-011: Notify the class owner when a verified tutor applies.")
    void IT_MKT_011_TutorApplicationNotifiesClassOwner() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(activeWallet(tutorUser)));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.findFirstByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, tutor.getTutorId()))
                .thenReturn(Optional.empty());
        when(tutorApplicationRepository.save(any(TutorApplication.class))).thenAnswer(invocation -> {
            TutorApplication saved = invocation.getArgument(0);
            saved.setApplicationId(901L);
            return saved;
        });

        marketplaceService.applyToClass(CLASS_ID, applyClassRequest());

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(clientUser),
                eq(NotificationType.APPLICATION),
                eq("MARKETPLACE_NEW_APPLICATION"),
                any(),
                eq("Có gia sư ứng tuyển"),
                anyString(),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
    }

    /**
     * Test Case: IT-MKT-012
     * Title: Reject a tutor application after the marketplace class is no longer open.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply).
     * Input: Application for class 77.
     * Steps:
     *   1. Prepare the fixture: Verified tutor and wallet exist; class 77 is CANCELLED.
     *   2. Use the input: Application for class 77.
     *   3. Execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_012_RejectTutorApplicationWhenMarketplaceClassIsNoLongerOpen.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify no application save.
     * Expected: The service returns “Lớp không mở đơn ứng tuyển” and does not save an application or audit row.
     * Pre-conditions: Verified tutor and wallet exist; class 77 is CANCELLED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-012: Reject a tutor application after the marketplace class is no longer open.")
    void IT_MKT_012_RejectTutorApplicationWhenMarketplaceClassIsNoLongerOpen() {
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        TutoringClass cancelledClass = tutoringClass(user(CLIENT_USER_ID), TutoringClassStatus.CANCELLED);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(activeWallet(tutorUser)));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(cancelledClass));

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, applyClassRequest()));

        assertEquals("Lớp không mở đơn ứng tuyển", exception.getMessage());
        verify(tutorApplicationRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
    }

    /**
     * Test Case: IT-MKT-013
     * Title: Search tutors by keyword and subject membership.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.searchTutors (GET /api/marketplace/tutors/search).
     * Input: keyword=minh; subjectId=101.
     * Steps:
     *   1. Prepare the fixture: Three tutor fixtures cover matching keyword, wrong subject and wrong keyword.
     *   2. Use the input: keyword=minh; subjectId=101.
     *   3. Execute MarketplaceServiceImpl.searchTutors (GET /api/marketplace/tutors/search). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_013_SearchTutorsFiltersByKeywordAndSubjectMembership.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert one result with tutor id 301 and name Nguyễn Minh Toán.
     * Expected: Only the verified tutor matching keyword “minh” and subject 101 is returned.
     * Pre-conditions: Three tutor fixtures cover matching keyword, wrong subject and wrong keyword.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-013: Search tutors by keyword and subject membership.")
    void IT_MKT_013_SearchTutorsFiltersByKeywordAndSubjectMembership() {
        Tutor matchingTutor = tutor(user(301L));
        matchingTutor.setTutorId(301L);
        matchingTutor.setFullName("Nguyễn Minh Toán");
        matchingTutor.setBio("Gia sư luyện thi đại học môn Toán");
        matchingTutor.setHourlyRate(new BigDecimal("180000.00"));
        matchingTutor.setRatingAvg(new BigDecimal("4.80"));
        matchingTutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        Tutor wrongSubjectTutor = tutor(user(302L));
        wrongSubjectTutor.setTutorId(302L);
        wrongSubjectTutor.setFullName("Minh Anh");
        wrongSubjectTutor.setBio("Dạy tiếng Anh giao tiếp");
        wrongSubjectTutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        Tutor wrongKeywordTutor = tutor(user(303L));
        wrongKeywordTutor.setTutorId(303L);
        wrongKeywordTutor.setFullName("Trần Quốc Bảo");
        wrongKeywordTutor.setBio("Gia sư Vật lý");
        wrongKeywordTutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        when(tutorRepository.findAll()).thenReturn(List.of(matchingTutor, wrongSubjectTutor, wrongKeywordTutor));
        when(tutorSubjectRepository.existsByTutor_TutorIdAndSubject_SubjectId(301L, 101L)).thenReturn(true);
        when(tutorSubjectRepository.existsByTutor_TutorIdAndSubject_SubjectId(302L, 101L)).thenReturn(false);

        List<TutorSearchResponse> responses = marketplaceService.searchTutors("minh", 101L);

        assertEquals(1, responses.size());
        assertEquals(301L, responses.get(0).getTutorId());
        assertEquals("Nguyễn Minh Toán", responses.get(0).getFullName());
        verify(tutorRepository).findAll();
    }

    /**
     * Test Case: IT-MKT-014
     * Title: Store per-subject proposed rates and expose the highest rate on a tutor application.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply).
     * Input: Rates {101:120000, 102:150000}; cover letter.
     * Steps:
     *   1. Prepare the fixture: Verified tutor with wallet applies to an OPEN class containing subjects 101 and 102.
     *   2. Use the input: Rates {101:120000, 102:150000}; cover letter.
     *   3. Execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_014_TutorApplicationStoresPerSubjectRatesAndHighestDisplayedRate.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture TutorApplication and assert proposedRate and both JSON entries.
     * Expected: The saved application keeps rates 120000/150000 in JSON and uses 150000 as proposedRate.
     * Pre-conditions: Verified tutor with wallet applies to an OPEN class containing subjects 101 and 102.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-014: Store per-subject proposed rates and expose the highest rate on a tutor application.")
    void IT_MKT_014_TutorApplicationStoresPerSubjectRatesAndHighestDisplayedRate() {
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        TutoringClass tutoringClass = tutoringClass(user(CLIENT_USER_ID), TutoringClassStatus.OPEN);
        tutoringClass.setDetailsJson("{\"subjectIds\":[\"101\",\"102\"],\"slots\":[]}");
        ApplyClassRequest request = new ApplyClassRequest();
        request.setProposedRates(Map.of(
                "101", new BigDecimal("120000.00"),
                "102", new BigDecimal("150000.00")));
        request.setCoverLetter("Em có thể dạy cả Toán và Lý theo lịch lớp.");

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(activeWallet(tutorUser)));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.findFirstByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, tutor.getTutorId()))
                .thenReturn(Optional.empty());
        when(tutorApplicationRepository.save(any(TutorApplication.class))).thenAnswer(invocation -> {
            TutorApplication saved = invocation.getArgument(0);
            saved.setApplicationId(902L);
            return saved;
        });

        marketplaceService.applyToClass(CLASS_ID, request);

        ArgumentCaptor<TutorApplication> applicationCaptor = ArgumentCaptor.forClass(TutorApplication.class);
        verify(tutorApplicationRepository).save(applicationCaptor.capture());
        TutorApplication saved = applicationCaptor.getValue();
        assertEquals(new BigDecimal("150000.00"), saved.getProposedRate());
        assertTrue(saved.getProposedRatesJson().contains("\"101\":120000.00"));
        assertTrue(saved.getProposedRatesJson().contains("\"102\":150000.00"));
    }

    /**
     * Test Case: IT-MKT-015
     * Title: List only marketplace classes created by the current client.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.listMyClasses (GET /api/marketplace/classes/mine).
     * Input: Authenticated client session.
     * Steps:
     *   1. Prepare the fixture: Client 11 owns one DRAFT class.
     *   2. Use the input: Authenticated client session.
     *   3. Execute MarketplaceServiceImpl.listMyClasses (GET /api/marketplace/classes/mine). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_015_ListMyClassesReturnsOnlyRecordsOwnedByCurrentClient.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert count and verify findByCreator_UserId(11).
     * Expected: The response contains the current client’s class and uses the owner-scoped repository query.
     * Pre-conditions: Client 11 owns one DRAFT class.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-015: List only marketplace classes created by the current client.")
    void IT_MKT_015_ListMyClassesReturnsOnlyRecordsOwnedByCurrentClient() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass mine = tutoringClass(clientUser, TutoringClassStatus.DRAFT);
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(tutoringClassRepository.findByCreator_UserId(CLIENT_USER_ID)).thenReturn(List.of(mine));

        List<?> responses = marketplaceService.listMyClasses();

        assertEquals(1, responses.size());
        verify(tutoringClassRepository).findByCreator_UserId(CLIENT_USER_ID);
    }

    /**
     * Test Case: IT-MKT-016
     * Title: Copy the selected tutor’s proposed rate into the private-class deal.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.chooseApplicant (POST /api/marketplace/class-requests/{requestId}/choose-tutor/{tutorId}).
     * Input: classId=77; applicationId=55.
     * Steps:
     *   1. Prepare the fixture: Client owns an OPEN class; application 55 proposes 140000 for subject 1.
     *   2. Use the input: classId=77; applicationId=55.
     *   3. Execute MarketplaceServiceImpl.chooseApplicant (POST /api/marketplace/class-requests/{requestId}/choose-tutor/{tutorId}). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_016_ChoosingTutorCopiesProposedRateIntoPrivateClassDeal.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert tuitionFee and parsed subjectFees/subjectIds after choosing.
     * Expected: The class tuition becomes 140000 and its details JSON keeps only subject 1 with the selected rate.
     * Pre-conditions: Client owns an OPEN class; application 55 proposes 140000 for subject 1.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-016: Copy the selected tutor’s proposed rate into the private-class deal.")
    void IT_MKT_016_ChoosingTutorCopiesProposedRateIntoPrivateClassDeal() throws Exception {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);
        tutoringClass.setNumberOfSessions(4);
        tutoringClass.setDetailsJson("""
                {"scheduleMode":"WEEKLY","repeatEveryWeeks":1,"subjectIds":["1","2"],
                 "subjectFees":{"1":"120000","2":"150000"},
                 "slots":[
                    {"subjectId":"1","day":"T2","start":"18:00","end":"19:00"},
                    {"subjectId":"2","day":"T3","start":"18:00","end":"19:00"}
                 ]}
                """);
        TutorApplication chosen = tutorApplication(tutoringClass, tutor(tutorUser));
        chosen.setProposedRatesJson("{\"1\":140000}");
        chosen.setProposedRate(new BigDecimal("140000"));

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(cccdService.getByUserId(CLIENT_USER_ID)).thenReturn(CccdInfoDto.builder()
                .fullName("Client Test")
                .cccdNumber("012345678901")
                .dateOfBirth("01/01/2000")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build());
        when(tutorApplicationRepository.findById(55L)).thenReturn(Optional.of(chosen));
        when(tutorApplicationRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(chosen));
        when(classAssignmentRepository.findByApplication_ApplicationId(55L)).thenReturn(Optional.empty());
        when(classAssignmentRepository.save(any(ClassAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        marketplaceService.chooseApplicant(CLASS_ID, 55L);

        Map<?, ?> parsed = new ObjectMapper().readValue(tutoringClass.getDetailsJson(), Map.class);
        assertEquals(BigDecimal.valueOf(140000), tutoringClass.getTuitionFee());
        assertEquals("{1=140000}", parsed.get("subjectFees").toString());
        assertEquals("[1]", parsed.get("subjectIds").toString());
    }

    /**
     * Test Case: IT-MKT-017
     * Title: Return an unpublished class with no applications to DRAFT.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.unpublishClass (POST /api/marketplace/classes/{classId}/unpublish).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: Current client owns an OPEN class with zero active applications.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.unpublishClass (POST /api/marketplace/classes/{classId}/unpublish). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_017_UnpublishOpenClassWithoutApplicationsReturnsItToDraft.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert DRAFT, null expiry and saved class.
     * Expected: Status becomes DRAFT and expiresAt is cleared when there are no non-rejected applications.
     * Pre-conditions: Current client owns an OPEN class with zero active applications.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-017: Return an unpublished class with no applications to DRAFT.")
    void IT_MKT_017_UnpublishOpenClassWithoutApplicationsReturnsItToDraft() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);
        tutoringClass.setExpiresAt(LocalDateTime.now().plusDays(10));

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.countByTutoringClass_ClassIdAndStatusNot(
                eq(CLASS_ID),
                eq(com.tcs.module.marketplace.enums.TutorApplicationStatus.REJECTED)))
                .thenReturn(0L);
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        marketplaceService.unpublishClass(CLASS_ID);

        assertEquals(TutoringClassStatus.DRAFT, tutoringClass.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(tutoringClass.getExpiresAt());
    }

    /**
     * Test Case: IT-MKT-018
     * Title: Remove dependent rows before deleting an expired open class.
     * Procedure: Prepare the stated fixture and input, then execute ExpiredClassCleanupService.cleanupExpiredOpenClasses (scheduled cleanup).
     * Input: Scheduled cleanup time is current time.
     * Steps:
     *   1. Prepare the fixture: Class 701 is OPEN and expired.
     *   2. Use the input: Scheduled cleanup time is current time.
     *   3. Execute ExpiredClassCleanupService.cleanupExpiredOpenClasses (scheduled cleanup). Mapped test: com.tcs.module.marketplace.service.impl.Report52ExpiredClassCleanupITTest#IT_MKT_018_CleanupExpiredOpenClassRemovesDependentRowsBeforeClassRow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Verify native delete queries and that the class delete is executed after dependent deletes.
     * Expected: The cleanup queries expired OPEN classes, deletes dependent tutor applications and then deletes the class row for class 701.
     * Pre-conditions: Class 701 is OPEN and expired.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-018: Remove dependent rows before deleting an expired open class.")
    void IT_MKT_018_CleanupExpiredOpenClassRemovesDependentRowsBeforeClassRow() {
        TutoringClass expired = new TutoringClass();
        expired.setClassId(701L);
        expired.setStatus(TutoringClassStatus.OPEN);
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(tutoringClassRepository.findByStatusAndExpiresAtBefore(
                eq(TutoringClassStatus.OPEN),
                any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        cleanupService.cleanupExpiredOpenClasses();

        verify(tutoringClassRepository).findByStatusAndExpiresAtBefore(
                eq(TutoringClassStatus.OPEN),
                any(LocalDateTime.class));
        verify(entityManager).createNativeQuery("DELETE FROM tutor_applications WHERE class_id = :id");
        verify(entityManager).createNativeQuery("DELETE FROM tutoring_classes WHERE class_id = :id");
        verify(query, atLeast(6)).setParameter("id", 701L);
        verify(query, atLeast(6)).executeUpdate();
    }

    /**
     * Test Case: IT-MKT-019
     * Title: Include class context in the notification sent for a new tutor application.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply).
     * Input: Valid application for class 77.
     * Steps:
     *   1. Prepare the fixture: Verified tutor with wallet applies to the client’s OPEN class.
     *   2. Use the input: Valid application for class 77.
     *   3. Execute MarketplaceServiceImpl.applyToClass (POST /api/marketplace/classes/{classId}/apply). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_019_TutorApplicationNotificationUsesClassContextForFrontendNavigation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification template, text, reference type and reference id.
     * Expected: The application notification uses referenceType TUTORING_CLASS and referenceId 77 so the frontend can open the correct context.
     * Pre-conditions: Verified tutor with wallet applies to the client’s OPEN class.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-019: Include class context in the notification sent for a new tutor application.")
    void IT_MKT_019_TutorApplicationNotificationUsesClassContextForFrontendNavigation() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(activeWallet(tutorUser)));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.findFirstByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, tutor.getTutorId()))
                .thenReturn(Optional.empty());
        when(tutorApplicationRepository.save(any(TutorApplication.class))).thenAnswer(invocation -> {
            TutorApplication saved = invocation.getArgument(0);
            saved.setApplicationId(902L);
            return saved;
        });

        marketplaceService.applyToClass(CLASS_ID, applyClassRequest());

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(clientUser),
                eq(NotificationType.APPLICATION),
                eq("MARKETPLACE_NEW_APPLICATION"),
                any(),
                eq("Có gia sư ứng tuyển"),
                anyString(),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
    }

    /**
     * Test Case: IT-MKT-020
     * Title: Keep create-then-publish status consistent for a fresh private class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.createClass then publishClass (POST /api/marketplace/classes and /publish).
     * Input: Valid private-class creation request.
     * Steps:
     *   1. Prepare the fixture: Fresh authenticated client with valid profile/catalog data.
     *   2. Use the input: Valid private-class creation request.
     *   3. Execute MarketplaceServiceImpl.createClass then publishClass (POST /api/marketplace/classes and /publish). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_MKT_020_CreateThenPublishFreshPrivateClassKeepsConsistentFinalStatus.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert final status/expiry and both audit actions.
     * Expected: The newly created class ends OPEN with a future expiry and has both CREATE_CLASS and PUBLISH_CLASS audit records.
     * Pre-conditions: Fresh authenticated client with valid profile/catalog data.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-MKT-020: Keep create-then-publish status consistent for a fresh private class.")
    void IT_MKT_020_CreateThenPublishFreshPrivateClassKeepsConsistentFinalStatus() {
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        CreateClassRequest request = createClassRequest();
        TutoringClass[] savedHolder = new TutoringClass[1];

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> {
            TutoringClass saved = invocation.getArgument(0);
            saved.setClassId(CLASS_ID);
            savedHolder[0] = saved;
            return saved;
        });
        when(tutoringClassRepository.findById(CLASS_ID)).thenAnswer(invocation -> Optional.of(savedHolder[0]));

        marketplaceService.createClass(request);
        marketplaceService.publishClass(CLASS_ID);

        assertEquals(TutoringClassStatus.OPEN, savedHolder[0].getStatus());
        assertTrue(savedHolder[0].getExpiresAt().isAfter(LocalDateTime.now()));
        verify(auditLogService).record(CLIENT_USER_ID, "CREATE_CLASS", "TutoringClass", CLASS_ID, null, request);
        verify(auditLogService).record(CLIENT_USER_ID, "PUBLISH_CLASS", "TutoringClass", CLASS_ID, null, null);
    }

    

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.test");
        return user;
    }

    

    private Client client(User user) {
        Client client = new Client();
        client.setClientId(user.getUserId());
        client.setUser(user);
        client.setFullName("Phụ huynh test");
        client.setPhone("0900000000");
        client.setDateOfBirth(LocalDate.of(1988, 1, 1));
        return client;
    }

    

    private CreateClassRequest createClassRequest() {
        CreateClassRequest request = new CreateClassRequest();
        request.setTitle("Cần gia sư Toán lớp 9");
        request.setDetailsJson("{\"subjectIds\":[\"101\"],\"slots\":[]}");
        request.setBudget(new BigDecimal("120000.00"));
        return request;
    }

    

    private TutoringClass tutoringClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(creator);
        tutoringClass.setTitle("Lớp toán");
        tutoringClass.setDescription("Lớp toán test");
        tutoringClass.setStatus(status);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.PRIVATE);
        return tutoringClass;
    }

    

    private Tutor tutor(User tutorUser) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(44L);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư test");
        return tutor;
    }

    

    private TutorApplication tutorApplication(TutoringClass tutoringClass, Tutor tutor) {
        TutorApplication application = new TutorApplication();
        application.setApplicationId(55L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        return application;
    }

    

    private Wallet activeWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(user.getUserId());
        wallet.setUser(user);
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    

    private ApplyClassRequest applyClassRequest() {
        ApplyClassRequest request = new ApplyClassRequest();
        request.setProposedRate(new BigDecimal("120000.00"));
        request.setCoverLetter("Em có kinh nghiệm dạy Toán THCS.");
        return request;
    }
}
