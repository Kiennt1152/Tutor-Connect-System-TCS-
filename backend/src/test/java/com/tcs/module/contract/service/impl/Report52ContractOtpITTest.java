package com.tcs.module.contract.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.CooperationContractSigned;
import com.tcs.common.event.EscrowFunded;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
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
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.OtpPurpose;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52ContractOtpITTest {


    private static final Long TUTOR_USER_ID = 200L;
    private static final Long STRANGER_USER_ID = 999L;
    private static final Long CONTRACT_ID = 900L;
    private static final String TUTOR_EMAIL = "tutor.it@tcs.test";

    @Mock private AuthHelper authHelper;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private ContractTemplateRepository contractTemplateRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private EmailService emailService;
    @Mock private EscrowService escrowService;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ReviewRepository reviewRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private ReputationHistoryRepository reputationHistoryRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private CccdService cccdService;
    @Mock private EmailOtpRepository emailOtpRepository;

    @InjectMocks private ContractServiceImpl contractService;

    private Contract contract;
    private ContractSignature tutorSignature;
    private EmailOtp activeOtp;

    @BeforeEach
    void setUpContractOtpItFixture() {
        ReflectionTestUtils.setField(contractService, "otpService", new OtpService(emailOtpRepository));

        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail(TUTOR_EMAIL);
        Tutor tutor = new Tutor();
        tutor.setTutorId(20L);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư IT");

        User centerUser = new User();
        centerUser.setUserId(100L);
        centerUser.setEmail("center.it@tcs.test");
        TutorCenter center = new TutorCenter();
        center.setCenterId(1L);
        center.setUser(centerUser);
        center.setCompanyName("Trung tâm IT");
        center.setAddress("Hà Nội");
        center.setPhone("0900000000");
        RecruitmentPost post = new RecruitmentPost();
        post.setRecruitmentId(300L);
        post.setCenter(center);
        RecruitmentApplication application = new RecruitmentApplication();
        application.setRecruitmentAppId(400L);
        application.setTutor(tutor);
        application.setRecruitmentPost(post);

        contract = new Contract();
        contract.setContractId(CONTRACT_ID);
        contract.setContractNo("HD-IT-001");
        contract.setStatus(ContractStatus.PENDING);
        contract.setRecruitmentApplication(application);

        tutorSignature = new ContractSignature();
        tutorSignature.setSignatureId(1L);
        tutorSignature.setContract(contract);
        tutorSignature.setPartyRole(PartyRole.TUTOR);
        tutorSignature.setSignatureStatus(ContractSignatureStatus.PENDING);
        tutorSignature.setEmail(TUTOR_EMAIL);

        activeOtp = new EmailOtp();
        activeOtp.setEmail(TUTOR_EMAIL);
        activeOtp.setPurpose(OtpPurpose.CONTRACT_SIGNING);
        activeOtp.setCode("123456");
        activeOtp.setAttempts(0);
        activeOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(cccdService.isComplete(TUTOR_USER_ID)).thenReturn(true);
        when(cccdService.getByUserId(anyLong())).thenReturn(CccdInfoDto.builder()
                .fullName("Nguyễn Văn IT")
                .cccdNumber("012345678901")
                .dateOfBirth("01/01/1999")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build());
        when(clientRepository.findByUser_UserId(anyLong())).thenReturn(Optional.empty());
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));
        when(emailOtpRepository.findFirstByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                anyString(),
                any(OtpPurpose.class)))
                .thenReturn(Optional.of(activeOtp));
    }

    
    private static final Long CLASS_ID = 5L;
    private static final Long ASSIGNMENT_ID = 7L;
    private static final Long CLIENT_USER_ID = 11L;

    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private FavoriteTutorRepository favoriteTutorRepository;
    @Mock private ScheduleSlotRepository scheduleSlotRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private TutorSubjectRepository tutorSubjectRepository;
    @Mock private LessonRescheduleRequestRepository rescheduleRequestRepository;
    @Mock private LessonReminderService lessonReminderService;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private OtpService otpService;
    @Mock private PenaltyAccessService penaltyAccessService;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;


    /**
     * Test Case: IT-CT-001
     * Title: Sign a contract with a valid OTP and store signer data.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Tutor is the current signer and an unexpired matching OTP exists.
     *   2. Use the input: contractId=900; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_001_SuccessfulOtpSignatureStoresSignerAndSignatureData.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert signature status/signer/time/data and verify signature save.
     * Expected: The pending tutor signature becomes SIGNED, stores the tutor and timestamp, and records OTP_VERIFIED signature data.
     * Pre-conditions: Tutor is the current signer and an unexpired matching OTP exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-001: Sign a contract with a valid OTP and store signer data.")
    void IT_CT_001_SuccessfulOtpSignatureStoresSignerAndSignatureData() {
        User tutorUser = tutorUser();
        ContractSignature centerSignature = signedCenterSignature();

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(1);

        contractService.signWithOtp(CONTRACT_ID, otp("123456"));

        assertEquals(ContractSignatureStatus.SIGNED, tutorSignature.getSignatureStatus());
        assertEquals(tutorUser, tutorSignature.getSigner());
        assertNotNull(tutorSignature.getSignedAt());
        assertTrue(tutorSignature.getSignatureData().startsWith("OTP_VERIFIED:" + TUTOR_EMAIL));
        verify(contractSignatureRepository).save(tutorSignature);
    }

    /**
     * Test Case: IT-CT-002
     * Title: Return signed and pending signature slots for the contract page.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getSignatures (GET /api/contract/{contractId}/signatures).
     * Input: contractId=900.
     * Steps:
     *   1. Prepare the fixture: Contract 900 has one signed and one pending signature.
     *   2. Use the input: contractId=900.
     *   3. Execute ContractServiceImpl.getSignatures (GET /api/contract/{contractId}/signatures). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_002_GetSignatureListReturnsSignedAndPendingSlotsForContractPage.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert contract id, counts, fullySigned=false and signature display fields.
     * Expected: The response reports two required signatures, one signed signature and the correct current-user/party label.
     * Pre-conditions: Contract 900 has one signed and one pending signature.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-002: Return signed and pending signature slots for the contract page.")
    void IT_CT_002_GetSignatureListReturnsSignedAndPendingSlotsForContractPage() {
        ContractSignature centerSignature = signedCenterSignature();
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, centerSignature));

        var response = contractService.getSignatures(CONTRACT_ID);

        assertEquals(CONTRACT_ID, response.getContractId());
        assertEquals(2, response.getRequiredSignatures());
        assertEquals(1, response.getSignedCount());
        assertEquals(false, response.isFullySigned());
        assertEquals(true, response.getSignatures().get(0).isCurrentUser());
        assertEquals("Gia sư", response.getSignatures().get(0).getPartyLabel());
    }

    /**
     * Test Case: IT-CT-003
     * Title: Return signature status with signed rows and the current-user flag.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getSignatureStatus (GET /api/contract/{contractId}/signatures).
     * Input: contractId=900.
     * Steps:
     *   1. Prepare the fixture: Contract 900 has two signed signatures and a linked tutor profile.
     *   2. Use the input: contractId=900.
     *   3. Execute ContractServiceImpl.getSignatureStatus (GET /api/contract/{contractId}/signatures). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_003_GetSignatureStatusReturnsOnlySignedRowsWithCurrentUserFlag.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert fullySigned/count/row count and current-user marker.
     * Expected: A fully signed contract returns two signed rows, signedCount=2 and correctly identifies the current user.
     * Pre-conditions: Contract 900 has two signed signatures and a linked tutor profile.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-003: Return signature status with signed rows and the current-user flag.")
    void IT_CT_003_GetSignatureStatusReturnsOnlySignedRowsWithCurrentUserFlag() {
        ContractSignature centerSignature = signedCenterSignature();
        ContractSignature signedTutor = signedTutorSignature(tutorUser());

        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(signedTutor, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(2);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID))
                .thenReturn(Optional.of(contract.getRecruitmentApplication().getTutor()));

        var response = contractService.getSignatureStatus(CONTRACT_ID);

        assertEquals(true, response.isFullySigned());
        assertEquals(2, response.getSignedCount());
        assertEquals(2, response.getSignatures().size());
        assertEquals(true, response.getSignatures().stream()
                .anyMatch(signature -> signature.isCurrentUser()
                        && "Gia sư IT".equals(signature.getSignerName())));
    }

    /**
     * Test Case: IT-CT-004
     * Title: Reject contract signing when the OTP is blank.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; blank OTP.
     * Steps:
     *   1. Prepare the fixture: A pending signer slot exists.
     *   2. Use the input: contractId=900; blank OTP.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_004_RejectBlankOtpBeforeChangingSignatureState.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert validation error and verify signature save is never called.
     * Expected: The service returns “Mã OTP là bắt buộc” and does not save a signature.
     * Pre-conditions: A pending signer slot exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-004: Reject contract signing when the OTP is blank.")
    void IT_CT_004_RejectBlankOtpBeforeChangingSignatureState() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("")));

        assertEquals("Mã OTP là bắt buộc", exception.getMessage());
        verify(contractSignatureRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CT-005
     * Title: Reject contract signing when the signer’s CCCD is incomplete.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Tutor is the current signer but CccdService.isComplete returns false.
     *   2. Use the input: contractId=900; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_005_RejectSigningWhenSignerCccdIsIncomplete.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify signature save is skipped.
     * Expected: The service returns a CCCD-related error and does not change the signature row.
     * Pre-conditions: Tutor is the current signer but CccdService.isComplete returns false.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-005: Reject contract signing when the signer’s CCCD is incomplete.")
    void IT_CT_005_RejectSigningWhenSignerCccdIsIncomplete() {
        when(cccdService.isComplete(TUTOR_USER_ID)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("123456")));

        assertTrue(exception.getMessage().contains("CCCD"));
        verify(contractSignatureRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CT-006
     * Title: Block anonymous contract detail access.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}).
     * Input: contractId=900.
     * Steps:
     *   1. Prepare the fixture: No authenticated principal.
     *   2. Use the input: contractId=900.
     *   3. Execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_006_BlockAnonymousContractReadBeforeReturningDetail.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and no detail is returned.
     * Expected: The service returns “Yêu cầu đăng nhập” before exposing contract data.
     * Pre-conditions: No authenticated principal.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-006: Block anonymous contract detail access.")
    void IT_CT_006_BlockAnonymousContractReadBeforeReturningDetail() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContract(CONTRACT_ID));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
    }

    /**
     * Test Case: IT-CT-007
     * Title: Block a non-signer from reading contract detail.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}).
     * Input: contractId=900.
     * Steps:
     *   1. Prepare the fixture: Contract 900 has signer rows for other users; current user is not a signer.
     *   2. Use the input: contractId=900.
     *   3. Execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_007_BlockNonSignerFromReadingContractDetail.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and no unauthorized signature data.
     * Expected: The service returns “Bạn không có quyền xem hợp đồng này”.
     * Pre-conditions: Contract 900 has signer rows for other users; current user is not a signer.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-007: Block a non-signer from reading contract detail.")
    void IT_CT_007_BlockNonSignerFromReadingContractDetail() {
        User stranger = new User();
        stranger.setUserId(STRANGER_USER_ID);
        stranger.setEmail("stranger.it@tcs.test");

        when(authHelper.currentUserId()).thenReturn(STRANGER_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(stranger, UserRole.CLIENT));
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, signedCenterSignature()));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContract(CONTRACT_ID));

        assertEquals("Bạn không có quyền xem hợp đồng này", exception.getMessage());
    }

    /**
     * Test Case: IT-CT-008
     * Title: Prevent one user from signing on behalf of another account.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.sign (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; OTP 123456; signerUserId=999.
     * Steps:
     *   1. Prepare the fixture: The caller is authenticated as a different user from the requested signer.
     *   2. Use the input: contractId=900; OTP 123456; signerUserId=999.
     *   3. Execute ContractServiceImpl.sign (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_008_PreventUserFromSigningForAnotherAccount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify signature save is never called.
     * Expected: The service returns “Không thể ký thay người dùng khác” and does not save a signature.
     * Pre-conditions: The caller is authenticated as a different user from the requested signer.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-008: Prevent one user from signing on behalf of another account.")
    void IT_CT_008_PreventUserFromSigningForAnotherAccount() {
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.sign(CONTRACT_ID, "123456", STRANGER_USER_ID));

        assertEquals("Không thể ký thay người dùng khác", exception.getMessage());
        verify(contractSignatureRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CT-009
     * Title: Reject OTP signing for a contract that is already signed.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Contract 900 has status SIGNED.
     *   2. Use the input: contractId=900; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_009_RejectOtpSigningForAlreadySignedContract.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert illegal-state message.
     * Expected: The service returns “Hợp đồng không ở trạng thái chờ ký” and does not alter the contract.
     * Pre-conditions: Contract 900 has status SIGNED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-009: Reject OTP signing for a contract that is already signed.")
    void IT_CT_009_RejectOtpSigningForAlreadySignedContract() {
        contract.setStatus(ContractStatus.SIGNED);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("123456")));

        assertEquals("Hợp đồng không ở trạng thái chờ ký", exception.getMessage());
    }

    /**
     * Test Case: IT-CT-010
     * Title: Update tutorSignedAt when the tutor signs a private assignment contract.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=901; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: A pending tutor signature exists on private contract 901 and tutor CCCD is complete.
     *   2. Use the input: contractId=901; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_010_OtpSignatureUpdatesTutorSignedAtOnPrivateAssignment.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert tutorSignedAt and verify ClassAssignment.save.
     * Expected: The private assignment’s tutorSignedAt is populated and the assignment is saved.
     * Pre-conditions: A pending tutor signature exists on private contract 901 and tutor CCCD is complete.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-010: Update tutorSignedAt when the tutor signs a private assignment contract.")
    void IT_CT_010_OtpSignatureUpdatesTutorSignedAtOnPrivateAssignment() {
        Contract privateContract = privateAssignmentContract();
        ContractSignature privateTutorSignature = new ContractSignature();
        privateTutorSignature.setSignatureId(8L);
        privateTutorSignature.setContract(privateContract);
        privateTutorSignature.setPartyRole(PartyRole.TUTOR);
        privateTutorSignature.setSignatureStatus(ContractSignatureStatus.PENDING);
        privateTutorSignature.setEmail(TUTOR_EMAIL);

        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractSignatureRepository.findByContractIdAndPartyRole(901L, PartyRole.TUTOR))
                .thenReturn(Optional.of(privateTutorSignature));
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser()));
        when(contractSignatureRepository.findByContractId(901L)).thenReturn(List.of(privateTutorSignature));
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(1);

        contractService.signWithOtp(901L, otp("123456"));

        assertNotNull(privateContract.getAssignment().getTutorSignedAt());
        verify(classAssignmentRepository).save(privateContract.getAssignment());
    }

    /**
     * Test Case: IT-CT-011
     * Title: Send a contract OTP to the signer and keep the signature slot pending.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.sendOtp (POST /api/contract/{contractId}/send-otp).
     * Input: contractId=900.
     * Steps:
     *   1. Prepare the fixture: Contract 900 has a pending tutor signature with a valid email.
     *   2. Use the input: contractId=900.
     *   3. Execute ContractServiceImpl.sendOtp (POST /api/contract/{contractId}/send-otp). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_011_SendContractOtpEmailsSignerAndStoresPendingSignatureSlot.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response fields and verify EmailOtp, email and signature saves.
     * Expected: The response contains masked email tu***@tcs.test and five-minute expiry; OTP and pending signature are saved and email is sent.
     * Pre-conditions: Contract 900 has a pending tutor signature with a valid email.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-011: Send a contract OTP to the signer and keep the signature slot pending.")
    void IT_CT_011_SendContractOtpEmailsSignerAndStoresPendingSignatureSlot() {
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));

        var response = contractService.sendOtp(CONTRACT_ID);

        assertEquals("Mã OTP đã được gửi đến email của bạn", response.get("message"));
        assertEquals("tu***@tcs.test", response.get("maskedEmail"));
        assertEquals(5, response.get("expiresInMinutes"));
        assertEquals(TUTOR_EMAIL, tutorSignature.getEmail());
        verify(emailOtpRepository, org.mockito.Mockito.atLeastOnce()).save(any(EmailOtp.class));
        verify(emailService).sendContractOtp(eq(TUTOR_EMAIL), anyString(), eq(contract.getContractNo()));
        verify(contractSignatureRepository).save(tutorSignature);
    }

    /**
     * Test Case: IT-CT-012
     * Title: Keep a contract signature pending after sending an OTP.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.sendOtp (POST /api/contract/{contractId}/send-otp).
     * Input: contractId=900.
     * Steps:
     *   1. Prepare the fixture: Tutor signature slot is PENDING.
     *   2. Use the input: contractId=900.
     *   3. Execute ContractServiceImpl.sendOtp (POST /api/contract/{contractId}/send-otp). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_012_SentOtpKeepsSignaturePendingUntilUserSubmitsCode.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert pending status and verify ContractRepository.save is never called.
     * Expected: Sending the code does not sign the contract; the slot remains PENDING and the contract is not saved as signed.
     * Pre-conditions: Tutor signature slot is PENDING.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-012: Keep a contract signature pending after sending an OTP.")
    void IT_CT_012_SentOtpKeepsSignaturePendingUntilUserSubmitsCode() {
        when(contractSignatureRepository.findByContractIdAndPartyRole(CONTRACT_ID, PartyRole.TUTOR))
                .thenReturn(Optional.of(tutorSignature));

        contractService.sendOtp(CONTRACT_ID);

        assertEquals(ContractSignatureStatus.PENDING, tutorSignature.getSignatureStatus());
        assertEquals(TUTOR_EMAIL, tutorSignature.getEmail());
        verify(contractSignatureRepository).save(tutorSignature);
        verify(emailOtpRepository, org.mockito.Mockito.atLeastOnce()).save(any(EmailOtp.class));
        verify(contractRepository, never()).save(contract);
    }

    /**
     * Test Case: IT-CT-013
     * Title: Publish the student-contract-signed event when all required signers complete the contract.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=902; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Center student contract 902 has two required signatures and the current client has complete CCCD.
     *   2. Use the input: contractId=902; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_013_FullySignedStudentContractPublishesStudentSignedEvent.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert contract status and verify event publisher.
     * Expected: The student contract becomes SIGNED and a StudentContractSigned event is published.
     * Pre-conditions: Center student contract 902 has two required signatures and the current client has complete CCCD.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-013: Publish the student-contract-signed event when all required signers complete the contract.")
    void IT_CT_013_FullySignedStudentContractPublishesStudentSignedEvent() {
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

        contractService.signWithOtp(902L, otp("123456"));

        assertEquals(ContractStatus.SIGNED, studentContract.getStatus());
        verify(eventPublisher).publishEvent(any(StudentContractSigned.class));
    }

    /**
     * Test Case: IT-CT-014
     * Title: Keep student contract tuition stable when the final signer submits OTP.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=902; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Center student contract 902 is awaiting the client signature and has fixed terms.
     *   2. Use the input: contractId=902; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_014_StudentContractAmountRemainsStableAfterOtpSignature.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response terms and captured escrow amount.
     * Expected: The response keeps tuition 120000 and five sessions, and prepares an escrow command for 600000.
     * Pre-conditions: Center student contract 902 is awaiting the client signature and has fixed terms.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-014: Keep student contract tuition stable when the final signer submits OTP.")
    void IT_CT_014_StudentContractAmountRemainsStableAfterOtpSignature() {
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

        ContractResponse response = contractService.signWithOtp(902L, otp("123456"));

        assertEquals(new BigDecimal("120000.00"), response.getTuitionFee());
        assertEquals(5, response.getNumberOfSessions());
        ArgumentCaptor<EscrowLockCommand> commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(new BigDecimal("600000.00"), commandCaptor.getValue().amount());
    }

    /**
     * Test Case: IT-CT-015
     * Title: Store the signer timestamp and consume the OTP after successful signing.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Tutor signature and matching active OTP exist for contract 900.
     *   2. Use the input: contractId=900; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_015_ContractOtpSigningStoresSignerTimestampAndConsumesOtp.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert signature fields, consumedAt and both saves.
     * Expected: The signature becomes SIGNED with signer/time/data and the active OTP receives consumedAt.
     * Pre-conditions: Tutor signature and matching active OTP exist for contract 900.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-015: Store the signer timestamp and consume the OTP after successful signing.")
    void IT_CT_015_ContractOtpSigningStoresSignerTimestampAndConsumesOtp() {
        User tutorUser = tutorUser();

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, signedCenterSignature()));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(2);

        contractService.signWithOtp(CONTRACT_ID, otp("123456"));

        assertEquals(ContractSignatureStatus.SIGNED, tutorSignature.getSignatureStatus());
        assertEquals(tutorUser, tutorSignature.getSigner());
        assertNotNull(tutorSignature.getSignedAt());
        assertTrue(tutorSignature.getSignatureData().startsWith("OTP_VERIFIED:" + tutorUser.getEmail()));
        assertNotNull(activeOtp.getConsumedAt());
        verify(contractSignatureRepository).save(tutorSignature);
        verify(emailOtpRepository).save(activeOtp);
    }

    /**
     * Test Case: IT-CT-016
     * Title: Prepare the center-student escrow payment when the contract is fully signed.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=902; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Center student contract 902 is fully signed after the client’s valid OTP.
     *   2. Use the input: contractId=902; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_016_FullySignedStudentContractPreparesCenterEscrowPayment.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture and assert all EscrowLockCommand selectors/amount.
     * Expected: The escrow command uses payer 300, classStudent 88 and amount 600000.
     * Pre-conditions: Center student contract 902 is fully signed after the client’s valid OTP.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-016: Prepare the center-student escrow payment when the contract is fully signed.")
    void IT_CT_016_FullySignedStudentContractPreparesCenterEscrowPayment() {
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

        contractService.signWithOtp(902L, otp("123456"));

        ArgumentCaptor<EscrowLockCommand> commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(300L, commandCaptor.getValue().payerUserId());
        assertEquals(88L, commandCaptor.getValue().classStudentId());
        assertEquals(new BigDecimal("600000.00"), commandCaptor.getValue().amount());
    }

    /**
     * Test Case: IT-CT-017
     * Title: Terminate a cooperation contract when the tutor declines it and withdraw the application.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.declineCooperationContract (POST /api/contract/{contractId}/decline).
     * Input: contractId=900.
     * Steps:
     *   1. Prepare the fixture: Tutor owns a pending cooperation contract and recruitment application.
     *   2. Use the input: contractId=900.
     *   3. Execute ContractServiceImpl.declineCooperationContract (POST /api/contract/{contractId}/decline). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_017_TutorDeclinesPendingCooperationContractAndApplicationIsWithdrawn.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert both statuses and verify both saves.
     * Expected: The contract becomes TERMINATED and the linked recruitment application becomes WITHDRAWN.
     * Pre-conditions: Tutor owns a pending cooperation contract and recruitment application.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-017: Terminate a cooperation contract when the tutor declines it and withdraw the application.")
    void IT_CT_017_TutorDeclinesPendingCooperationContractAndApplicationIsWithdrawn() {
        RecruitmentApplication application = contract.getRecruitmentApplication();

        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));
        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);

        contractService.declineCooperationContract(CONTRACT_ID);

        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        assertEquals(com.tcs.module.center.enums.RecruitmentApplicationStatus.WITHDRAWN, application.getStatus());
        verify(contractRepository).save(contract);
        verify(recruitmentApplicationRepository).save(application);
    }

    /**
     * Test Case: IT-CT-018
     * Title: Mark the signature slot EXPIRED when the contract OTP has expired.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: The current OTP expired one minute ago.
     *   2. Use the input: contractId=900; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_018_MarkSignatureExpiredWhenContractOtpExpires.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert message/status and verify signature save.
     * Expected: The service returns the OTP-expired message and saves the signature slot as EXPIRED.
     * Pre-conditions: The current OTP expired one minute ago.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-018: Mark the signature slot EXPIRED when the contract OTP has expired.")
    void IT_CT_018_MarkSignatureExpiredWhenContractOtpExpires() {
        activeOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> contractService.signWithOtp(CONTRACT_ID, otp("123456")));

        assertEquals("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.", exception.getMessage());
        assertEquals(ContractSignatureStatus.EXPIRED, tutorSignature.getSignatureStatus());
        verify(contractSignatureRepository).save(tutorSignature);
    }

    /**
     * Test Case: IT-CT-019
     * Title: Attach contract reference context to the client-signature notification for the tutor.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign).
     * Input: assignmentId=800; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Client signs a pending private assignment with complete CCCD.
     *   2. Use the input: assignmentId=800; OTP 123456.
     *   3. Execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CT_019_ClientSignatureNotificationUsesContractReferenceForTutorContractPage.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification recipient, template, reference type and id.
     * Expected: The tutor notification uses CONTRACT reference and class id 77 so the tutor can open the contract screen.
     * Pre-conditions: Client signs a pending private assignment with complete CCCD.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-019: Attach contract reference context to the client-signature notification for the tutor.")
    void IT_CT_019_ClientSignatureNotificationUsesContractReferenceForTutorContractPage() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(cccdService.getByUserId(CLIENT_USER_ID)).thenReturn(completeCccd("Nguyễn Thu Hà", "001200000001"));

        marketplaceService.signAssignmentContract(ASSIGNMENT_ID, "123456");

        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(tutorUser),
                eq(NotificationType.APPLICATION),
                eq("MARKETPLACE_CONTRACT_TUTOR_SIGN"),
                any(),
                eq("Bên A đã ký hợp đồng — mời bạn ký"),
                anyString(),
                eq("CONTRACT"),
                eq(CLASS_ID));
    }

    /**
     * Test Case: IT-CT-020
     * Title: Mark a fully signed cooperation contract and publish its completion event.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign).
     * Input: contractId=900; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Both cooperation-contract signatures are complete and the current tutor has a valid OTP.
     *   2. Use the input: contractId=900; OTP 123456.
     *   3. Execute ContractServiceImpl.signWithOtp (POST /api/contract/{contractId}/sign). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_CT_020_FullySignedCooperationContractMarksContractSignedAndPublishesEvent.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert statuses/timestamps and verify event publication.
     * Expected: The cooperation contract becomes SIGNED with signedAt/confirmedAt and publishes CooperationContractSigned.
     * Pre-conditions: Both cooperation-contract signatures are complete and the current tutor has a valid OTP.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CT-020: Mark a fully signed cooperation contract and publish its completion event.")
    void IT_CT_020_FullySignedCooperationContractMarksContractSignedAndPublishesEvent() {
        User tutorUser = tutorUser();
        ContractSignature centerSignature = signedCenterSignature();

        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(contractSignatureRepository.findByContractId(CONTRACT_ID))
                .thenReturn(List.of(tutorSignature, centerSignature));
        when(contractSignatureRepository.countSignedByContractId(CONTRACT_ID)).thenReturn(2);

        contractService.signWithOtp(CONTRACT_ID, otp("123456"));

        assertEquals(ContractStatus.SIGNED, contract.getStatus());
        assertNotNull(contract.getSignedAt());
        assertNotNull(contract.getConfirmedAt());
        verify(contractRepository).save(contract);
        verify(eventPublisher).publishEvent(any(CooperationContractSigned.class));
    }



    private SignWithOtpRequest otp(String code) {
        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode(code);
        return request;
    }

    private User tutorUser() {
        return contract.getRecruitmentApplication().getTutor().getUser();
    }

    private ContractSignature signedCenterSignature() {
        User centerUser = contract.getRecruitmentApplication().getRecruitmentPost().getCenter().getUser();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(2L);
        signature.setContract(contract);
        signature.setPartyRole(PartyRole.CENTER);
        signature.setSigner(centerUser);
        signature.setEmail("center.it@tcs.test");
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now().minusHours(1));
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

    private ContractSignature signedTutorSignature(User tutorUser) {
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(3L);
        signature.setContract(contract);
        signature.setPartyRole(PartyRole.TUTOR);
        signature.setSigner(tutorUser);
        signature.setEmail(tutorUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now());
        return signature;
    }

    private ContractSignature signedClientSignature(Contract targetContract) {
        User clientUser = targetContract.getAssignment().getApplication().getTutoringClass().getCreator();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(21L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.CLIENT);
        signature.setSigner(clientUser);
        signature.setEmail(clientUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now().minusMinutes(5));
        return signature;
    }

    private ContractSignature pendingTutorSignature(Contract targetContract) {
        User tutorUser = targetContract.getAssignment().getTutor().getUser();
        ContractSignature signature = new ContractSignature();
        signature.setSignatureId(22L);
        signature.setContract(targetContract);
        signature.setPartyRole(PartyRole.TUTOR);
        signature.setEmail(tutorUser.getEmail());
        signature.setSignatureStatus(ContractSignatureStatus.PENDING);
        return signature;
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

    private void stubContractListForUser(Long userId, String email, Contract visibleContract) {
        when(contractRepository.findContractsByUserId(userId)).thenReturn(List.of(visibleContract));
        when(contractRepository.findBySignatureParty(userId, email)).thenReturn(List.of(visibleContract));
        when(contractRepository.findByAssignment_Tutor_UserId(userId)).thenReturn(List.of(visibleContract));
        when(contractRepository.findByAssignment_ClassCreator_UserId(userId)).thenReturn(List.of());
        when(contractRepository.findByClassStudent_UserId(userId)).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_Tutor_UserId(userId)).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_CenterUser_UserId(userId)).thenReturn(List.of());
        when(tutorRepository.findByUser_UserId(userId)).thenReturn(Optional.of(visibleContract.getAssignment().getTutor()));
        when(classAssignmentRepository.findByTutor_TutorIdOrderByAssignedDateDesc(20L)).thenReturn(List.of());
        when(classAssignmentRepository.findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(userId))
                .thenReturn(List.of());
    }

    private void preparePrivateTuitionData(Contract privateContract) {
        ClassAssignment assignment = privateContract.getAssignment();
        TutoringClass tutoringClass = assignment.getApplication().getTutoringClass();
        tutoringClass.setTitle("Lớp private Toán 12");
        tutoringClass.setNumberOfSessions(4);
        tutoringClass.setTuitionFee(new BigDecimal("100000.00"));
        tutoringClass.setDetailsJson("""
                {"subjectFees":{"1":100000},"slots":[{"subjectId":"1","start":"18:00","end":"19:00"}],
                 "billingCycle":"MONTH","months":1,"durationUnit":"MONTH","scheduleMode":"WEEKLY"}
                """);
        assignment.getApplication().setProposedRatesJson("{\"1\":100000}");
    }

    private PaymentTransaction pendingEscrowPayment(String referenceCode, BigDecimal amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(710L);
        transaction.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setDescription("Chờ thanh toán ký quỹ hợp đồng private");
        transaction.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        return transaction;
    }

    private Contract privateAssignmentContract() {
        User clientUser = new User();
        clientUser.setUserId(300L);
        clientUser.setEmail("client.it@tcs.test");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(500L);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.PRIVATE);
        tutoringClass.setCreator(clientUser);

        TutorApplication application = new TutorApplication();
        application.setApplicationId(700L);
        application.setTutoringClass(tutoringClass);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(800L);
        assignment.setApplication(application);
        assignment.setTutor(contract.getRecruitmentApplication().getTutor());

        Contract privateContract = new Contract();
        privateContract.setContractId(901L);
        privateContract.setContractNo("HD-PRIVATE-IT");
        privateContract.setStatus(ContractStatus.PENDING);
        privateContract.setAssignment(assignment);
        privateContract.setSourceType(com.tcs.module.contract.enums.ContractSourceType.PRIVATE);
        return privateContract;
    }


    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.test");
        return user;
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

    

    private ClassAssignment assignment(TutoringClass tutoringClass, User tutorUser) {
        Tutor tutor = tutor(tutorUser);
        TutorApplication application = tutorApplication(tutoringClass, tutor);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(ASSIGNMENT_ID);
        assignment.setTutor(tutor);
        assignment.setApplication(application);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        return assignment;
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
}
