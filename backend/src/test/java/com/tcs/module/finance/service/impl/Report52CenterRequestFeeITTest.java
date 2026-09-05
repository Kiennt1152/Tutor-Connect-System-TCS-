package com.tcs.module.finance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.common.event.EscrowFunded;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.response.CenterRequestFeePaymentResponse;
import com.tcs.module.finance.entity.CenterRequestFeeHold;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.entity.WithdrawalRequest;
import com.tcs.module.finance.enums.CenterRequestFeeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.finance.enums.WithdrawalRequestStatus;
import com.tcs.module.finance.repository.CenterRequestFeeHoldRepository;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.repository.WithdrawalRequestRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.service.PaymentNotificationService;
import com.tcs.module.finance.service.WalletService;
import com.tcs.module.finance.service.impl.PaymentReconciliationService;
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
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52CenterRequestFeeITTest {

    private static final String REQUEST_ID = "REQ-CENTER-001";
    private static final Long USER_ID = 7L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long CENTER_USER_ID = 22L;
    private static final Long TUTOR_USER_ID = 33L;

    @Mock private CenterRequestFeeHoldRepository feeHoldRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private WithdrawalRequestRepository withdrawalRequestRepository;
    @Mock private WalletService walletService;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private PaymentNotificationService paymentNotificationService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SystemParameterRepository systemParameterRepository;

    @Mock private AuthHelper authHelper;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private ContractRepository contractRepository;
    @Mock private ContractSignatureRepository contractSignatureRepository;
    @Mock private ContractService contractService;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private EscrowService escrowService;
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

    @Spy
    @InjectMocks
    private CenterRequestFeeServiceImpl centerRequestFeeService;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    private PaymentReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new PaymentReconciliationService(
                paymentTransactionRepository,
                withdrawalRequestRepository,
                walletService,
                paymentNotificationService);
    }

    /**
     * Test Case: IT-CFR-001
     * Title: Create one pending QR payment and fee hold for a client request to a center.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.createPayment (called by POST /api/marketplace/centers/{centerId}/class-requests).
     * Input: request REQ-CENTER-001; client 11; center 22; requested amount 500000; center payout TPBank/02660559201.
     * Steps:
     *   1. Prepare the fixture: No existing fee hold; the platform fee parameter is 0.02 and the system escrow wallet exists.
     *   2. Use the input: request REQ-CENTER-001; client 11; center 22; requested amount 500000; center payout TPBank/02660559201.
     *   3. Execute CenterRequestFeeServiceImpl.createPayment (called by POST /api/marketplace/centers/{centerId}/class-requests). Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_001_CreateCenterRequestFeePaymentBuildsPendingQrHold.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert amount, status, reference, QR, masked account and payment notification.
     * Expected: A PENDING_PAYMENT hold and ESCROW_DEPOSIT transaction are created for 10000 (2% of 500000), with a VietQR URL and masked payout account.
     * Pre-conditions: No existing fee hold; the platform fee parameter is 0.02 and the system escrow wallet exists.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-001: Create one pending QR payment and fee hold for a client request to a center.")
    void IT_CFR_001_CreateCenterRequestFeePaymentBuildsPendingQrHold() {
        SystemParameter feeRate = new SystemParameter();
        feeRate.setParamKey("PLATFORM_FEE_RATE");
        feeRate.setParamValue("0.02");
        Wallet systemWallet = wallet(999L);

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(feeRate));
        when(walletService.getSystemEscrowWallet()).thenReturn(systemWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setTransactionId(501L);
            return tx;
        });
        when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(invocation -> {
            CenterRequestFeeHold hold = invocation.getArgument(0);
            hold.setFeeHoldId(601L);
            return hold;
        });

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(CenterRequestFeeStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(new BigDecimal("10000"), response.getAmount());
        assertTrue(response.getReferenceCode().startsWith("CENTERREQ-"));
        assertEquals(response.getReferenceCode(), response.getTransferContent());
        assertTrue(response.getQrUrl().contains("img.vietqr.io"));
        assertEquals("****6789", response.getPayoutAccountNoMasked());
        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Phí xử lý yêu cầu đã sẵn sàng"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    /**
     * Test Case: IT-CFR-002
     * Title: Read the existing center-request fee payment instead of creating a new one.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.getPayment (used by the center-request screen).
     * Input: requestId=REQ-CENTER-001.
     * Steps:
     *   1. Prepare the fixture: A fee hold and payment transaction already exist for the request.
     *   2. Use the input: requestId=REQ-CENTER-001.
     *   3. Execute CenterRequestFeeServiceImpl.getPayment (used by the center-request screen). Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_002_GetPaymentReturnsExistingCenterRequestFeeHold.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the optional response and its hold/reference fields.
     * Expected: The existing hold 601 is returned with request id REQ-CENTER-001, status PENDING_PAYMENT and reference CENTERREQ-ABC.
     * Pre-conditions: A fee hold and payment transaction already exist for the request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-002: Read the existing center-request fee payment instead of creating a new one.")
    void IT_CFR_002_GetPaymentReturnsExistingCenterRequestFeeHold() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        Optional<CenterRequestFeePaymentResponse> response = centerRequestFeeService.getPayment(REQUEST_ID);

        assertTrue(response.isPresent());
        assertEquals(REQUEST_ID, response.get().getRequestId());
        assertEquals(CenterRequestFeeStatus.PENDING_PAYMENT, response.get().getStatus());
        assertEquals("CENTERREQ-ABC", response.get().getReferenceCode());
    }

    /**
     * Test Case: IT-CFR-003
     * Title: Build the center-request payment detail with QR data and a masked payout account.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.getPayment (used by the payment modal).
     * Input: requestId=REQ-CENTER-001.
     * Steps:
     *   1. Prepare the fixture: The fee hold is linked to class/assignment and contains complete center payout information.
     *   2. Use the input: requestId=REQ-CENTER-001.
     *   3. Execute CenterRequestFeeServiceImpl.getPayment (used by the payment modal). Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_003_PaymentDetailResponseIncludesQrAndMaskedPayoutAccount.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert every QR/detail field and ensure the account is masked separately.
     * Expected: The response includes class 71, assignment 81, TPBank, account 02660559201, reference CENTERREQ-ABC, QR amount 10000 and masked account ****6789.
     * Pre-conditions: The fee hold is linked to class/assignment and contains complete center payout information.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-003: Build the center-request payment detail with QR data and a masked payout account.")
    void IT_CFR_003_PaymentDetailResponseIncludesQrAndMaskedPayoutAccount() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));
        existing.setClassId(71L);
        existing.setAssignmentId(81L);
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.getPayment(REQUEST_ID).orElseThrow();

        assertEquals(71L, response.getClassId());
        assertEquals(81L, response.getAssignmentId());
        assertEquals("TPBank", response.getBankName());
        assertEquals("02660559201", response.getAccountNumber());
        assertEquals("CENTERREQ-ABC", response.getTransferContent());
        assertTrue(response.getQrUrl().contains("amount=10000"));
        assertTrue(response.getQrUrl().contains("addInfo=CENTERREQ-ABC"));
        assertEquals("****6789", response.getPayoutAccountNoMasked());
    }

    /**
     * Test Case: IT-CFR-004
     * Title: Reject center-request fee payment creation when payout information is incomplete.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.createPayment.
     * Input: Blank bank name, account number or account holder.
     * Steps:
     *   1. Prepare the fixture: The request and center are otherwise valid.
     *   2. Use the input: Blank bank name, account number or account holder.
     *   3. Execute CenterRequestFeeServiceImpl.createPayment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_004_RejectPaymentCreationWhenPayoutInformationIsMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the exact error and verify both repository saves are skipped.
     * Expected: The service returns the complete-bank-information error and does not save a payment or hold.
     * Pre-conditions: The request and center are otherwise valid.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-004: Reject center-request fee payment creation when payout information is incomplete.")
    void IT_CFR_004_RejectPaymentCreationWhenPayoutInformationIsMissing() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> centerRequestFeeService.createPayment(
                        REQUEST_ID,
                        CLIENT_USER_ID,
                        CENTER_USER_ID,
                        "Trung tâm Minh Tâm",
                        new BigDecimal("500000.00"),
                        new RefundPayoutInfo("TPBank", "", "Nguyen Van A")));

        assertEquals("Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CFR-005
     * Title: Reject fee payment creation when the request or center cannot be resolved.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.createPayment.
     * Input: Missing/invalid request id with otherwise valid payout info.
     * Steps:
     *   1. Prepare the fixture: The supplied request id or center data does not resolve to a valid business record.
     *   2. Use the input: Missing/invalid request id with otherwise valid payout info.
     *   3. Execute CenterRequestFeeServiceImpl.createPayment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_005_RejectPaymentCreationWhenRequiredRequestOrCenterDataIsMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify payment/hold saves are never called.
     * Expected: The service returns the request/center resolution error and performs no financial persistence.
     * Pre-conditions: The supplied request id or center data does not resolve to a valid business record.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-005: Reject fee payment creation when the request or center cannot be resolved.")
    void IT_CFR_005_RejectPaymentCreationWhenRequiredRequestOrCenterDataIsMissing() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> centerRequestFeeService.createPayment(
                        "",
                        CLIENT_USER_ID,
                        CENTER_USER_ID,
                        "Trung tâm Minh Tâm",
                        new BigDecimal("500000.00"),
                        payoutInfo()));

        assertEquals("Không xác định được thông tin yêu cầu/trung tâm để tạo phí xử lý", exception.getMessage());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CFR-006
     * Title: Block an anonymous user before creating a center class request or fee hold.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.createClassRequest (POST /api/marketplace/centers/{centerId}/class-requests).
     * Input: centerId=77 and a valid class-request payload.
     * Steps:
     *   1. Prepare the fixture: No authenticated user.
     *   2. Use the input: centerId=77 and a valid class-request payload.
     *   3. Execute MarketplaceServiceImpl.createClassRequest (POST /api/marketplace/centers/{centerId}/class-requests). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CFR_006_BlockAnonymousCenterRequestBeforeCreatingFeeHold.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify ClassRequestStore.create and fee-service.createPayment are skipped.
     * Expected: The service returns “Yêu cầu đăng nhập” and neither the class request nor fee payment is created.
     * Pre-conditions: No authenticated user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-006: Block an anonymous user before creating a center class request or fee hold.")
    void IT_CFR_006_BlockAnonymousCenterRequestBeforeCreatingFeeHold() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClassRequest(77L, classRequestCreateRequest()));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(classRequestStore, never()).create(any(), any(), any(), any(), any(), any());
        verify(centerRequestFeeService, never()).createPayment(any(), any(), any(), any(), any(), any());
    }

    /**
     * Test Case: IT-CFR-007
     * Title: Prevent a tutor from creating a client-to-center request.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.createClassRequest (POST /api/marketplace/centers/{centerId}/class-requests).
     * Input: centerId=77 and a valid request payload.
     * Steps:
     *   1. Prepare the fixture: Authenticated user has TUTOR role and no Client profile.
     *   2. Use the input: centerId=77 and a valid request payload.
     *   3. Execute MarketplaceServiceImpl.createClassRequest (POST /api/marketplace/centers/{centerId}/class-requests). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_CFR_007_BlockTutorRoleFromCreatingClientCenterRequest.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert role error and verify request creation is skipped.
     * Expected: The service returns the client-only permission message and creates no request.
     * Pre-conditions: Authenticated user has TUTOR role and no Client profile.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-007: Prevent a tutor from creating a client-to-center request.")
    void IT_CFR_007_BlockTutorRoleFromCreatingClientCenterRequest() {
        User tutorUser = user(TUTOR_USER_ID);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClassRequest(77L, classRequestCreateRequest()));

        assertEquals("Chỉ phụ huynh/khách hàng mới tạo lớp học", exception.getMessage());
        verify(tutorCenterRepository, never()).findById(any());
        verify(classRequestStore, never()).create(any(), any(), any(), any(), any(), any());
    }

    /**
     * Test Case: IT-CFR-008
     * Title: Ignore a replayed successful center-fee webhook after the hold is already HELD.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.completeIncomingPayment (called by the incoming SePay webhook).
     * Input: Replay external transaction id SEPAY-IN-DUP.
     * Steps:
     *   1. Prepare the fixture: Payment 501 is SUCCESS and its fee hold 601 is already HELD.
     *   2. Use the input: Replay external transaction id SEPAY-IN-DUP.
     *   3. Execute CenterRequestFeeServiceImpl.completeIncomingPayment (called by the incoming SePay webhook). Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_008_ReplayedSuccessfulWebhookDoesNotCreateDuplicatePaidRequest.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status and verify all duplicate writes are absent.
     * Expected: The response remains HELD and no payment, hold or class-request row is saved a second time.
     * Pre-conditions: Payment 501 is SUCCESS and its fee hold 601 is already HELD.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-008: Ignore a replayed successful center-fee webhook after the hold is already HELD.")
    void IT_CFR_008_ReplayedSuccessfulWebhookDoesNotCreateDuplicatePaidRequest() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        hold.setStatus(CenterRequestFeeStatus.HELD);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-DUP");

        assertEquals(CenterRequestFeeStatus.HELD, response.getStatus());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
        verify(classRequestStore, never()).save(any());
    }

    /**
     * Test Case: IT-CFR-009
     * Title: Return an existing fee hold when the client repeats payment creation.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.createPayment.
     * Input: Repeat the same request, client, center, amount and payout data.
     * Steps:
     *   1. Prepare the fixture: A fee hold already exists for REQ-CENTER-001.
     *   2. Use the input: Repeat the same request, client, center, amount and payout data.
     *   3. Execute CenterRequestFeeServiceImpl.createPayment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_009_ReturnExistingHoldInsteadOfCreatingDuplicatePayment.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert existing ids/reference and verify no save calls.
     * Expected: The existing hold 601 and reference CENTERREQ-ABC are returned without a second transaction.
     * Pre-conditions: A fee hold already exists for REQ-CENTER-001.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-009: Return an existing fee hold when the client repeats payment creation.")
    void IT_CFR_009_ReturnExistingHoldInsteadOfCreatingDuplicatePayment() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(601L, response.getFeeHoldId());
        assertEquals("CENTERREQ-ABC", response.getReferenceCode());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CFR-010
     * Title: Link a fulfilled center request to its class and assignment.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.linkFulfilledAssignment.
     * Input: request REQ-CENTER-001; classId=71; assignmentId=81.
     * Steps:
     *   1. Prepare the fixture: Hold 601 exists for the request.
     *   2. Use the input: request REQ-CENTER-001; classId=71; assignmentId=81.
     *   3. Execute CenterRequestFeeServiceImpl.linkFulfilledAssignment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_010_LinkFulfilledRequestStoresClassAndAssignmentTrace.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture hold and assert both trace fields.
     * Expected: The fee hold stores classId 71 and assignmentId 81 and is saved.
     * Pre-conditions: Hold 601 exists for the request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-010: Link a fulfilled center request to its class and assignment.")
    void IT_CFR_010_LinkFulfilledRequestStoresClassAndAssignmentTrace() {
        CenterRequestFeeHold hold = pendingHold(601L, pendingPayment(501L));
        hold.setStatus(CenterRequestFeeStatus.HELD);
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));

        centerRequestFeeService.linkFulfilledAssignment(REQUEST_ID, 71L, 81L);

        assertEquals(71L, hold.getClassId());
        assertEquals(81L, hold.getAssignmentId());
        verify(feeHoldRepository).save(hold);
    }

    /**
     * Test Case: IT-CFR-011
     * Title: Notify the client and platform admins when a center-fee refund is requested.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.requestRefund.
     * Input: request REQ-CENTER-001; reason “Trung tâm không thể tìm gia sư phù hợp”.
     * Steps:
     *   1. Prepare the fixture: Fee hold 601 is HELD and has complete center/user data.
     *   2. Use the input: request REQ-CENTER-001; reason “Trung tâm không thể tìm gia sư phù hợp”.
     *   3. Execute CenterRequestFeeServiceImpl.requestRefund. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_011_RequestRefundNotifiesClientAndPlatformAdmin.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture both notification recipients/templates/reference ids.
     * Expected: The refund request is created and both the admin and client receive the correct Vietnamese notifications with REFUND_REQUEST reference.
     * Pre-conditions: Fee hold 601 is HELD and has complete center/user data.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-011: Notify the client and platform admins when a center-fee refund is requested.")
    void IT_CFR_011_RequestRefundNotifiesClientAndPlatformAdmin() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        hold.setStatus(CenterRequestFeeStatus.HELD);
        User centerUser = user(CENTER_USER_ID, "center.it@tcs.test");
        User adminUser = user(1L, "admin.it@tcs.test");

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));
        when(refundRequestRepository.findFirstByCenterRequestFeeHold_FeeHoldIdOrderByRequestedAtDesc(601L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(CENTER_USER_ID)).thenReturn(Optional.of(centerUser));
        when(platformAdminRepository.findAll()).thenReturn(java.util.List.of(platformAdmin(adminUser)));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refund = invocation.getArgument(0);
            refund.setRefundId(701L);
            return refund;
        });

        centerRequestFeeService.requestRefund(REQUEST_ID, "Trung tâm không thể tìm gia sư phù hợp");

        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu hoàn phí trung tâm mới"),
                any(),
                eq("REFUND_REQUEST"),
                eq(701L));
        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Đã tạo yêu cầu hoàn phí"),
                any(),
                eq("REFUND_REQUEST"),
                eq(701L));
    }

    /**
     * Test Case: IT-CFR-012
     * Title: Return the same QR reference when a pending center-fee payment is re-opened.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.createPayment.
     * Input: Repeat payment creation for REQ-CENTER-001.
     * Steps:
     *   1. Prepare the fixture: The request has an existing PENDING_PAYMENT hold.
     *   2. Use the input: Repeat payment creation for REQ-CENTER-001.
     *   3. Execute CenterRequestFeeServiceImpl.createPayment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_012_ReloadPendingPaymentReturnsSameQrReferenceWithoutCreatingANewHold.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert stable hold/reference and verify no repository saves.
     * Expected: Reloading the payment returns hold 601 and transfer content CENTERREQ-ABC without creating a new hold.
     * Pre-conditions: The request has an existing PENDING_PAYMENT hold.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-012: Return the same QR reference when a pending center-fee payment is re-opened.")
    void IT_CFR_012_ReloadPendingPaymentReturnsSameQrReferenceWithoutCreatingANewHold() {
        CenterRequestFeeHold existing = pendingHold(601L, pendingPayment(501L));
        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(601L, response.getFeeHoldId());
        assertEquals("CENTERREQ-ABC", response.getTransferContent());
        verify(paymentTransactionRepository, never()).save(any());
        verify(feeHoldRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CFR-013
     * Title: Mark an incoming center-fee payment successful and move its hold to HELD.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.completeIncomingPayment (POST /api/finance/webhooks/sepay/in).
     * Input: External transaction id SEPAY-IN-1.
     * Steps:
     *   1. Prepare the fixture: Payment 501 is pending and linked to hold 601.
     *   2. Use the input: External transaction id SEPAY-IN-1.
     *   3. Execute CenterRequestFeeServiceImpl.completeIncomingPayment (POST /api/finance/webhooks/sepay/in). Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_013_CompleteIncomingPaymentMovesHoldToHeldAndNotifiesClientAndCenter.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert both statuses/external id and notification recipients/references.
     * Expected: Payment 501 becomes SUCCESS, hold 601 becomes HELD and both client and center receive notifications.
     * Pre-conditions: Payment 501 is pending and linked to hold 601.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-013: Mark an incoming center-fee payment successful and move its hold to HELD.")
    void IT_CFR_013_CompleteIncomingPaymentMovesHoldToHeldAndNotifiesClientAndCenter() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));
        when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

        CenterRequestFeePaymentResponse response = centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-1");

        assertEquals(CenterRequestFeeStatus.HELD, response.getStatus());
        assertEquals(PaymentTransactionStatus.SUCCESS, payment.getStatus());
        assertEquals("SEPAY-IN-1", payment.getExternalTransactionId());
        verify(paymentTransactionRepository).save(payment);
        verify(feeHoldRepository).save(hold);
        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Thanh toán phí yêu cầu thành công"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
        verify(paymentNotificationService).notifyPayment(
                eq(CENTER_USER_ID),
                eq("Có yêu cầu mới đã thanh toán phí"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    /**
     * Test Case: IT-CFR-014
     * Title: Apply the configured platform fee rate to the center-request QR amount.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.createPayment.
     * Input: request amount 500000 with complete center payout info.
     * Steps:
     *   1. Prepare the fixture: No existing hold; platform fee rate is configured to 0.05.
     *   2. Use the input: request amount 500000 with complete center payout info.
     *   3. Execute CenterRequestFeeServiceImpl.createPayment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_014_ConfiguredFeeRateMatchesQrAmountAndPendingTransaction.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture transaction and assert amount, QR amount and rate description.
     * Expected: With rate 0.05 and request amount 500000, the pending transaction and QR both use 25000 and the description records 5%.
     * Pre-conditions: No existing hold; platform fee rate is configured to 0.05.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-014: Apply the configured platform fee rate to the center-request QR amount.")
    void IT_CFR_014_ConfiguredFeeRateMatchesQrAmountAndPendingTransaction() {
        SystemParameter feeRate = new SystemParameter();
        feeRate.setParamKey("PLATFORM_FEE_RATE");
        feeRate.setParamValue("0.05");
        Wallet systemWallet = wallet(999L);

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.of(feeRate));
        when(walletService.getSystemEscrowWallet()).thenReturn(systemWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setTransactionId(502L);
            return tx;
        });
        when(feeHoldRepository.save(any(CenterRequestFeeHold.class))).thenAnswer(invocation -> {
            CenterRequestFeeHold hold = invocation.getArgument(0);
            hold.setFeeHoldId(602L);
            return hold;
        });

        CenterRequestFeePaymentResponse response = centerRequestFeeService.createPayment(
                REQUEST_ID,
                CLIENT_USER_ID,
                CENTER_USER_ID,
                "Trung tâm Minh Tâm",
                new BigDecimal("500000.00"),
                payoutInfo());

        assertEquals(new BigDecimal("25000"), response.getAmount());
        assertTrue(response.getQrUrl().contains("amount=25000"));
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        assertEquals(new BigDecimal("25000"), txCaptor.getValue().getAmount());
        assertTrue(txCaptor.getValue().getDescription().contains("5%"));
    }

    /**
     * Test Case: IT-CFR-015
     * Title: Update the center request from PAYMENT_PENDING to PENDING after payment confirmation.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.completeIncomingPayment.
     * Input: External transaction id SEPAY-IN-2.
     * Steps:
     *   1. Prepare the fixture: The class request is PAYMENT_PENDING and linked to payment 501/hold 601.
     *   2. Use the input: External transaction id SEPAY-IN-2.
     *   3. Execute CenterRequestFeeServiceImpl.completeIncomingPayment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_015_PaymentConfirmationUpdatesHoldAndClassRequestStatusTrace.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert statuses and verify the saved request is the pending state.
     * Expected: The fee hold and payment become held/successful and ClassRequestStore saves the request with STATUS_PENDING.
     * Pre-conditions: The class request is PAYMENT_PENDING and linked to payment 501/hold 601.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-015: Update the center request from PAYMENT_PENDING to PENDING after payment confirmation.")
    void IT_CFR_015_PaymentConfirmationUpdatesHoldAndClassRequestStatusTrace() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        ClassRequestStore.ClassRequestData data = new ClassRequestStore.ClassRequestData(
                REQUEST_ID,
                CLIENT_USER_ID,
                33L,
                null,
                "Nhờ trung tâm tìm gia sư",
                new BigDecimal("500000.00"),
                ClassRequestStore.STATUS_PAYMENT_PENDING,
                null,
                LocalDateTime.now().toString(),
                "{}",
                java.util.List.of(),
                null);
        ClassRequestStore.ClassRequestData pendingData = new ClassRequestStore.ClassRequestData(
                REQUEST_ID,
                CLIENT_USER_ID,
                33L,
                null,
                "Nhờ trung tâm tìm gia sư",
                new BigDecimal("500000.00"),
                ClassRequestStore.STATUS_PENDING,
                null,
                data.createdAt(),
                "{}",
                java.util.List.of(),
                null);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));
        when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.of(data));
        when(classRequestStore.withStatus(data, ClassRequestStore.STATUS_PENDING, null)).thenReturn(pendingData);

        CenterRequestFeePaymentResponse response = centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-2");

        assertEquals(CenterRequestFeeStatus.HELD, response.getStatus());
        assertEquals(CenterRequestFeeStatus.HELD, hold.getStatus());
        assertEquals(PaymentTransactionStatus.SUCCESS, payment.getStatus());
        verify(classRequestStore).save(pendingData);
        verify(feeHoldRepository).save(hold);
    }

    /**
     * Test Case: IT-CFR-016
     * Title: Release a fulfilled center-request fee to the center wallet.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.releaseForFulfilledAssignment.
     * Input: assignmentId=77; reason “Lớp đã hoàn thành”.
     * Steps:
     *   1. Prepare the fixture: Assignment 77 has a HELD center-request fee.
     *   2. Use the input: assignmentId=77; reason “Lớp đã hoàn thành”.
     *   3. Execute CenterRequestFeeServiceImpl.releaseForFulfilledAssignment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_016_ReleaseFulfilledCenterRequestFeeToCenterWallet.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Verify wallet credit reference CENTERREQ_RELEASE-601, transaction save and notification.
     * Expected: Hold 601 becomes RELEASED, the center wallet is credited with its amount and the center is notified.
     * Pre-conditions: Assignment 77 has a HELD center-request fee.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-016: Release a fulfilled center-request fee to the center wallet.")
    void IT_CFR_016_ReleaseFulfilledCenterRequestFeeToCenterWallet() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        hold.setStatus(CenterRequestFeeStatus.HELD);
        hold.setAssignmentId(77L);
        Wallet centerWallet = wallet(CENTER_USER_ID);

        when(feeHoldRepository.findFirstByAssignmentIdOrderByCreatedAtDesc(77L)).thenReturn(Optional.of(hold));
        when(walletService.getOrCreate(CENTER_USER_ID)).thenReturn(centerWallet);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        centerRequestFeeService.releaseForFulfilledAssignment(77L, "Lớp đã hoàn thành");

        assertEquals(CenterRequestFeeStatus.RELEASED, hold.getStatus());
        verify(walletService).credit(CENTER_USER_ID, hold.getAmount(), "CENTERREQ_RELEASE-601");
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(paymentNotificationService).notifyPayment(
                eq(CENTER_USER_ID),
                eq("Đã nhận phí xử lý yêu cầu"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    /**
     * Test Case: IT-CFR-017
     * Title: Cancel an unpaid center-request fee hold and remove the draft request.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.cancelUnpaid.
     * Input: requestId=REQ-CENTER-001.
     * Steps:
     *   1. Prepare the fixture: REQ-CENTER-001 has a pending payment/fee hold and has not been paid.
     *   2. Use the input: requestId=REQ-CENTER-001.
     *   3. Execute CenterRequestFeeServiceImpl.cancelUnpaid. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_017_CancelUnpaidFeeHoldCancelsPendingPaymentAndDeletesDraftRequest.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert both statuses and verify payment/hold saves plus request deletion.
     * Expected: The payment and hold become CANCELLED and the draft request is deleted.
     * Pre-conditions: REQ-CENTER-001 has a pending payment/fee hold and has not been paid.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-017: Cancel an unpaid center-request fee hold and remove the draft request.")
    void IT_CFR_017_CancelUnpaidFeeHoldCancelsPendingPaymentAndDeletesDraftRequest() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));

        centerRequestFeeService.cancelUnpaid(REQUEST_ID);

        assertEquals(PaymentTransactionStatus.CANCELLED, payment.getStatus());
        assertEquals(CenterRequestFeeStatus.CANCELLED, hold.getStatus());
        verify(paymentTransactionRepository).save(payment);
        verify(feeHoldRepository).save(hold);
        verify(classRequestStore).delete(REQUEST_ID);
    }

    /**
     * Test Case: IT-CFR-018
     * Title: Keep a center-request fee outside the class escrow timeout job.
     * Procedure: Prepare the stated fixture and input, then execute PaymentReconciliationService.expirePendingEscrowDeposits (scheduled reconciliation).
     * Input: Current time 2026-08-31 10:00; transaction created 20 minutes earlier.
     * Steps:
     *   1. Prepare the fixture: Center-request fee transaction is pending and older than the normal threshold.
     *   2. Use the input: Current time 2026-08-31 10:00; transaction created 20 minutes earlier.
     *   3. Execute PaymentReconciliationService.expirePendingEscrowDeposits (scheduled reconciliation). Mapped test: com.tcs.module.finance.service.impl.Report52PaymentReconciliationITTest#IT_CFR_018_KeepCenterRequestFeeOutsideClassEscrowTimeout.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert changed count 0, status PENDING and empty saveAll.
     * Expected: A pending ESCROW_DEPOSIT transaction with CENTERREQ reference is not cancelled by the class-escrow timeout path.
     * Pre-conditions: Center-request fee transaction is pending and older than the normal threshold.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-018: Keep a center-request fee outside the class escrow timeout job.")
    void IT_CFR_018_KeepCenterRequestFeeOutsideClassEscrowTimeout() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        PaymentTransaction centerRequestFee = transaction(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                new BigDecimal("2500.00"),
                "CENTERREQ-ABC12345",
                now.minusMinutes(20));

        when(paymentTransactionRepository.findByTypeAndStatusAndCreatedAtBefore(
                PaymentTransactionType.ESCROW_DEPOSIT,
                PaymentTransactionStatus.PENDING,
                now.minusMinutes(15)))
                .thenReturn(List.of(centerRequestFee));

        int changed = reconciliationService.expirePendingEscrowDeposits(now);

        assertEquals(0, changed);
        assertEquals(PaymentTransactionStatus.PENDING, centerRequestFee.getStatus());
        verify(paymentTransactionRepository).saveAll(List.of());
    }

    /**
     * Test Case: IT-CFR-019
     * Title: Use the fee-hold reference when notifying users about a successful center-request payment.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.completeIncomingPayment.
     * Input: External transaction id SEPAY-IN-3.
     * Steps:
     *   1. Prepare the fixture: Pending payment 501 is linked to fee hold 601.
     *   2. Use the input: External transaction id SEPAY-IN-3.
     *   3. Execute CenterRequestFeeServiceImpl.completeIncomingPayment. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_019_PaymentSuccessNotificationsUseClassRequestFeeReferenceForRequestList.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture both notifications and compare reference type/id.
     * Expected: Client and center notifications use referenceType CLASS_REQUEST_FEE and hold id 601 for frontend request-list navigation.
     * Pre-conditions: Pending payment 501 is linked to fee hold 601.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-019: Use the fee-hold reference when notifying users about a successful center-request payment.")
    void IT_CFR_019_PaymentSuccessNotificationsUseClassRequestFeeReferenceForRequestList() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);

        when(feeHoldRepository.findByPaymentTransaction_TransactionId(501L)).thenReturn(Optional.of(hold));
        when(classRequestStore.find(REQUEST_ID)).thenReturn(Optional.empty());

        centerRequestFeeService.completeIncomingPayment(payment, "SEPAY-IN-3");

        verify(paymentNotificationService).notifyPayment(
                eq(CLIENT_USER_ID),
                eq("Thanh toán phí yêu cầu thành công"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
        verify(paymentNotificationService).notifyPayment(
                eq(CENTER_USER_ID),
                eq("Có yêu cầu mới đã thanh toán phí"),
                any(),
                eq("CLASS_REQUEST_FEE"),
                eq(601L));
    }

    /**
     * Test Case: IT-CFR-020
     * Title: Create a pending refund request and encode the center-fee payout details.
     * Procedure: Prepare the stated fixture and input, then execute CenterRequestFeeServiceImpl.requestRefund.
     * Input: Refund reason “Trung tâm không thể tìm gia sư phù hợp”.
     * Steps:
     *   1. Prepare the fixture: Fee hold 601 is HELD and center payout details are complete.
     *   2. Use the input: Refund reason “Trung tâm không thể tìm gia sư phù hợp”.
     *   3. Execute CenterRequestFeeServiceImpl.requestRefund. Mapped test: com.tcs.module.finance.service.impl.Report52CenterRequestFeeITTest#IT_CFR_020_RequestRefundCreatesAdminTransferAndMarksHoldRefundRequested.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture RefundRequest and assert status, transfer status, reference and masked account.
     * Expected: Hold 601 becomes REFUND_REQUESTED; the refund is PENDING with a transfer reference and masked account, and admins are notified.
     * Pre-conditions: Fee hold 601 is HELD and center payout details are complete.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CFR-020: Create a pending refund request and encode the center-fee payout details.")
    void IT_CFR_020_RequestRefundCreatesAdminTransferAndMarksHoldRefundRequested() {
        PaymentTransaction payment = pendingPayment(501L);
        CenterRequestFeeHold hold = pendingHold(601L, payment);
        hold.setStatus(CenterRequestFeeStatus.HELD);
        User centerUser = user(CENTER_USER_ID, "center.it@tcs.test");
        User adminUser = user(1L, "admin.it@tcs.test");

        when(feeHoldRepository.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(hold));
        when(refundRequestRepository.findFirstByCenterRequestFeeHold_FeeHoldIdOrderByRequestedAtDesc(601L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(CENTER_USER_ID)).thenReturn(Optional.of(centerUser));
        when(platformAdminRepository.findAll()).thenReturn(java.util.List.of(platformAdmin(adminUser)));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refund = invocation.getArgument(0);
            refund.setRefundId(701L);
            return refund;
        });

        centerRequestFeeService.requestRefund(REQUEST_ID, "Trung tâm không thể tìm gia sư phù hợp");

        assertEquals(CenterRequestFeeStatus.REFUND_REQUESTED, hold.getStatus());
        ArgumentCaptor<RefundRequest> refundCaptor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(refundRequestRepository).save(refundCaptor.capture());
        RefundRequest refund = refundCaptor.getValue();
        assertEquals(RefundRequestStatus.PENDING, refund.getStatus());
        assertEquals("PENDING", refund.getTransferStatus());
        assertEquals("REFUND-CREQFEE-601", refund.getRefundReferenceCode());
        assertEquals("****6789", com.tcs.module.finance.util.RefundPayoutInfoCodec.maskAccountNo(refund.getAccountNo()));
        verify(feeHoldRepository).save(hold);
        verify(paymentNotificationService).notifyPayment(
                eq(adminUser),
                eq("Có yêu cầu hoàn phí trung tâm mới"),
                any(),
                eq("REFUND_REQUEST"),
                eq(701L));
    }



    private CenterRequestFeeHold pendingHold(Long holdId, PaymentTransaction payment) {
        CenterRequestFeeHold hold = new CenterRequestFeeHold();
        hold.setFeeHoldId(holdId);
        hold.setRequestId(REQUEST_ID);
        hold.setClientUserId(CLIENT_USER_ID);
        hold.setCenterUserId(CENTER_USER_ID);
        hold.setCenterName("Trung tâm Minh Tâm");
        hold.setPaymentTransaction(payment);
        hold.setProjectedEscrowAmount(new BigDecimal("500000.00"));
        hold.setAmount(new BigDecimal("10000.00"));
        hold.setReferenceCode("CENTERREQ-ABC");
        hold.setPayoutBankName("TPBank");
        hold.setPayoutAccountNo("0123456789");
        hold.setPayoutAccountHolderName("Nguyen Van A");
        hold.setStatus(CenterRequestFeeStatus.PENDING_PAYMENT);
        hold.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 0));
        return hold;
    }

    private PaymentTransaction pendingPayment(Long transactionId) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(transactionId);
        payment.setWallet(wallet(999L));
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.PENDING);
        payment.setAmount(new BigDecimal("10000.00"));
        payment.setReferenceCode("CENTERREQ-ABC");
        payment.setCreatedAt(LocalDateTime.of(2026, 8, 31, 9, 0));
        return payment;
    }

    private RefundPayoutInfo payoutInfo() {
        return new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A");
    }

    private Wallet wallet(Long walletId) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        return wallet;
    }

    private PlatformAdmin platformAdmin(User user) {
        PlatformAdmin admin = new PlatformAdmin();
        admin.setAdminId(user.getUserId());
        admin.setUser(user);
        return admin;
    }

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        return user;
    }


    private User user(Long userId) {
        return user(userId, "user" + userId + "@tcs.test");
    }

    

    private ClassRequestCreateRequest classRequestCreateRequest() {
        ClassRequestCreateRequest request = new ClassRequestCreateRequest();
        request.setNote("Gia đình muốn tìm gia sư Toán lớp 9 học buổi tối.");
        request.setDesiredBudget(new BigDecimal("500000.00"));
        request.setRefundPayoutInfo(new com.tcs.module.finance.dto.RefundPayoutInfo(
                "TPBank",
                "0123456789",
                "Nguyen Thu Ha"));
        return request;
    }

    

    private PaymentTransaction transaction(
            PaymentTransactionType type,
            PaymentTransactionStatus status,
            BigDecimal amount,
            String referenceCode,
            LocalDateTime createdAt) {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setWallet(wallet(USER_ID));
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setReferenceCode(referenceCode);
        transaction.setCreatedAt(createdAt);
        return transaction;
    }
}
