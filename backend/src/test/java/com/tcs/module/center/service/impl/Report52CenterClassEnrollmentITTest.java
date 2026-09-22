package com.tcs.module.center.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.CooperationContractSigned;
import com.tcs.common.event.EscrowFunded;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.request.ScheduleSlotRequest;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.repository.CenterTutorMembershipRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.center.repository.RecruitmentPostRepository;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.contract.service.impl.ContractServiceImpl;
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
import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
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
import com.tcs.module.marketplace.entity.LessonAttendance;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
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
import com.tcs.module.marketplace.service.SubstitutionService;
import com.tcs.module.marketplace.service.impl.LessonReminderService;
import com.tcs.module.marketplace.service.impl.MarketplaceServiceImpl;
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
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
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
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52CenterClassEnrollmentITTest {

    private static final Long CENTER_USER_ID = 100L;
    private static final Long CLASS_ID = 500L;
    private static final Long TUTOR_ID = 20L;
    private static final Long CLASS_STUDENT_ID = 20L;
    private static final Long ESCROW_ID = 30L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;

    @Mock private AuthHelper authHelper;
    @Mock private RecruitmentPostRepository recruitmentPostRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private CenterTutorMembershipRepository membershipRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private VerificationRequestRepository verificationRequestRepository;
    @Mock private VerificationDocumentRepository verificationDocumentRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private EscrowService escrowService;
    @Mock private CenterRequestFeeService centerRequestFeeService;
    @Mock private RescheduleService rescheduleService;
    @Mock private SubstitutionService substitutionService;
    @Mock private AuditLogService auditLogService;
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private ContractTemplateRepository contractTemplateRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private CccdService cccdService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private EmailOtpRepository emailOtpRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FavoriteTutorRepository favoriteTutorRepository;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private TutorSubjectRepository tutorSubjectRepository;
    @Mock private LessonRescheduleRequestRepository rescheduleRequestRepository;
    @Mock private LessonReminderService lessonReminderService;
    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private EmailService emailService;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReputationHistoryRepository reputationHistoryRepository;

    @Mock private CenterEscrowAutoSettlementService centerEscrowAutoSettlementService;
    @Mock private ContractService contractService;

    @InjectMocks
    private CenterServiceImpl centerService;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    private CenterEscrowAutoSettlementService settlementService;
    private ContractServiceImpl contractServiceImpl;

    private EmailOtp activeOtp;

    @BeforeEach
    void setUp() {
        settlementService = new CenterEscrowAutoSettlementService(
                tutoringClassRepository,
                classStudentRepository,
                lessonRepository,
                lessonAttendanceRepository,
                escrowTransactionRepository,
                disputeRepository,
                refundRequestRepository,
                classTerminationRequestRepository,
                reportRepository,
                escrowService,
                systemParameterRepository,
                notificationDispatchService
        );

        contractServiceImpl = new ContractServiceImpl(
                authHelper,
                contractRepository,
                contractSignatureRepository,
                contractTemplateRepository,
                classAssignmentRepository,
                classStudentRepository,
                userRepository,
                tutorRepository,
                clientRepository,
                tutorCenterRepository,
                emailService,
                new OtpService(emailOtpRepository),
                escrowService,
                escrowTransactionRepository,
                paymentTransactionRepository,
                eventPublisher,
                reviewRepository,
                recruitmentApplicationRepository,
                systemParameterRepository,
                reputationHistoryRepository,
                lessonRepository,
                lessonAttendanceRepository,
                cccdService
        );

        activeOtp = new EmailOtp();
        activeOtp.setEmail("client.it@tcs.test");
        activeOtp.setPurpose(OtpPurpose.CONTRACT_SIGNING);
        activeOtp.setCode("123456");
        activeOtp.setAttempts(0);
        activeOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                anyString(),
                any(OtpPurpose.class)))
                .thenReturn(Optional.of(activeOtp));

        when(cccdService.getByUserId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(CccdInfoDto.builder()
                .fullName("Nguyễn Văn IT")
                .cccdNumber("012345678901")
                .dateOfBirth("01/01/1999")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build());

        ReflectionTestUtils.setField(marketplaceService, "contractEmailService", emailService);
        ReflectionTestUtils.setField(marketplaceService, "contractService", contractService);
    }

    /**
     * Test Case: IT-CCE-001
     * Title: Enroll a center student after the student escrow is funded.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.onEscrowFunded (EscrowFunded event handler).
     * Input: EscrowFunded(classStudentId=20, escrowId=30).
     * Steps:
     *   1. Prepare the fixture: Class student 20 is PENDING_SIGNATURE and an EscrowFunded event references it.
     *   2. Use the input: EscrowFunded(classStudentId=20, escrowId=30).
     *   3. Execute MarketplaceServiceImpl.onEscrowFunded (EscrowFunded event handler). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_001_EscrowFundedEventMovesCenterStudentFromPendingSignatureToEnrolled.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status and verify save/notification.
     * Expected: The class-student row changes to ENROLLED, is saved and the relevant parties receive the enrollment notification.
     * Pre-conditions: Class student 20 is PENDING_SIGNATURE and an EscrowFunded event references it.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-001: Enroll a center student after the student escrow is funded.")
    void IT_CCE_001_EscrowFundedEventMovesCenterStudentFromPendingSignatureToEnrolled() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(user(99L), TutoringClassStatus.OPEN);
        ClassStudent classStudent = classStudent(tutoringClass, clientUser);
        classStudent.setStatus(ClassStudentStatus.PENDING_SIGNATURE);

        when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.of(classStudent));

        marketplaceService.onEscrowFunded(new EscrowFunded(
                101L,
                CLASS_ID,
                CLIENT_USER_ID,
                99L,
                new BigDecimal("100000.00"),
                null,
                CLASS_STUDENT_ID));

        assertEquals(ClassStudentStatus.ENROLLED, classStudent.getStatus());
        verify(classStudentRepository).save(classStudent);
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(clientUser),
                eq(NotificationType.CLASS),
                eq("MARKETPLACE_CLASS_EVENT"),
                any(),
                eq("Ghi danh thành công"),
                eq("Học viên test đã được ghi danh thành công vào lớp \"Lớp toán\" sau khi hệ thống xác nhận thanh toán."),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
    }

    /**
     * Test Case: IT-CCE-002
     * Title: List classes owned by the center with enrollment counts.
     * Procedure: Prepare the stated fixture and input, then execute CenterServiceImpl.listMyClasses (GET /api/center/classes).
     * Input: Center session.
     * Steps:
     *   1. Prepare the fixture: Authenticated verified center owns class 10 with one enrolled student.
     *   2. Use the input: Center session.
     *   3. Execute CenterServiceImpl.listMyClasses (GET /api/center/classes). Mapped test: com.tcs.module.center.service.impl.Report52CenterClassEnrollmentITTest#IT_CCE_002_ListCenterClassesReturnsOwnedRowsWithEnrollmentCounts.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert class id/status/enrolledCount and owner-scoped repository query.
     * Expected: The center receives its OPEN class 10 and the response reports one enrolled student.
     * Pre-conditions: Authenticated verified center owns class 10 with one enrolled student.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-002: List classes owned by the center with enrollment counts.")
    void IT_CCE_002_ListCenterClassesReturnsOwnedRowsWithEnrollmentCounts() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.OPEN);
        ClassStudent student = enrolledStudent(tutoringClass, user(301L));

        loginAsCenter(center);
        stubClassResponseDependencies(tutoringClass, List.of(student));
        when(tutoringClassRepository.findByCreator_UserId(CENTER_USER_ID)).thenReturn(List.of(tutoringClass));

        List<CenterClassResponse> response = centerService.listMyClasses();

        assertEquals(1, response.size());
        assertEquals(CLASS_ID, response.get(0).getClassId());
        assertEquals(TutoringClassStatus.OPEN, response.get(0).getStatus());
        assertEquals(1, response.get(0).getEnrolledCount());
    }

    /**
     * Test Case: IT-CCE-003
     * Title: Load a center-class detail with schedule and student information.
     * Procedure: Prepare the stated fixture and input, then execute CenterServiceImpl.getMyClass (GET /api/center/classes/{classId}).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: Verified center owns class 10; schedule and student fixtures are linked.
     *   2. Use the input: classId=10.
     *   3. Execute CenterServiceImpl.getMyClass (GET /api/center/classes/{classId}). Mapped test: com.tcs.module.center.service.impl.Report52CenterClassEnrollmentITTest#IT_CCE_003_GetCenterClassDetailReturnsJoinedScheduleAndStudentData.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert title, schedule count and student display name.
     * Expected: Class 10 detail contains title Lớp Toán trung tâm, one schedule slot and student Nguyễn Minh Anh.
     * Pre-conditions: Verified center owns class 10; schedule and student fixtures are linked.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-003: Load a center-class detail with schedule and student information.")
    void IT_CCE_003_GetCenterClassDetailReturnsJoinedScheduleAndStudentData() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.OPEN);
        ClassStudent student = enrolledStudent(tutoringClass, user(301L));
        ScheduleSlot slot = scheduleSlot(tutoringClass);

        loginAsCenter(center);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        stubClassResponseDependencies(tutoringClass, List.of(student));
        when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(slot));

        CenterClassResponse response = centerService.getMyClass(CLASS_ID);

        assertEquals(CLASS_ID, response.getClassId());
        assertEquals("Lớp Toán trung tâm", response.getTitle());
        assertEquals(1, response.getSchedule().size());
        assertEquals("Nguyễn Minh Anh", response.getStudents().get(0).getStudentName());
    }

    /**
     * Test Case: IT-CCE-004
     * Title: Reject center-class creation when the title is missing.
     * Procedure: Prepare the stated fixture and input, then execute CenterServiceImpl.createClass (POST /api/center/classes).
     * Input: Valid center-class request with blank title.
     * Steps:
     *   1. Prepare the fixture: Verified center and wallet are available.
     *   2. Use the input: Valid center-class request with blank title.
     *   3. Execute CenterServiceImpl.createClass (POST /api/center/classes). Mapped test: com.tcs.module.center.service.impl.Report52CenterClassEnrollmentITTest#IT_CCE_004_RejectCenterClassCreationWhenRequiredFieldsAreMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert validation error and verify no class/audit save.
     * Expected: The service returns “Tiêu đề là bắt buộc” and does not save a class or audit row.
     * Pre-conditions: Verified center and wallet are available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-004: Reject center-class creation when the title is missing.")
    void IT_CCE_004_RejectCenterClassCreationWhenRequiredFieldsAreMissing() {
        TutorCenter center = verifiedCenter();
        SaveClassRequest request = validCenterClassRequest();
        request.setTitle("");

        loginAsCenter(center);
        when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(activeWallet()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> centerService.createClass(request));

        assertEquals("Tiêu đề là bắt buộc", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
    }

    /**
     * Test Case: IT-CCE-005
     * Title: Reject publishing a center class that is not in DRAFT state.
     * Procedure: Prepare the stated fixture and input, then execute CenterServiceImpl.publishClass (POST /api/center/classes/{classId}/publish).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: Center owns class 10 in IN_PROGRESS state.
     *   2. Use the input: classId=10.
     *   3. Execute CenterServiceImpl.publishClass (POST /api/center/classes/{classId}/publish). Mapped test: com.tcs.module.center.service.impl.Report52CenterClassEnrollmentITTest#IT_CCE_005_RejectPublishingCenterClassInIllegalState.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exact illegal-state message and verify no save.
     * Expected: An IN_PROGRESS class cannot be published and remains unchanged.
     * Pre-conditions: Center owns class 10 in IN_PROGRESS state.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-005: Reject publishing a center class that is not in DRAFT state.")
    void IT_CCE_005_RejectPublishingCenterClassInIllegalState() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.IN_PROGRESS);

        loginAsCenter(center);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> centerService.publishClass(CLASS_ID));

        assertEquals("Chỉ lớp ở trạng thái nháp mới có thể đăng tải", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CCE-006
     * Title: Block anonymous enrollment in a center class before creating a student row.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: No authenticated user.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_006_BlockAnonymousCenterEnrollmentBeforeStudentRecordCreation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify both writes are skipped.
     * Expected: The service returns “Yêu cầu đăng nhập” and does not save ClassStudent or generate a contract.
     * Pre-conditions: No authenticated user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-006: Block anonymous enrollment in a center class before creating a student row.")
    void IT_CCE_006_BlockAnonymousCenterEnrollmentBeforeStudentRecordCreation() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.registerToClass(CLASS_ID));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(classStudentRepository, never()).save(any());
        verify(contractService, never()).generateStudentContract(any());
    }

    /**
     * Test Case: IT-CCE-007
     * Title: Prevent a tutor from registering as a student in a center class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: Authenticated user is a verified tutor; class 10 is an OPEN center class.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_007_BlockTutorFromSelfRegisteringCenterClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role error and verify no student/contract saves.
     * Expected: The tutor-only registration is rejected and no student/contract row is created.
     * Pre-conditions: Authenticated user is a verified tutor; class 10 is an OPEN center class.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-007: Prevent a tutor from registering as a student in a center class.")
    void IT_CCE_007_BlockTutorFromSelfRegisteringCenterClass() {
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        TutoringClass tutoringClass = centerClass(user(99L), TutoringClassStatus.OPEN);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.registerToClass(CLASS_ID));

        assertEquals("Lớp của trung tâm do trung tâm tự bố trí gia sư — gia sư không thể tự đăng ký.",
                exception.getMessage());
        verify(tutorApplicationRepository, never()).save(any());
        verify(classStudentRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CCE-008
     * Title: Prevent duplicate center enrollment from creating another student contract.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: Client already has a student row for class 10.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_008_PreventDuplicateCenterEnrollmentFromCreatingSecondStudentContract.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert duplicate error and verify ClassStudent/contract saves are skipped.
     * Expected: An existing student email for class 10 causes “Bạn đã đăng ký lớp này rồi” and no second contract.
     * Pre-conditions: Client already has a student row for class 10.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-008: Prevent duplicate center enrollment from creating another student contract.")
    void IT_CCE_008_PreventDuplicateCenterEnrollmentFromCreatingSecondStudentContract() {
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        TutoringClass tutoringClass = centerClass(user(99L), TutoringClassStatus.OPEN);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndStudentEmail(CLASS_ID, clientUser.getEmail()))
                .thenReturn(true);

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.registerToClass(CLASS_ID));

        assertEquals("Bạn đã đăng ký lớp này rồi", exception.getMessage());
        verify(classStudentRepository, never()).save(any());
        verify(contractService, never()).generateStudentContract(any());
    }

    /**
     * Test Case: IT-CCE-009
     * Title: Reject a second enrollment of the same student in the same center class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: The repository reports an existing enrolled student for the current email.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_009_RejectDuplicateStudentEnrollmentForSameCenterClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert message and verify no duplicate persistence.
     * Expected: The duplicate enrollment is rejected before another ClassStudent or contract is saved.
     * Pre-conditions: The repository reports an existing enrolled student for the current email.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-009: Reject a second enrollment of the same student in the same center class.")
    void IT_CCE_009_RejectDuplicateStudentEnrollmentForSameCenterClass() {
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        TutoringClass tutoringClass = centerClass(user(99L), TutoringClassStatus.OPEN);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndStudentEmail(CLASS_ID, clientUser.getEmail()))
                .thenReturn(true);

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.registerToClass(CLASS_ID));

        assertEquals("Bạn đã đăng ký lớp này rồi", exception.getMessage());
        verify(classStudentRepository, never()).save(any());
        verify(contractService, never()).generateStudentContract(any());
    }

    /**
     * Test Case: IT-CCE-010
     * Title: Create a pending student enrollment and audit the registration.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: Authenticated client has a Client profile and class 10 is open with capacity.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_010_RegisterCenterClassCreatesPendingStudentRecordAndAuditTrail.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture saved student, assert status/owner and verify CREATE enrollment audit.
     * Expected: A PENDING_SIGNATURE ClassStudent is saved for the client and the response tells the client to sign/pay in Contracts.
     * Pre-conditions: Authenticated client has a Client profile and class 10 is open with capacity.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-010: Create a pending student enrollment and audit the registration.")
    void IT_CCE_010_RegisterCenterClassCreatesPendingStudentRecordAndAuditTrail() {
        User centerUser = user(99L);
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        TutoringClass tutoringClass = centerClass(centerUser, TutoringClassStatus.OPEN);

        stubSuccessfulCenterEnrollment(clientUser, client, tutoringClass);

        String message = marketplaceService.registerToClass(CLASS_ID);

        assertTrue(message.contains("Vui lòng vào mục Hợp đồng để ký và thanh toán"));
        ArgumentCaptor<ClassStudent> studentCaptor = ArgumentCaptor.forClass(ClassStudent.class);
        verify(classStudentRepository).save(studentCaptor.capture());
        assertEquals(ClassStudentStatus.PENDING_SIGNATURE, studentCaptor.getValue().getStatus());
        assertEquals(clientUser, studentCaptor.getValue().getEnrolledByUser());
        verify(auditLogService).record(
                eq(CLIENT_USER_ID),
                eq("REGISTER_CLASS"),
                eq("ClassStudent"),
                eq(CLASS_STUDENT_ID),
                eq(null),
                any());
    }

    /**
     * Test Case: IT-CCE-011
     * Title: Notify the client that a center enrollment contract needs signing.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: A valid center-class registration creates a pending student row.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_011_RegisterCenterClassSendsContractNotificationToClient.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture recipient, template, text, reference type and class id.
     * Expected: The client receives the “Cần ký hợp đồng lớp học” notification with CONTRACT reference.
     * Pre-conditions: A valid center-class registration creates a pending student row.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-011: Notify the client that a center enrollment contract needs signing.")
    void IT_CCE_011_RegisterCenterClassSendsContractNotificationToClient() {
        User centerUser = user(99L);
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        TutoringClass tutoringClass = centerClass(centerUser, TutoringClassStatus.OPEN);

        stubSuccessfulCenterEnrollment(clientUser, client, tutoringClass);

        marketplaceService.registerToClass(CLASS_ID);

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(clientUser),
                eq(NotificationType.CLASS),
                eq("MARKETPLACE_CLASS_EVENT"),
                any(),
                eq("Cần ký hợp đồng lớp học"),
                anyString(),
                eq("CONTRACT"),
                eq(CLASS_ID));
    }

    /**
     * Test Case: IT-CCE-012
     * Title: Keep center enrollment pending when the student contract is signed but escrow is not funded.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.onStudentContractSigned (StudentContractSigned event handler).
     * Input: StudentContractSigned(classStudentId=20, contractId=880).
     * Steps:
     *   1. Prepare the fixture: Class student 20 is pending and the student contract signed event has no funded escrow.
     *   2. Use the input: StudentContractSigned(classStudentId=20, contractId=880).
     *   3. Execute MarketplaceServiceImpl.onStudentContractSigned (StudentContractSigned event handler). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_012_StudentContractSignedKeepsEnrollmentPendingUntilEscrowIsFunded.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status and verify no student save; audit explains the pending state.
     * Expected: The ClassStudent remains PENDING_SIGNATURE and is not saved as ENROLLED until escrow funding arrives.
     * Pre-conditions: Class student 20 is pending and the student contract signed event has no funded escrow.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-012: Keep center enrollment pending when the student contract is signed but escrow is not funded.")
    void IT_CCE_012_StudentContractSignedKeepsEnrollmentPendingUntilEscrowIsFunded() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = centerClass(user(99L), TutoringClassStatus.OPEN);
        ClassStudent classStudent = classStudent(tutoringClass, clientUser);
        classStudent.setStatus(ClassStudentStatus.PENDING_SIGNATURE);

        when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.of(classStudent));

        marketplaceService.onStudentContractSigned(new StudentContractSigned(CLASS_STUDENT_ID, 880L));

        assertEquals(ClassStudentStatus.PENDING_SIGNATURE, classStudent.getStatus());
        verify(auditLogService).record(
                eq(CLIENT_USER_ID),
                eq("STUDENT_CONTRACT_SIGNED_WAIT_PAYMENT"),
                eq("ClassStudent"),
                eq(CLASS_STUDENT_ID),
                eq(null),
                any());
        verify(classStudentRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CCE-013
     * Title: Generate an enrollment contract when a client registers for a center class.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: The center class registration passes validation and creates a pending student row.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_013_RegisterCenterClassGeneratesEnrollmentContractForStudent.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Verify contract generation receives the saved student id.
     * Expected: ContractService.generateStudentContract is called with the newly created class-student id.
     * Pre-conditions: The center class registration passes validation and creates a pending student row.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-013: Generate an enrollment contract when a client registers for a center class.")
    void IT_CCE_013_RegisterCenterClassGeneratesEnrollmentContractForStudent() {
        User centerUser = user(99L);
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        TutoringClass tutoringClass = centerClass(centerUser, TutoringClassStatus.OPEN);

        stubSuccessfulCenterEnrollment(clientUser, client, tutoringClass);

        marketplaceService.registerToClass(CLASS_ID);

        verify(contractService).generateStudentContract(CLASS_STUDENT_ID);
    }

    /**
     * Test Case: IT-CCE-014
     * Title: Use each student’s tuition when preparing the center escrow command.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=902; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Client CCCD is complete; the center student contract is fully signed and no escrow exists.
     *   2. Use the input: contractId=902; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CCE_014_StudentEnrollmentContractUsesPerStudentTuitionForEscrowCommand.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture EscrowLockCommand and compare payer, classStudent and amount.
     * Expected: The escrow command uses the client as payer, classStudent 88 and amount 600000 (120000 x 5 sessions).
     * Pre-conditions: Client CCCD is complete; the center student contract is fully signed and no escrow exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-014: Use each student’s tuition when preparing the center escrow command.")
    void IT_CCE_014_StudentEnrollmentContractUsesPerStudentTuitionForEscrowCommand() {
        Contract studentContract = studentEnrollmentContract();
        ContractSignature clientSignature = pendingClientSignature(studentContract);
        ContractSignature centerSignature = signedCenterSignature(studentContract);
        User clientUser = studentContract.getClassStudent().getEnrolledByUser();
        activeOtp.setEmail(clientUser.getEmail());

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(cccdService.isComplete(clientUser.getUserId())).thenReturn(true);
        when(contractRepository.findById(902L)).thenReturn(Optional.of(studentContract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(902L, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractId(902L)).thenReturn(List.of(clientSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(902L)).thenReturn(2);
        when(userRepository.findById(clientUser.getUserId())).thenReturn(Optional.of(clientUser));
        when(tutorCenterRepository.findByUser_UserId(100L)).thenReturn(Optional.of(studentCenter()));
        when(clientRepository.findByUser_UserId(clientUser.getUserId())).thenReturn(Optional.empty());

        contractServiceImpl.signWithOtp(902L, otp("123456"));

        ArgumentCaptor<EscrowLockCommand> commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(clientUser.getUserId(), commandCaptor.getValue().payerUserId());
        assertEquals(88L, commandCaptor.getValue().classStudentId());
        assertEquals(new BigDecimal("600000.00"), commandCaptor.getValue().amount());
    }

    /**
     * Test Case: IT-CCE-015
     * Title: Preserve the number of center classes returned for the current center.
     * Procedure: Prepare the stated fixture and input, then execute CenterServiceImpl.listMyClasses (GET /api/center/classes).
     * Input: Authenticated center session.
     * Steps:
     *   1. Prepare the fixture: Center owns two classes with response dependencies available.
     *   2. Use the input: Authenticated center session.
     *   3. Execute CenterServiceImpl.listMyClasses (GET /api/center/classes). Mapped test: com.tcs.module.center.service.impl.Report52CenterClassEnrollmentITTest#IT_CCE_015_ListCenterClassesKeepsRepositoryRowCountForCurrentCenter.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response count 2 and verify owner query calls.
     * Expected: Two repository rows produce two response rows; the center-scoped query is used.
     * Pre-conditions: Center owns two classes with response dependencies available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-015: Preserve the number of center classes returned for the current center.")
    void IT_CCE_015_ListCenterClassesKeepsRepositoryRowCountForCurrentCenter() {
        TutorCenter center = verifiedCenter();
        TutoringClass first = centerClass(center, TutoringClassStatus.OPEN);
        TutoringClass second = centerClass(center, TutoringClassStatus.MATCHED);
        second.setClassId(CLASS_ID + 1);

        loginAsCenter(center);
        stubClassResponseDependencies(first, List.of());
        stubClassResponseDependencies(second, List.of());
        when(tutoringClassRepository.findByCreator_UserId(CENTER_USER_ID)).thenReturn(List.of(first, second));

        List<CenterClassResponse> response = centerService.listMyClasses();

        assertEquals(2, response.size());
        verify(tutoringClassRepository, org.mockito.Mockito.times(2)).findByCreator_UserId(CENTER_USER_ID);
    }

    /**
     * Test Case: IT-CCE-016
     * Title: Auto-release funded center escrow after all lessons are completed without an issue.
     * Procedure: Prepare the stated fixture and input, then execute CenterEscrowAutoSettlementService.trySettleCompletedCenterClass (scheduled settlement).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: Center class has one enrolled student, all lesson attendance is completed, escrow is FUNDED and no report exists.
     *   2. Use the input: classId=10.
     *   3. Execute CenterEscrowAutoSettlementService.trySettleCompletedCenterClass (scheduled settlement). Mapped test: com.tcs.module.finance.service.Report52CenterEscrowAutoSettlementITTest#IT_CCE_016_ReleaseFundedCenterEscrowWhenAllLessonsAreCompletedWithoutIssue.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture release instruction, amount and final class status.
     * Expected: The method returns true, applies a full release instruction for escrow 30, and marks the center class COMPLETED.
     * Pre-conditions: Center class has one enrolled student, all lesson attendance is completed, escrow is FUNDED and no report exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-016: Auto-release funded center escrow after all lessons are completed without an issue.")
    void IT_CCE_016_ReleaseFundedCenterEscrowWhenAllLessonsAreCompletedWithoutIssue() {
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
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(anyList())).thenReturn(List.of(attendance));
        when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(escrow));

        boolean released = settlementService.trySettleCompletedCenterClass(CLASS_ID);

        assertTrue(released);
        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        assertTrue(ESCROW_ID.equals(instructionCaptor.getValue().escrowId()));
        assertTrue(new BigDecimal("100000.00").compareTo(instructionCaptor.getValue().releaseToBeneficiary()) == 0);
        assertTrue(BigDecimal.ZERO.compareTo(instructionCaptor.getValue().refundToPayer()) == 0);
        assertTrue(tutoringClass.getStatus() == TutoringClassStatus.COMPLETED);
        verify(tutoringClassRepository).save(tutoringClass);
    }

    /**
     * Test Case: IT-CCE-017
     * Title: Skip center auto-release while a class report is still pending.
     * Procedure: Prepare the stated fixture and input, then execute CenterEscrowAutoSettlementService.trySettleCompletedCenterClass (scheduled settlement).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: All lessons appear complete but an open/pending report targets the class.
     *   2. Use the input: classId=10.
     *   3. Execute CenterEscrowAutoSettlementService.trySettleCompletedCenterClass (scheduled settlement). Mapped test: com.tcs.module.finance.service.Report52CenterEscrowAutoSettlementITTest#IT_CCE_017_SkipAutoReleaseWhenClassHasPendingReport.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert false and verify escrowService.apply/class save are never called.
     * Expected: The method returns false and neither escrow settlement nor class completion is performed.
     * Pre-conditions: All lessons appear complete but an open/pending report targets the class.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-017: Skip center auto-release while a class report is still pending.")
    void IT_CCE_017_SkipAutoReleaseWhenClassHasPendingReport() {
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
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(anyList())).thenReturn(List.of(attendance));
        when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(escrow));
        when(reportRepository.existsByTargetTypeAndTargetIdAndStatus(
                ReportTargetType.CLASS,
                CLASS_ID,
                ReportStatus.PENDING))
                .thenReturn(true);

        boolean released = settlementService.trySettleCompletedCenterClass(CLASS_ID);

        assertFalse(released);
        verify(escrowService, never()).apply(org.mockito.ArgumentMatchers.any());
        verify(tutoringClassRepository, never()).save(tutoringClass);
    }

    /**
     * Test Case: IT-CCE-018
     * Title: Cancel an expired open center class when the minimum student count is not reached.
     * Procedure: Prepare the stated fixture and input, then execute CenterServiceImpl.listMyClasses (GET /api/center/classes).
     * Input: Center class list request.
     * Steps:
     *   1. Prepare the fixture: Class 10 is OPEN, enrollment deadline has passed and minStudents=2 but fewer students are enrolled.
     *   2. Use the input: Center class list request.
     *   3. Execute CenterServiceImpl.listMyClasses (GET /api/center/classes). Mapped test: com.tcs.module.center.service.impl.Report52CenterClassEnrollmentITTest#IT_CCE_018_ExpiredOpenEnrollmentAutoCancelsClassWhenMinimumStudentsNotReached.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert CANCELLED and verify class save.
     * Expected: The expired OPEN class changes to CANCELLED when enrolled students are below minStudents.
     * Pre-conditions: Class 10 is OPEN, enrollment deadline has passed and minStudents=2 but fewer students are enrolled.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-018: Cancel an expired open center class when the minimum student count is not reached.")
    void IT_CCE_018_ExpiredOpenEnrollmentAutoCancelsClassWhenMinimumStudentsNotReached() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.OPEN);
        tutoringClass.setMinStudents(2);
        tutoringClass.setEnrollmentDeadline(LocalDate.now().minusDays(1));

        loginAsCenter(center);
        stubClassResponseDependencies(tutoringClass, List.of());
        when(tutoringClassRepository.findByCreator_UserId(CENTER_USER_ID)).thenReturn(List.of(tutoringClass));
        when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(1L);

        centerService.listMyClasses();

        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());
        verify(tutoringClassRepository).save(tutoringClass);
    }

    /**
     * Test Case: IT-CCE-019
     * Title: Include contract context in the center enrollment notification.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: A valid client registration creates a pending center student contract.
     *   2. Use the input: classId=10.
     *   3. Execute MarketplaceServiceImpl.registerToClass (POST /api/marketplace/classes/{classId}/register). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CCE_019_CenterEnrollmentNotificationUsesContractContextForFrontendNavigation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification reference fields and recipient.
     * Expected: The enrollment notification carries referenceType CONTRACT and class id 10 for frontend navigation.
     * Pre-conditions: A valid client registration creates a pending center student contract.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-019: Include contract context in the center enrollment notification.")
    void IT_CCE_019_CenterEnrollmentNotificationUsesContractContextForFrontendNavigation() {
        User centerUser = user(99L);
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        TutoringClass tutoringClass = centerClass(centerUser, TutoringClassStatus.OPEN);

        stubSuccessfulCenterEnrollment(clientUser, client, tutoringClass);

        marketplaceService.registerToClass(CLASS_ID);

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(clientUser),
                eq(NotificationType.CLASS),
                eq("MARKETPLACE_CLASS_EVENT"),
                any(),
                eq("Cần ký hợp đồng lớp học"),
                anyString(),
                eq("CONTRACT"),
                eq(CLASS_ID));
    }

    /**
     * Test Case: IT-CCE-020
     * Title: Activate a center class and notify the tutor and enrolled clients.
     * Procedure: Prepare the stated fixture and input, then execute CenterServiceImpl.activateClass (POST /api/center/classes/{classId}/activate).
     * Input: classId=10.
     * Steps:
     *   1. Prepare the fixture: Center owns an OPEN class with the required enrolled students, active assignment and funded state.
     *   2. Use the input: classId=10.
     *   3. Execute CenterServiceImpl.activateClass (POST /api/center/classes/{classId}/activate). Mapped test: com.tcs.module.center.service.impl.Report52CenterClassEnrollmentITTest#IT_CCE_020_ActivateCenterClassUpdatesClassStatusAndNotifiesTutorAndClient.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response status and verify both notification calls.
     * Expected: The class becomes IN_PROGRESS and both tutor and client receive class-activation notifications.
     * Pre-conditions: Center owns an OPEN class with the required enrolled students, active assignment and funded state.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CCE-020: Activate a center class and notify the tutor and enrolled clients.")
    void IT_CCE_020_ActivateCenterClassUpdatesClassStatusAndNotifiesTutorAndClient() {
        TutorCenter center = verifiedCenter();
        TutoringClass tutoringClass = centerClass(center, TutoringClassStatus.MATCHED);
        TutoringClass activeClass = centerClass(center, TutoringClassStatus.IN_PROGRESS);
        Tutor tutor = tutor(user(201L));
        ClassAssignment assignment = assignment(tutoringClass, tutor);
        ClassStudent student = enrolledStudent(tutoringClass, user(301L));

        loginAsCenter(center);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classStudentRepository.countByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(1L);
        when(tutoringClassRepository.save(tutoringClass)).thenAnswer(invocation -> {
            tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
            return activeClass;
        });
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of(student));
        stubClassResponseDependencies(activeClass, List.of(student));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));

        CenterClassResponse response = centerService.activateClass(CLASS_ID);

        assertEquals(TutoringClassStatus.IN_PROGRESS, response.getStatus());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutor.getUser()),
                eq(NotificationType.CLASS),
                eq("CENTER_CLASS_STARTED"),
                any(),
                eq("Lớp học đã bắt đầu"),
                any(),
                eq("CENTER_CLASS"),
                eq(CLASS_ID));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(student.getEnrolledByUser()),
                eq(NotificationType.CLASS),
                eq("CENTER_CLASS_STARTED"),
                any(),
                eq("Lớp học đã bắt đầu"),
                any(),
                eq("CENTER_CLASS"),
                eq(CLASS_ID));
    }



    private void loginAsCenter(TutorCenter center) {
        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
    }

    private void stubClassResponseDependencies(TutoringClass tutoringClass, List<ClassStudent> students) {
        when(classStudentRepository.existsByTutoringClass_ClassId(tutoringClass.getClassId())).thenReturn(!students.isEmpty());
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                tutoringClass.getClassId(), ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());
        when(substitutionService.findAssistant(tutoringClass.getClassId())).thenReturn(Optional.empty());
        when(scheduleSlotRepository.findByTutoringClass_ClassId(tutoringClass.getClassId())).thenReturn(List.of());
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                tutoringClass.getClassId(), ClassStudentStatus.ENROLLED)).thenReturn(students);
        when(systemParameterRepository.findByParamKey("classorigin:" + tutoringClass.getClassId()))
                .thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("classtpl:" + tutoringClass.getClassId()))
                .thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("classterms:" + tutoringClass.getClassId()))
                .thenReturn(Optional.empty());
        when(centerEscrowAutoSettlementService.isTutorConfirmed(tutoringClass.getClassId())).thenReturn(false);
    }

    private void stubCatalogLookups() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setName("Lớp phổ thông");
        Subject subject = new Subject();
        subject.setSubjectId(2L);
        subject.setSubjectName("Toán");
        Grade grade = new Grade();
        grade.setGradeId(3L);
        grade.setGradeName("Lớp 9");
        Province province = new Province();
        province.setProvinceId(4L);
        province.setProvinceName("Hà Nội");
        Location location = new Location();
        location.setLocationId(5L);
        location.setProvince(province);
        location.setWardName("Cầu Giấy");
        location.setAddressLine("Số 15 Trần Duy Hưng");

        when(categoryRepository.findByNameIgnoreCase("Lớp phổ thông")).thenReturn(Optional.of(category));
        when(subjectRepository.findFirstBySubjectNameIgnoreCase("Toán")).thenReturn(Optional.of(subject));
        when(gradeRepository.findFirstByGradeNameIgnoreCase("Lớp 9")).thenReturn(Optional.of(grade));
        when(provinceRepository.findFirstByProvinceNameIgnoreCase("Hà Nội")).thenReturn(Optional.of(province));
        when(locationRepository.findFirstByProvince_ProvinceIdAndWardNameIgnoreCaseAndAddressLineIgnoreCase(
                4L, "Cầu Giấy", "Số 15 Trần Duy Hưng")).thenReturn(Optional.of(location));
    }

    private SaveClassRequest validCenterClassRequest() {
        SaveClassRequest request = new SaveClassRequest();
        request.setTitle("Lớp Toán trung tâm");
        request.setDescription("Ôn tập kiến thức Toán lớp 9");
        request.setCategoryName("Lớp phổ thông");
        request.setSubjectName("Toán");
        request.setGradeName("Lớp 9");
        request.setProvinceName("Hà Nội");
        request.setWardName("Cầu Giấy");
        request.setAddressDetail("Số 15 Trần Duy Hưng");
        request.setLessonMode(LessonMode.OFFLINE);
        request.setRecurringType(RecurringType.WEEKLY);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(15));
        request.setTuitionFee(new BigDecimal("1200000.00"));
        request.setMaxStudents(10);
        request.setMinStudents(1);
        request.setSchedule(List.of(scheduleRequest(2), scheduleRequest(4)));
        return request;
    }

    private ScheduleSlotRequest scheduleRequest(int dayOfWeek) {
        ScheduleSlotRequest request = new ScheduleSlotRequest();
        request.setDayOfWeek(dayOfWeek);
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(19, 30));
        return request;
    }

    private ScheduleSlot scheduleSlot(TutoringClass tutoringClass) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(30L);
        slot.setTutoringClass(tutoringClass);
        slot.setDayOfWeek(2);
        slot.setStartTime(LocalTime.of(18, 0));
        slot.setEndTime(LocalTime.of(19, 30));
        return slot;
    }

    private TutorCenter verifiedCenter() {
        User user = user(CENTER_USER_ID);
        TutorCenter center = new TutorCenter();
        center.setCenterId(10L);
        center.setUser(user);
        center.setCompanyName("Trung tâm Minh Tâm");
        center.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        return center;
    }

    private TutoringClass centerClass(TutorCenter center, TutoringClassStatus status) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(center.getUser());
        tutoringClass.setCenter(center);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setTitle("Lớp Toán trung tâm");
        tutoringClass.setDescription("Ôn tập Toán lớp 9");
        tutoringClass.setStatus(status);
        tutoringClass.setLessonMode(LessonMode.OFFLINE);
        tutoringClass.setRecurringType(RecurringType.WEEKLY);
        tutoringClass.setStartDate(LocalDate.now().plusDays(1));
        tutoringClass.setEndDate(LocalDate.now().plusDays(15));
        tutoringClass.setTuitionFee(new BigDecimal("1200000.00"));
        tutoringClass.setMaxStudents(10);
        tutoringClass.setMinStudents(1);
        return tutoringClass;
    }

    private ClassStudent enrolledStudent(TutoringClass tutoringClass, User enrolledBy) {
        ClassStudent student = new ClassStudent();
        student.setClassStudentId(700L);
        student.setTutoringClass(tutoringClass);
        student.setEnrolledByUser(enrolledBy);
        student.setStudentName("Nguyễn Minh Anh");
        student.setStudentPhone("0900000001");
        student.setStatus(ClassStudentStatus.ENROLLED);
        return student;
    }

    private ClassAssignment assignment(TutoringClass tutoringClass, Tutor tutor) {
        TutorApplication application = new TutorApplication();
        application.setApplicationId(90L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        application.setStatus(TutorApplicationStatus.ACCEPTED);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(91L);
        assignment.setApplication(application);
        assignment.setTutor(tutor);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        return assignment;
    }

    private Tutor tutor(User user) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(user);
        tutor.setFullName("Lê Hoàng Nam");
        return tutor;
    }

    private Wallet activeWallet() {
        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.test");
        return user;
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

    private TutoringClass centerClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = tutoringClass(creator, status);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.CENTER);
        tutoringClass.setMaxStudents(20);
        return tutoringClass;
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

    

    private CccdInfoDto completeCccd(String fullName, String cccdNumber) {
        return CccdInfoDto.builder()
                .fullName(fullName)
                .cccdNumber(cccdNumber)
                .dateOfBirth("01/01/2000")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build();
    }

    

    private Contract studentEnrollmentContract() {
        User centerUser = new User();
        centerUser.setUserId(100L);
        centerUser.setEmail("center.it@tcs.test");
        User clientUser = new User();
        clientUser.setUserId(300L);
        clientUser.setEmail("client.it@tcs.test");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(500L);
        tutoringClass.setCreator(centerUser);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setTitle("Lớp Toán trung tâm");
        tutoringClass.setTuitionFee(new BigDecimal("120000.00"));
        tutoringClass.setNumberOfSessions(5);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(88L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(clientUser);
        classStudent.setStudentName("Nguyễn Minh Anh");

        Contract studentContract = new Contract();
        studentContract.setContractId(902L);
        studentContract.setContractNo("HD-STUDENT-IT");
        studentContract.setStatus(ContractStatus.PENDING);
        studentContract.setClassStudent(classStudent);
        studentContract.setSourceType(com.tcs.module.contract.enums.ContractSourceType.CENTER);
        return studentContract;
    }

    

    private ContractSignature pendingClientSignature(Contract targetContract) {
        User clientUser = targetContract.getClassStudent().getEnrolledByUser();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(11L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.CLIENT);
        signature.setEmail(clientUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.PENDING);
        return signature;
    }

    private ContractSignature signedCenterSignature(Contract targetContract) {
        User centerUser = targetContract.getClassStudent().getTutoringClass().getCreator();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(12L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.CENTER);
        signature.setSigner(centerUser);
        signature.setEmail(centerUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now().minusHours(1));
        return signature;
    }

    

    private TutorCenter studentCenter() {
        User centerUser = new User();
        centerUser.setUserId(100L);
        centerUser.setEmail("center.it@tcs.test");
        TutorCenter center = new TutorCenter();
        center.setCenterId(1L);
        center.setUser(centerUser);
        center.setCompanyName("Trung tâm IT");
        return center;
    }

    

    private SignWithOtpRequest otp(String code) {
        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode(code);
        return request;
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

    

    private ClassStudent classStudent(TutoringClass tutoringClass, User enrolledUser) {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(CLASS_STUDENT_ID);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(enrolledUser);
        classStudent.setStudentName("Học viên test");
        classStudent.setStudentEmail(enrolledUser.getEmail());
        classStudent.setStatus(ClassStudentStatus.ENROLLED);
        return classStudent;
    }

    

    private void stubSuccessfulCenterEnrollment(User clientUser, Client client, TutoringClass tutoringClass) {
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client));
        when(classStudentRepository.existsByTutoringClass_ClassIdAndStudentEmail(CLASS_ID, clientUser.getEmail()))
                .thenReturn(false);
        when(clientLegalAccountService.resolveForClient(client)).thenReturn(
                ClientLegalAccountService.LegalAccountContext.builder()
                        .sessionUserId(CLIENT_USER_ID)
                        .legalUserId(CLIENT_USER_ID)
                        .legalHolderName(client.getFullName())
                        .legalHolderEmail(clientUser.getEmail())
                        .delegatedToParent(false)
                        .build());
        when(classStudentRepository.save(any(ClassStudent.class))).thenAnswer(invocation -> {
            ClassStudent saved = invocation.getArgument(0);
            saved.setClassStudentId(CLASS_STUDENT_ID);
            return saved;
        });
    }
}
