package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
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
import jakarta.persistence.Query;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Report 5.2 F03 Marketplace Tutor: all test functions for this sheet.
 * Separate package-private classes preserve each original JUnit test context.
 */
@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52F03MarketplaceTutorITTest {

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

    @Test
    @Tag("report52-it")
    void IT_MKT_002_ListOpenMarketplaceClassesFiltersByStatus() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass openClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);
        when(tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN)).thenReturn(List.of(openClass));

        List<?> responses = marketplaceService.listClasses(TutoringClassStatus.OPEN);

        assertEquals(1, responses.size());
        verify(tutoringClassRepository).findByStatus(TutoringClassStatus.OPEN);
        verify(tutoringClassRepository, never()).findAll();
    }

    @Test
    @Tag("report52-it")
    void IT_MKT_003_GetClassDetailLoadsTargetMarketplaceRecord() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass openClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(openClass));

        var response = marketplaceService.getClass(CLASS_ID, null, null);

        assertEquals(CLASS_ID, response.getClassId());
        assertEquals("Lớp toán", response.getTitle());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_MKT_006_BlockAnonymousClassCreationBeforeRepositoryMutation() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClass(createClassRequest()));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

        ArgumentCaptor<TutorApplication> applicationCaptor = ArgumentCaptor.forClass(TutorApplication.class);
        verify(tutorApplicationRepository).save(applicationCaptor.capture());
        assertEquals(com.tcs.module.marketplace.enums.TutorApplicationStatus.SUBMITTED,
                applicationCaptor.getValue().getStatus());
        assertEquals(tutoringClass, applicationCaptor.getValue().getTutoringClass());
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
    void IT_MKT_015_ListMyClassesReturnsOnlyRecordsOwnedByCurrentClient() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass mine = tutoringClass(clientUser, TutoringClassStatus.DRAFT);
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(tutoringClassRepository.findByCreator_UserId(CLIENT_USER_ID)).thenReturn(List.of(mine));

        List<?> responses = marketplaceService.listMyClasses();

        assertEquals(1, responses.size());
        verify(tutoringClassRepository).findByCreator_UserId(CLIENT_USER_ID);
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
class Report52F03MarketplaceTutorPart2ITTest {

    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    private ExpiredClassCleanupService cleanupService;

    @BeforeEach
    void setUpExpiredCleanupItFixture() {
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

    @Test
    @Tag("report52-it")
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
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(6)).createNativeQuery(sqlCaptor.capture());
        var statements = sqlCaptor.getAllValues();
        assertEquals(6, statements.size());
        assertTrue(statements.get(0).startsWith("DELETE FROM application_status_histories"));
        assertTrue(statements.get(1).startsWith("DELETE FROM tutor_applications"));
        assertTrue(statements.get(2).startsWith("DELETE FROM recommendation_logs"));
        assertTrue(statements.get(3).startsWith("DELETE FROM schedule_slots"));
        assertTrue(statements.get(4).startsWith("UPDATE support_tickets"));
        assertTrue(statements.get(5).startsWith("DELETE FROM tutoring_classes"));
        verify(query, atLeast(6)).setParameter("id", 701L);
        verify(query, atLeast(6)).executeUpdate();
    }
}
