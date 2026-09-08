package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.tcs.module.center.service.impl.*;
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
import com.tcs.module.finance.service.*;
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
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
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
 * Report 5.2 F06 Center Class Enrollment: all test functions for this sheet.
 * Separate package-private classes preserve each original JUnit test context.
 */
@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52F06CenterClassEnrollmentITTest {

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

    @Test
    @Tag("report52-it")
    void IT_CCE_006_BlockAnonymousCenterEnrollmentBeforeStudentRecordCreation() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.registerToClass(CLASS_ID));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(classStudentRepository, never()).save(any());
        verify(contractService, never()).generateStudentContract(any());
    }

    @Test
    void SUPPORT_CCE_RejectCenterEnrollmentWhenClassIsNotOpen() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = centerClass(user(99L), TutoringClassStatus.DRAFT);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.registerToClass(CLASS_ID));

        assertEquals("Lớp chưa mở đăng ký", exception.getMessage());
        verify(classStudentRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_CCE_013_RegisterCenterClassGeneratesEnrollmentContractForStudent() {
        User centerUser = user(99L);
        User clientUser = user(CLIENT_USER_ID);
        Client client = client(clientUser);
        TutoringClass tutoringClass = centerClass(centerUser, TutoringClassStatus.OPEN);

        stubSuccessfulCenterEnrollment(clientUser, client, tutoringClass);

        marketplaceService.registerToClass(CLASS_ID);

        verify(contractService).generateStudentContract(CLASS_STUDENT_ID);
    }

    @Test
    @Tag("report52-it")
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
class Report52F06CenterClassEnrollmentPart2ITTest {

    private static final Long CENTER_USER_ID = 100L;
    private static final Long CLASS_ID = 500L;
    private static final Long TUTOR_ID = 20L;

    @Mock private AuthHelper authHelper;
    @Mock private RecruitmentPostRepository recruitmentPostRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private CenterTutorMembershipRepository membershipRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private VerificationRequestRepository verificationRequestRepository;
    @Mock private VerificationDocumentRepository verificationDocumentRepository;
    @Mock private CenterEscrowAutoSettlementService centerEscrowAutoSettlementService;
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
    @Mock private ContractService contractService;
    @Mock private ContractTemplateRepository contractTemplateRepository;
    @Mock private CccdService cccdService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private CenterServiceImpl centerService;

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    void SUPPORT_CCE_CreateCenterClassStoresDraftAndAuditHistory() {
        TutorCenter center = verifiedCenter();
        SaveClassRequest request = validCenterClassRequest();

        loginAsCenter(center);
        when(walletRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(activeWallet()));
        stubCatalogLookups();
        when(tutoringClassRepository.save(any(TutoringClass.class))).thenAnswer(invocation -> {
            TutoringClass saved = invocation.getArgument(0);
            saved.setClassId(CLASS_ID);
            return saved;
        });
        when(scheduleSlotRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of());
        when(scheduleSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(centerClass(center, TutoringClassStatus.DRAFT)));
        stubClassResponseDependencies(centerClass(center, TutoringClassStatus.DRAFT), List.of());

        CenterClassResponse response = centerService.createClass(request);

        assertEquals(CLASS_ID, response.getClassId());
        ArgumentCaptor<TutoringClass> classCaptor = ArgumentCaptor.forClass(TutoringClass.class);
        verify(tutoringClassRepository).save(classCaptor.capture());
        assertEquals(ClassType.CENTER, classCaptor.getValue().getClassType());
        assertEquals(TutoringClassStatus.DRAFT, classCaptor.getValue().getStatus());
        assertEquals(4, classCaptor.getValue().getNumberOfSessions());
        verify(auditLogService).record(eq(CENTER_USER_ID), eq("CREATE_CENTER_CLASS"),
                eq("TutoringClass"), eq(CLASS_ID), eq(null), eq(request));
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
}

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
class Report52F06CenterClassEnrollmentPart3ITTest {

    private static final Long CLASS_ID = 10L;
    private static final Long CLASS_STUDENT_ID = 20L;
    private static final Long ESCROW_ID = 30L;

    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private EscrowService escrowService;

    @InjectMocks
    private CenterEscrowAutoSettlementService settlementService;

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
}

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52F06CenterClassEnrollmentPart4ITTest {

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

        contractService.signWithOtp(902L, otp("123456"));

        ArgumentCaptor<EscrowLockCommand> commandCaptor = ArgumentCaptor.forClass(EscrowLockCommand.class);
        verify(escrowService).preparePayment(commandCaptor.capture());
        assertEquals(clientUser.getUserId(), commandCaptor.getValue().payerUserId());
        assertEquals(88L, commandCaptor.getValue().classStudentId());
        assertEquals(new BigDecimal("600000.00"), commandCaptor.getValue().amount());
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
