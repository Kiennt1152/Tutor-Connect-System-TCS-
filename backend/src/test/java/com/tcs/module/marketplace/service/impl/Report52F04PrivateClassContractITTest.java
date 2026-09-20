package com.tcs.module.marketplace.service.impl;

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
import com.tcs.module.contract.service.impl.*;
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
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Report 5.2 F04 Private Class Contract: all test functions for this sheet.
 * Separate package-private classes preserve each original JUnit test context.
 */
@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52F04PrivateClassContractITTest {

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

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;




















    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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


    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
        when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(true);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(escrow));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        String message = marketplaceService.confirmClassCompletion(CLASS_ID);

        assertEquals("Lớp đã hoàn thành. Học phí ký quỹ đã được giải ngân cho gia sư.", message);
        assertEquals(TutoringClassStatus.COMPLETED, tutoringClass.getStatus());
        verify(escrowService).apply(any(ReleaseInstruction.class));
        verify(centerRequestFeeService).releaseForFulfilledAssignment(eq(ASSIGNMENT_ID), anyString());
        verify(tutoringClassRepository).save(tutoringClass);
    }

    @Test
    @Tag("report52-it")
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


















    private CreateClassTerminationRequest terminationRequest() {
        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");
        return request;
    }

    private CreateClassRequest createClassRequest() {
        CreateClassRequest request = new CreateClassRequest();
        request.setTitle("Cần gia sư Toán lớp 9");
        request.setDetailsJson("{\"subjectIds\":[\"101\"],\"slots\":[]}");
        request.setBudget(new BigDecimal("120000.00"));
        return request;
    }

    private ApplyClassRequest applyClassRequest() {
        ApplyClassRequest request = new ApplyClassRequest();
        request.setProposedRate(new BigDecimal("120000.00"));
        request.setCoverLetter("Em có kinh nghiệm dạy Toán THCS.");
        return request;
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

    private CccdInfoDto completeCccd(String fullName, String cccdNumber) {
        return CccdInfoDto.builder()
                .fullName(fullName)
                .cccdNumber(cccdNumber)
                .dateOfBirth("01/01/2000")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build();
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

    private Wallet activeWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(user.getUserId());
        wallet.setUser(user);
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
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

    private TutoringClass centerClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = tutoringClass(creator, status);
        tutoringClass.setClassType(com.tcs.module.marketplace.enums.ClassType.CENTER);
        tutoringClass.setMaxStudents(20);
        return tutoringClass;
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
                    lesson.setLessonDate(LocalDate.now().minusDays(total - sequence));
                    lesson.setAttendanceStatus(sequence <= completed ? AttendanceStatus.COMPLETED : AttendanceStatus.PENDING);
                    return lesson;
                })
                .toList();
    }
}

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52F04PrivateClassContractPart2ITTest {

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








    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_PRV_006_BlockAnonymousContractListBeforeReturningPrivateContracts() {
        when(authHelper.requireAuthenticated()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> contractService.getMyContracts());

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(contractRepository, never()).findContractsByUserId(anyLong());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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











    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
}
