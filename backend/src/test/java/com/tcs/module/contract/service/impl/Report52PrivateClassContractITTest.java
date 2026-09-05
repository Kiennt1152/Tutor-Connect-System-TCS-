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
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
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
import org.mockito.Spy;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52PrivateClassContractITTest {

    private static final Long CLASS_ID = 5L;
    private static final Long ASSIGNMENT_ID = 7L;
    private static final Long CLASS_STUDENT_ID = 8L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;
    private static final Long STRANGER_USER_ID = 999L;
    private static final Long CONTRACT_ID = 900L;
    private static final String TUTOR_EMAIL = "tutor.it@tcs.test";

    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private ContractTemplateRepository contractTemplateRepository;
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
    @Mock private EmailService emailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private ReviewRepository reviewRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private ReputationHistoryRepository reputationHistoryRepository;

    @Spy
    @InjectMocks
    private ContractServiceImpl contractService;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    private Contract contract;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contractService, "otpService", new OtpService(emailOtpRepository));
        ReflectionTestUtils.setField(marketplaceService, "contractEmailService", emailService);
        ReflectionTestUtils.setField(marketplaceService, "contractService", contractService);

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

        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));

        when(cccdService.getByUserId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(CccdInfoDto.builder()
                .fullName("Nguyễn Văn IT")
                .cccdNumber("012345678901")
                .dateOfBirth("01/01/1999")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build());
    }

    /**
     * Test Case: IT-PRV-001
     * Title: Build the assignment contract view with the client role, tuition and escrow QR payment gate.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.getAssignmentContract (GET /api/marketplace/assignments/{assignmentId}/contract).
     * Input: assignmentId=800.
     * Steps:
     *   1. Prepare the fixture: Private assignment is signed by both parties and has a pending escrow payment.
     *   2. Use the input: assignmentId=800.
     *   3. Execute MarketplaceServiceImpl.getAssignmentContract (GET /api/marketplace/assignments/{assignmentId}/contract). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_001_GetAssignmentContractBuildsClientPaymentGateFromContractScreen.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role, amounts, transfer content and QR URL.
     * Expected: The client sees assignment 800, role CLIENT, escrow 500000, reference ESCROW-A800 and a QR URL containing amount=500000.
     * Pre-conditions: Private assignment is signed by both parties and has a pending escrow payment.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-001: Build the assignment contract view with the client role, tuition and escrow QR payment gate.")
    void IT_PRV_001_GetAssignmentContractBuildsClientPaymentGateFromContractScreen() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        tutoringClass.setNumberOfSessions(5);
        tutoringClass.setTuitionFee(new BigDecimal("100000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(10));
        assignment.setTutorSignedAt(LocalDateTime.now().minusMinutes(5));
        assignment.setPaymentMethod("FULL");
        Contract contract = privateContract(assignment);
        PaymentTransaction pendingPayment = privateEscrowPayment(701L, "ESCROW-A" + ASSIGNMENT_ID);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(contract));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID)).thenReturn(Optional.of(client(clientUser)));
        when(cccdService.getByUserId(CLIENT_USER_ID)).thenReturn(completeCccd("Nguyễn Thu Hà", "001200000001"));
        when(cccdService.getByUserId(TUTOR_USER_ID)).thenReturn(completeCccd("Lê Hoàng Nam", "001200000002"));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByReferenceCode("ESCROW-A" + ASSIGNMENT_ID))
                .thenReturn(Optional.of(pendingPayment));

        var response = marketplaceService.getAssignmentContract(ASSIGNMENT_ID);

        assertEquals(ASSIGNMENT_ID, response.getAssignmentId());
        assertEquals("CLIENT", response.getMyRole());
        assertEquals(new BigDecimal("500000.00"), response.getEscrowAmount());
        assertEquals("ESCROW-A" + ASSIGNMENT_ID, response.getEscrowPayment().getTransferContent());
        assertTrue(response.getEscrowPayment().getQrUrl().contains("amount=500000"));
    }

    /**
     * Test Case: IT-PRV-002
     * Title: List each private contract once for a signer.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContracts (GET /api/contract/my).
     * Input: Authenticated tutor session.
     * Steps:
     *   1. Prepare the fixture: The current tutor is a signer of private contract 901; duplicate repository sources may contain the same assignment.
     *   2. Use the input: Authenticated tutor session.
     *   3. Execute ContractServiceImpl.getMyContracts (GET /api/contract/my). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_PRV_002_GetMyContractsReturnsDeduplicatedPrivateContractRowsForSigner.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert one deduplicated response and its signer/assignment fields.
     * Expected: The signed private contract is returned once with assignment 800, tutor id 22 and class type PRIVATE.
     * Pre-conditions: The current tutor is a signer of private contract 901; duplicate repository sources may contain the same assignment.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-002: List each private contract once for a signer.")
    void IT_PRV_002_GetMyContractsReturnsDeduplicatedPrivateContractRowsForSigner() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        privateContract.setStatus(ContractStatus.SIGNED);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser(), UserRole.TUTOR));
        stubContractListForUser(TUTOR_USER_ID, TUTOR_EMAIL, privateContract);
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(2);

        var responses = contractService.getMyContracts();

        assertEquals(1, responses.size());
        assertEquals(901L, responses.get(0).getContractId());
        assertEquals(800L, responses.get(0).getAssignmentId());
        assertEquals(TUTOR_USER_ID, responses.get(0).getTutorId());
        assertEquals("PRIVATE", responses.get(0).getClassType());
    }

    /**
     * Test Case: IT-PRV-003
     * Title: Load private contract detail with parties and tuition.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}).
     * Input: contractId=901.
     * Steps:
     *   1. Prepare the fixture: Current client is an authorized signer of contract 901.
     *   2. Use the input: contractId=901.
     *   3. Execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_PRV_003_GetPrivateContractDetailReturnsClassPartiesAndTuition.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Compare contract number, class, client/tutor emails and amount.
     * Expected: Contract 901 returns contract number HD-PRIVATE-IT, class title Lớp private Toán 12, both email addresses and escrow 400000.
     * Pre-conditions: Current client is an authorized signer of contract 901.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-003: Load private contract detail with parties and tuition.")
    void IT_PRV_003_GetPrivateContractDetailReturnsClassPartiesAndTuition() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        User clientUser = privateContract.getAssignment().getApplication().getTutoringClass().getCreator();

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractRepository.findContractsByUserId(clientUser.getUserId())).thenReturn(List.of());
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(1);

        var response = contractService.getMyContract(901L);

        assertEquals("HD-PRIVATE-IT", response.getContractNo());
        assertEquals("Lớp private Toán 12", response.getClassTitle());
        assertEquals("client.it@tcs.test", response.getClientEmail());
        assertEquals(TUTOR_EMAIL, response.getTutorEmail());
        assertEquals(new BigDecimal("400000.00"), response.getEscrowAmount());
    }

    /**
     * Test Case: IT-PRV-004
     * Title: Reject incomplete refund-payout information saved from a private contract.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.saveAssignmentRefundPayoutInfo (POST /api/marketplace/assignments/{assignmentId}/refund-payout).
     * Input: TPBank; blank account number; holder Nguyễn Thu Hà.
     * Steps:
     *   1. Prepare the fixture: Client can access assignment 800.
     *   2. Use the input: TPBank; blank account number; holder Nguyễn Thu Hà.
     *   3. Execute MarketplaceServiceImpl.saveAssignmentRefundPayoutInfo (POST /api/marketplace/assignments/{assignmentId}/refund-payout). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_004_RejectSavingRefundPayoutWhenRequiredBankFieldsAreMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the Vietnamese validation message and verify assignment save is never called.
     * Expected: The request is rejected when the bank account number is blank and the assignment is not changed.
     * Pre-conditions: Client can access assignment 800.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-004: Reject incomplete refund-payout information saved from a private contract.")
    void IT_PRV_004_RejectSavingRefundPayoutWhenRequiredBankFieldsAreMissing() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        ClassAssignment assignment = pendingSignedAssignment(tutoringClass, user(TUTOR_USER_ID));
        SaveRefundPayoutRequest request = new SaveRefundPayoutRequest();
        request.setBankName("TPBank");
        request.setAccountNo("");
        request.setAccountHolderName("Nguyễn Thu Hà");

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.saveAssignmentRefundPayoutInfo(ASSIGNMENT_ID, request));

        assertEquals("Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản", exception.getMessage());
        verify(classAssignmentRepository, never()).save(any());
    }

    /**
     * Test Case: IT-PRV-005
     * Title: Prevent a tutor from accepting an assignment before contract signing and escrow funding.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.acceptAssignment (POST /api/marketplace/assignments/{assignmentId}/accept).
     * Input: assignmentId=800.
     * Steps:
     *   1. Prepare the fixture: Tutor owns a PENDING assignment with no signed contract/escrow.
     *   2. Use the input: assignmentId=800.
     *   3. Execute MarketplaceServiceImpl.acceptAssignment (POST /api/marketplace/assignments/{assignmentId}/accept). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_005_RejectAcceptAssignmentBeforeContractAndEscrowAreReady.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify class save is never called.
     * Expected: The service returns the contract/escrow prerequisite message and does not activate the class.
     * Pre-conditions: Tutor owns a PENDING assignment with no signed contract/escrow.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-005: Prevent a tutor from accepting an assignment before contract signing and escrow funding.")
    void IT_PRV_005_RejectAcceptAssignmentBeforeContractAndEscrowAreReady() {
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(user(CLIENT_USER_ID), TutoringClassStatus.MATCHED);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.acceptAssignment(ASSIGNMENT_ID));

        assertEquals("Vui lòng ký hợp đồng và thanh toán escrow trước khi nhận lớp", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    /**
     * Test Case: IT-PRV-006
     * Title: Block an anonymous user from listing private contracts.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContracts (GET /api/contract/my).
     * Input: No access token.
     * Steps:
     *   1. Prepare the fixture: No authenticated principal.
     *   2. Use the input: No access token.
     *   3. Execute ContractServiceImpl.getMyContracts (GET /api/contract/my). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_PRV_006_BlockAnonymousContractListBeforeReturningPrivateContracts.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify contract list query is never called.
     * Expected: The service returns “Yêu cầu đăng nhập” before querying contract rows.
     * Pre-conditions: No authenticated principal.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-006: Block an anonymous user from listing private contracts.")
    void IT_PRV_006_BlockAnonymousContractListBeforeReturningPrivateContracts() {
        when(authHelper.requireAuthenticated()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContracts());

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(contractRepository, never()).findContractsByUserId(anyLong());
    }

    /**
     * Test Case: IT-PRV-007
     * Title: Block an unrelated user from opening private contract detail.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}).
     * Input: contractId=901.
     * Steps:
     *   1. Prepare the fixture: Contract 901 belongs to other parties; current user is not a signer.
     *   2. Use the input: contractId=901.
     *   3. Execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_PRV_007_BlockUnrelatedUserFromPrivateContractDetail.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify signature count lookup is skipped.
     * Expected: The service returns “Bạn không có quyền xem hợp đồng này” and does not load signature details.
     * Pre-conditions: Contract 901 belongs to other parties; current user is not a signer.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-007: Block an unrelated user from opening private contract detail.")
    void IT_PRV_007_BlockUnrelatedUserFromPrivateContractDetail() {
        Contract privateContract = privateAssignmentContract();

        when(authHelper.currentUserId()).thenReturn(STRANGER_USER_ID);
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContract(901L));

        assertEquals("Bạn không có quyền xem hợp đồng này", exception.getMessage());
        verify(contractSignatureRepository, never()).countSignedByContractId(901L);
    }

    /**
     * Test Case: IT-PRV-008
     * Title: Prevent an unrelated user from reading the assignment contract.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.getAssignmentContract (GET /api/marketplace/assignments/{assignmentId}/contract).
     * Input: assignmentId=800.
     * Steps:
     *   1. Prepare the fixture: Assignment 800 belongs to a different client/tutor pair.
     *   2. Use the input: assignmentId=800.
     *   3. Execute MarketplaceServiceImpl.getAssignmentContract (GET /api/marketplace/assignments/{assignmentId}/contract). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_008_PreventUnrelatedUserReadingPrivateAssignmentContract.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and no contract data is returned.
     * Expected: The service returns “Bạn không thuộc hợp đồng này”.
     * Pre-conditions: Assignment 800 belongs to a different client/tutor pair.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-008: Prevent an unrelated user from reading the assignment contract.")
    void IT_PRV_008_PreventUnrelatedUserReadingPrivateAssignmentContract() {
        User stranger = user(909L);
        TutoringClass tutoringClass = tutoringClass(user(CLIENT_USER_ID), TutoringClassStatus.MATCHED);
        ClassAssignment assignment = assignment(tutoringClass, user(TUTOR_USER_ID));

        when(authHelper.currentUserId()).thenReturn(stranger.getUserId());
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.getAssignmentContract(ASSIGNMENT_ID));

        assertEquals("Bạn không thuộc hợp đồng này", exception.getMessage());
    }

    /**
     * Test Case: IT-PRV-009
     * Title: Do not create a second escrow payment when one already exists after tutor signing.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign).
     * Input: assignmentId=800; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Client already signed; a funded/existing escrow is attached to assignment 800.
     *   2. Use the input: assignmentId=800; OTP 123456.
     *   3. Execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_009_DoNotCreateDuplicateEscrowPaymentWhenEscrowAlreadyExistsAfterTutorSigns.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert final payment method and verify no new escrow command.
     * Expected: The tutor signature succeeds, payment method remains FULL and EscrowService.preparePayment is not called again.
     * Pre-conditions: Client already signed; a funded/existing escrow is attached to assignment 800.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-009: Do not create a second escrow payment when one already exists after tutor signing.")
    void IT_PRV_009_DoNotCreateDuplicateEscrowPaymentWhenEscrowAlreadyExistsAfterTutorSigns() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        tutoringClass.setNumberOfSessions(5);
        tutoringClass.setTuitionFee(new BigDecimal("100000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(10));
        Contract contract = privateContract(assignment);
        EscrowTransaction escrow = escrow(95L, new BigDecimal("500000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(cccdService.getByUserId(TUTOR_USER_ID)).thenReturn(completeCccd("Lê Hoàng Nam", "001200000002"));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(escrow));

        marketplaceService.signAssignmentContract(ASSIGNMENT_ID, "123456");

        assertEquals("FULL", assignment.getPaymentMethod());
        verify(escrowService, never()).preparePayment(any(EscrowLockCommand.class));
    }

    /**
     * Test Case: IT-PRV-010
     * Title: Persist the client signature and notify the tutor that the contract needs action.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign).
     * Input: assignmentId=800; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Client is a contract party and has complete CCCD data.
     *   2. Use the input: assignmentId=800; OTP 123456.
     *   3. Execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_010_ClientSignaturePersistsPrivateContractStateAndNotifiesTutor.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert clientSignedAt and notification recipient/template/text/reference.
     * Expected: The clientSignedAt timestamp is set, the assignment is saved and the tutor receives MARKETPLACE_CONTRACT_TUTOR_SIGN.
     * Pre-conditions: Client is a contract party and has complete CCCD data.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-010: Persist the client signature and notify the tutor that the contract needs action.")
    void IT_PRV_010_ClientSignaturePersistsPrivateContractStateAndNotifiesTutor() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(cccdService.getByUserId(CLIENT_USER_ID)).thenReturn(completeCccd("Nguyễn Thu Hà", "001200000001"));

        marketplaceService.signAssignmentContract(ASSIGNMENT_ID, "123456");

        assertTrue(assignment.getClientSignedAt() != null);
        verify(classAssignmentRepository).save(assignment);
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
     * Test Case: IT-PRV-011
     * Title: Send a contract-action notification to the tutor after the client signs.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign).
     * Input: assignmentId=800; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Client can sign the pending private assignment and has complete CCCD.
     *   2. Use the input: assignmentId=800; OTP 123456.
     *   3. Execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_011_ClientSignatureNotifiesTutorToOpenContractPage.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification reference type CONTRACT and class id.
     * Expected: The notification text is “Bên A đã ký hợp đồng — mời bạn ký” and points to the contract context.
     * Pre-conditions: Client can sign the pending private assignment and has complete CCCD.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-011: Send a contract-action notification to the tutor after the client signs.")
    void IT_PRV_011_ClientSignatureNotifiesTutorToOpenContractPage() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(cccdService.getByUserId(CLIENT_USER_ID)).thenReturn(completeCccd("Nguyễn Thu Hà", "001200000001"));

        marketplaceService.signAssignmentContract(ASSIGNMENT_ID, "123456");

        assertTrue(assignment.getClientSignedAt() != null);
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
     * Test Case: IT-PRV-012
     * Title: Show the existing pending escrow QR payment when the signed contract page is reloaded.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}).
     * Input: contractId=901.
     * Steps:
     *   1. Prepare the fixture: Private contract 901 is fully signed and its pending payment already exists.
     *   2. Use the input: contractId=901.
     *   3. Execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_PRV_012_ReloadSignedContractReturnsExistingPendingEscrowQrPayment.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert escrow payment is reused instead of creating another transaction.
     * Expected: The response keeps payment status PENDING, transfer content ESCROW-A800 and a QR URL containing amount=400000.
     * Pre-conditions: Private contract 901 is fully signed and its pending payment already exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-012: Show the existing pending escrow QR payment when the signed contract page is reloaded.")
    void IT_PRV_012_ReloadSignedContractReturnsExistingPendingEscrowQrPayment() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        User clientUser = privateContract.getAssignment().getApplication().getTutoringClass().getCreator();
        PaymentTransaction pendingPayment = pendingEscrowPayment("ESCROW-A800", new BigDecimal("400000.00"));

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractRepository.findContractsByUserId(clientUser.getUserId())).thenReturn(List.of());
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(2);
        when(paymentTransactionRepository.findByReferenceCode("ESCROW-A800"))
                .thenReturn(Optional.of(pendingPayment));

        var response = contractService.getMyContract(901L);

        assertNotNull(response.getEscrowPayment());
        assertEquals(PaymentTransactionStatus.PENDING, response.getEscrowPayment().getPaymentStatus());
        assertEquals("ESCROW-A800", response.getEscrowPayment().getTransferContent());
        assertTrue(response.getEscrowPayment().getQrUrl().contains("amount=400000"));
    }

    /**
     * Test Case: IT-PRV-013
     * Title: Create the client escrow payment command when the tutor signs after the client.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign).
     * Input: assignmentId=800; tutor OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Client signed assignment 800; no existing escrow; tutor CCCD is complete.
     *   2. Use the input: assignmentId=800; tutor OTP 123456.
     *   3. Execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_013_TutorSignatureAfterClientBuildsEscrowPaymentCommand.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture EscrowLockCommand and compare payer, assignment and amount.
     * Expected: The command uses client 11 as payer, assignment 800 and amount 500000.
     * Pre-conditions: Client signed assignment 800; no existing escrow; tutor CCCD is complete.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-013: Create the client escrow payment command when the tutor signs after the client.")
    void IT_PRV_013_TutorSignatureAfterClientBuildsEscrowPaymentCommand() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        tutoringClass.setNumberOfSessions(5);
        tutoringClass.setTuitionFee(new BigDecimal("100000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(10));
        Contract contract = privateContract(assignment);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(cccdService.getByUserId(TUTOR_USER_ID)).thenReturn(completeCccd("Lê Hoàng Nam", "001200000002"));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        marketplaceService.signAssignmentContract(ASSIGNMENT_ID, "123456");

        var commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(CLIENT_USER_ID, commandCaptor.getValue().payerUserId());
        assertEquals(new BigDecimal("500000.00"), commandCaptor.getValue().amount());
        assertEquals(ASSIGNMENT_ID, commandCaptor.getValue().assignmentId());
    }

    /**
     * Test Case: IT-PRV-014
     * Title: Expose total tuition and the first payment amount from private contract terms.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}).
     * Input: contractId=901.
     * Steps:
     *   1. Prepare the fixture: Client is an authorized signer and contract terms contain five sessions at 100000.
     *   2. Use the input: contractId=901.
     *   3. Execute ContractServiceImpl.getMyContract (GET /api/contract/{contractId}). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_PRV_014_PrivateContractShowsTotalAndFirstPaymentAmountFromTerms.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert totalTuitionAmount, escrowAmount and class title.
     * Expected: Contract detail reports total tuition and escrow amount 400000 for Lớp private Toán 12.
     * Pre-conditions: Client is an authorized signer and contract terms contain five sessions at 100000.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-014: Expose total tuition and the first payment amount from private contract terms.")
    void IT_PRV_014_PrivateContractShowsTotalAndFirstPaymentAmountFromTerms() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        User clientUser = privateContract.getAssignment().getApplication().getTutoringClass().getCreator();

        when(authHelper.currentUserId()).thenReturn(clientUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(clientUser, UserRole.CLIENT));
        when(contractRepository.findById(901L)).thenReturn(Optional.of(privateContract));
        when(contractSignatureRepository.findByContractId(901L)).thenReturn(List.of());

        ContractResponse response = contractService.getMyContract(901L);

        assertEquals(new BigDecimal("400000.00"), response.getTotalTuitionAmount());
        assertEquals(new BigDecimal("400000.00"), response.getEscrowAmount());
        assertEquals("Lớp private Toán 12", response.getClassTitle());
    }

    /**
     * Test Case: IT-PRV-015
     * Title: Show a client-signed private contract in the tutor’s contract list.
     * Procedure: Prepare the stated fixture and input, then execute ContractServiceImpl.getMyContracts (GET /api/contract/my).
     * Input: Authenticated tutor session.
     * Steps:
     *   1. Prepare the fixture: Client signedAt is set; tutor is the current signer; assignment and signature rows are linked.
     *   2. Use the input: Authenticated tutor session.
     *   3. Execute ContractServiceImpl.getMyContracts (GET /api/contract/my). Mapped test: com.tcs.module.contract.service.impl.Report52ContractOtpServiceITTest#IT_PRV_015_TutorContractListIncludesClientSignedPrivateContractAfterNotification.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert list count, contract id and signature counts.
     * Expected: The tutor list contains contract 901 with signedCount=1 and requiredSignatures=2.
     * Pre-conditions: Client signedAt is set; tutor is the current signer; assignment and signature rows are linked.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-015: Show a client-signed private contract in the tutor’s contract list.")
    void IT_PRV_015_TutorContractListIncludesClientSignedPrivateContractAfterNotification() {
        Contract privateContract = privateAssignmentContract();
        preparePrivateTuitionData(privateContract);
        ClassAssignment assignment = privateContract.getAssignment();
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(5));
        User tutorUser = assignment.getTutor().getUser();
        ContractSignature clientSignature = signedClientSignature(privateContract);
        ContractSignature pendingTutorSignature = pendingTutorSignature(privateContract);

        when(authHelper.currentUserId()).thenReturn(tutorUser.getUserId());
        when(authHelper.requireAuthenticated()).thenReturn(new UserPrincipal(tutorUser, UserRole.TUTOR));
        when(contractRepository.findContractsByUserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findBySignatureParty(tutorUser.getUserId(), tutorUser.getEmail())).thenReturn(List.of());
        when(contractRepository.findByAssignment_Tutor_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByAssignment_ClassCreator_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByClassStudent_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_Tutor_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(contractRepository.findByRecruitmentApplication_CenterUser_UserId(tutorUser.getUserId())).thenReturn(List.of());
        when(tutorRepository.findByUser_UserId(tutorUser.getUserId())).thenReturn(Optional.of(assignment.getTutor()));
        when(classAssignmentRepository.findByTutor_TutorIdOrderByAssignedDateDesc(20L)).thenReturn(List.of(assignment));
        when(classAssignmentRepository.findByApplication_TutoringClass_Creator_UserIdOrderByAssignedDateDesc(tutorUser.getUserId()))
                .thenReturn(List.of());
        when(contractRepository.findByAssignment_AssignmentId(assignment.getAssignmentId()))
                .thenReturn(Optional.of(privateContract));
        when(contractRepository.save(privateContract)).thenReturn(privateContract);
        when(contractSignatureRepository.findByContractId(901L))
                .thenReturn(List.of(clientSignature, pendingTutorSignature));
        when(contractSignatureRepository.findByContractIdAndPartyRole(901L, PartyRole.CLIENT))
                .thenReturn(Optional.of(clientSignature));
        when(contractSignatureRepository.findByContractIdAndPartyRole(901L, PartyRole.TUTOR))
                .thenReturn(Optional.of(pendingTutorSignature));
        when(contractSignatureRepository.countSignedByContractId(901L)).thenReturn(1);

        List<ContractResponse> responses = contractService.getMyContracts();

        assertEquals(1, responses.size());
        assertEquals(901L, responses.get(0).getContractId());
        assertEquals(1, responses.get(0).getSignedCount());
        assertEquals(2, responses.get(0).getRequiredSignatures());
    }

    /**
     * Test Case: IT-PRV-016
     * Title: Release a private escrow when the tutor confirms completion after the client has reviewed.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: All private lessons are completed, client review exists and escrow 91 is funded.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.confirmClassCompletion (POST /api/marketplace/classes/{classId}/complete). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_016_TutorCompletionReleasesPrivateEscrowWhenClientAlreadyReviewed.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert class status/message and verify escrow apply, fee release and class save.
     * Expected: The class becomes COMPLETED, the success message is returned and a release instruction is applied.
     * Pre-conditions: All private lessons are completed, client review exists and escrow 91 is funded.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-016: Release a private escrow when the tutor confirms completion after the client has reviewed.")
    void IT_PRV_016_TutorCompletionReleasesPrivateEscrowWhenClientAlreadyReviewed() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 2, 2);
        EscrowTransaction escrow = escrow(91L, new BigDecimal("100000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(lessons);
        doReturn(true).when(contractService).hasClientReviewedClass(CLASS_ID);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(escrow));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        String message = marketplaceService.confirmClassCompletion(CLASS_ID);

        assertEquals("Lớp đã hoàn thành. Học phí ký quỹ đã được giải ngân cho gia sư.", message);
        assertEquals(TutoringClassStatus.COMPLETED, tutoringClass.getStatus());
        verify(escrowService).apply(any(ReleaseInstruction.class));
        verify(centerRequestFeeService).releaseForFulfilledAssignment(eq(ASSIGNMENT_ID), anyString());
        verify(tutoringClassRepository).save(tutoringClass);
    }

    /**
     * Test Case: IT-PRV-017
     * Title: Activate a private assignment only after signatures and escrow funding are ready.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.acceptAssignment (POST /api/marketplace/assignments/{assignmentId}/accept).
     * Input: assignmentId=800.
     * Steps:
     *   1. Prepare the fixture: Tutor owns a signed PENDING assignment and escrow 95 is funded.
     *   2. Use the input: assignmentId=800.
     *   3. Execute MarketplaceServiceImpl.acceptAssignment (POST /api/marketplace/assignments/{assignmentId}/accept). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_017_AcceptAssignmentActivatesPrivateClassAfterSigningAndEscrowFunding.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert both statuses and verify assignment/class saves.
     * Expected: Assignment becomes ACTIVE and its private class becomes IN_PROGRESS.
     * Pre-conditions: Tutor owns a signed PENDING assignment and escrow 95 is funded.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-017: Activate a private assignment only after signatures and escrow funding are ready.")
    void IT_PRV_017_AcceptAssignmentActivatesPrivateClassAfterSigningAndEscrowFunding() {
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(user(CLIENT_USER_ID), TutoringClassStatus.MATCHED);
        ClassAssignment assignment = pendingSignedAssignment(tutoringClass, tutorUser);
        EscrowTransaction escrow = escrow(95L, new BigDecimal("500000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(escrow));

        marketplaceService.acceptAssignment(ASSIGNMENT_ID);

        assertEquals(ClassAssignmentStatus.ACTIVE, assignment.getStatus());
        assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
        verify(classAssignmentRepository).save(assignment);
        verify(tutoringClassRepository).save(tutoringClass);
    }

    /**
     * Test Case: IT-PRV-018
     * Title: Calculate the first escrow amount for a private contract longer than one month.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign).
     * Input: assignmentId=800; OTP 123456; start 2026-08-15; end 2026-10-14.
     * Steps:
     *   1. Prepare the fixture: Tutor signs a long private assignment after the client; no escrow exists and tutor CCCD is complete.
     *   2. Use the input: assignmentId=800; OTP 123456; start 2026-08-15; end 2026-10-14.
     *   3. Execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_018_LongPrivateContractUsesFirstMonthEscrowAmount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture command and assert payment method and rounded first-period amount.
     * Expected: For 15-Aug through 14-Oct, eight sessions at 100000, the payment method is DEPOSIT_1M and the command amount is 266666.67.
     * Pre-conditions: Tutor signs a long private assignment after the client; no escrow exists and tutor CCCD is complete.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-018: Calculate the first escrow amount for a private contract longer than one month.")
    void IT_PRV_018_LongPrivateContractUsesFirstMonthEscrowAmount() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.MATCHED);
        tutoringClass.setStartDate(LocalDate.of(2026, 8, 15));
        tutoringClass.setEndDate(LocalDate.of(2026, 10, 14));
        tutoringClass.setNumberOfSessions(8);
        tutoringClass.setTuitionFee(new BigDecimal("100000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(10));
        Contract contract = privateContract(assignment);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(cccdService.getByUserId(TUTOR_USER_ID)).thenReturn(completeCccd("Lê Hoàng Nam", "001200000002"));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        marketplaceService.signAssignmentContract(ASSIGNMENT_ID, "123456");

        var commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals("DEPOSIT_1M", assignment.getPaymentMethod());
        assertEquals(new BigDecimal("266666.67"), commandCaptor.getValue().amount());
    }

    /**
     * Test Case: IT-PRV-019
     * Title: Attach contract context to the notification sent after a client signs.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign).
     * Input: assignmentId=800; OTP 123456.
     * Steps:
     *   1. Prepare the fixture: Client signs a pending private assignment with complete CCCD.
     *   2. Use the input: assignmentId=800; OTP 123456.
     *   3. Execute MarketplaceServiceImpl.signAssignmentContract (POST /api/marketplace/assignments/{assignmentId}/sign). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_019_ClientSignatureNotificationUsesContractContextForNavigation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture notification reference type/id and recipient.
     * Expected: The tutor notification carries referenceType CONTRACT and the class id so the frontend opens the correct contract list/detail context.
     * Pre-conditions: Client signs a pending private assignment with complete CCCD.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-019: Attach contract context to the notification sent after a client signs.")
    void IT_PRV_019_ClientSignatureNotificationUsesContractContextForNavigation() {
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
                anyString(),
                anyString(),
                eq("CONTRACT"),
                eq(CLASS_ID));
    }

    /**
     * Test Case: IT-PRV-020
     * Title: Close a private class after the tutor confirms completion and the client submits a review.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.completeClassAfterClientReview (service path used by client review completion).
     * Input: classId=77.
     * Steps:
     *   1. Prepare the fixture: Tutor completedAt is present, client review is complete and escrow 92 is funded.
     *   2. Use the input: classId=77.
     *   3. Execute MarketplaceServiceImpl.completeClassAfterClientReview (service path used by client review completion). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_PRV_020_ClientReviewCompletionClosesPrivateClassAfterTutorAlreadyConfirmed.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert class/assignment completion and verify release plus repository saves.
     * Expected: The class becomes COMPLETED, clientCompletedAt is set, escrow is released and the assignment/class are saved.
     * Pre-conditions: Tutor completedAt is present, client review is complete and escrow 92 is funded.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-PRV-020: Close a private class after the tutor confirms completion and the client submits a review.")
    void IT_PRV_020_ClientReviewCompletionClosesPrivateClassAfterTutorAlreadyConfirmed() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setTutorCompletedAt(LocalDateTime.now().minusMinutes(10));
        EscrowTransaction escrow = escrow(92L, new BigDecimal("100000.00"));

        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(escrow));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        marketplaceService.completeClassAfterClientReview(CLASS_ID);

        assertEquals(TutoringClassStatus.COMPLETED, tutoringClass.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(assignment.getClientCompletedAt());
        verify(classAssignmentRepository).save(assignment);
        verify(escrowService).apply(any(ReleaseInstruction.class));
        verify(tutoringClassRepository).save(tutoringClass);
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

    

    private CccdInfoDto completeCccd(String fullName, String cccdNumber) {
        return CccdInfoDto.builder()
                .fullName(fullName)
                .cccdNumber(cccdNumber)
                .dateOfBirth("01/01/2000")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build();
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
                    lesson.setLessonDate(LocalDate.now().minusDays(total - sequence));
                    lesson.setAttendanceStatus(sequence <= completed ? AttendanceStatus.COMPLETED : AttendanceStatus.PENDING);
                    return lesson;
                })
                .toList();
    }

    

    private EscrowTransaction escrow(Long escrowId, BigDecimal amount) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setAmount(amount);
        escrow.setStatus(EscrowStatus.FUNDED);
        return escrow;
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

    

    private ClassAssignment pendingSignedAssignment(TutoringClass tutoringClass, User tutorUser) {
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setStatus(ClassAssignmentStatus.PENDING);
        assignment.setClientSignedAt(LocalDateTime.now().minusMinutes(20));
        assignment.setTutorSignedAt(LocalDateTime.now().minusMinutes(10));
        return assignment;
    }

    

    private Contract privateContract(ClassAssignment assignment) {
        Contract contract = new Contract();
        contract.setContractId(880L);
        contract.setContractNo("BF08P-PRIVATE-001");
        contract.setAssignment(assignment);
        return contract;
    }

    

    private PaymentTransaction privateEscrowPayment(Long transactionId, String referenceCode) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(transactionId);
        payment.setReferenceCode(referenceCode);
        payment.setAmount(new BigDecimal("500000.00"));
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.PENDING);
        return payment;
    }

    

    private User tutorUser() {
        return contract.getRecruitmentApplication().getTutor().getUser();
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
}
