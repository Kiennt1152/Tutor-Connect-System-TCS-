package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.identity.repository.EmailOtpRepository;
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
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
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
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.module.profile.service.CccdService;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52MarketplacePrivateCenterITTest {

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

    @Test
    @Tag("report52-it")
    void IT_DSP_009_RejectDuplicateEarlyTerminationForPendingOrApprovedRequest() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, user(TUTOR_USER_ID));
        CreateClassTerminationRequest request = terminationRequest();

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(List.of(assignment));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                ASSIGNMENT_ID, ClassTerminationStatus.PENDING)).thenReturn(true);

        BusinessException pendingException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        assertEquals("Lớp học đã có yêu cầu chấm dứt sớm đang xử lý", pendingException.getMessage());

        // The duplicate guard must cover both PENDING and APPROVED requests.
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                ASSIGNMENT_ID, ClassTerminationStatus.PENDING)).thenReturn(false);
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                ASSIGNMENT_ID, ClassTerminationStatus.APPROVED)).thenReturn(true);

        BusinessException approvedException = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        assertEquals("Lớp học đã có yêu cầu chấm dứt sớm đang xử lý", approvedException.getMessage());
        verify(classTerminationRequestRepository).existsByAssignment_AssignmentIdAndStatus(
                ASSIGNMENT_ID, ClassTerminationStatus.APPROVED);

        verify(tutoringClassRepository, never()).save(any());
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_DSP_020_TutorPrivateTerminationUsesClientPayoutStoredOnAssignmentTerms() {
        User tutorUser = user(TUTOR_USER_ID);
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setNumberOfSessions(5);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setTermsB("""
                Thông tin nhận hoàn tiền:
                - Tên chủ tài khoản: Nguyễn Thu Hà
                - Ngân hàng: TPBank
                - Số tài khoản: 0123456789
                """);
        EscrowTransaction escrow = escrow(95L, new BigDecimal("500000.00"));
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 5, 2);
        CreateClassTerminationRequest request = terminationRequest();
        request.setAssignmentId(ASSIGNMENT_ID);
        request.setBankName("");
        request.setAccountNo("");
        request.setAccountHolderName("");

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                ASSIGNMENT_ID, ClassTerminationStatus.PENDING)).thenReturn(false);
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                ASSIGNMENT_ID, ClassTerminationStatus.APPROVED)).thenReturn(false);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(lessons);
        when(classTerminationRequestRepository.save(any(ClassTerminationRequest.class))).thenAnswer(invocation -> {
            ClassTerminationRequest saved = invocation.getArgument(0);
            saved.setTerminationId(901L);
            return saved;
        });

        ClassTerminationResponse response = marketplaceService.requestClassTermination(CLASS_ID, request);

        assertEquals(ClassTerminationStatus.COMPLETED, response.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());
        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());

        ArgumentCaptor<ClassTerminationRequest> terminationCaptor =
                ArgumentCaptor.forClass(ClassTerminationRequest.class);
        verify(classTerminationRequestRepository).save(terminationCaptor.capture());
        assertTrue(terminationCaptor.getValue().getReason().contains("Ngân hàng: TPBank"));
        assertTrue(terminationCaptor.getValue().getReason().contains("Số tài khoản: 0123456789"));

        ArgumentCaptor<ReleaseInstruction> instructionCaptor =
                ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        assertEquals(new BigDecimal("200000.00"), instructionCaptor.getValue().releaseToBeneficiary());
        assertEquals(new BigDecimal("300000.00"), instructionCaptor.getValue().refundToPayer());
    }

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

    @Test
    @Tag("report52-it")
    void IT_LSN_016_LongPrivateClassAutoReleasesFirstMonthEscrowWhenCompletedLessonValueCoversPaidEscrow() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setStartDate(LocalDate.now().minusDays(10));
        tutoringClass.setEndDate(LocalDate.now().plusMonths(2));
        configurePrivateHourlyDeal(tutoringClass, new BigDecimal("50000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 3, 2);
        lessons.get(2).setLessonDate(LocalDate.now());
        lessons.get(2).setTutorCheckInAt(LocalDateTime.now().minusMinutes(40));
        EscrowTransaction escrow = escrow(95L, new BigDecimal("100000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(lessonRepository.findById(lessons.get(2).getLessonId())).thenReturn(Optional.of(lessons.get(2)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                CLASS_ID, ClassAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID)).thenReturn(lessons);

        marketplaceService.checkOutLesson(lessons.get(2).getLessonId());

        assertEquals(AttendanceStatus.COMPLETED, lessons.get(2).getAttendanceStatus());
        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        assertEquals(95L, instructionCaptor.getValue().escrowId());
        assertEquals(new BigDecimal("100000.00"), instructionCaptor.getValue().releaseToBeneficiary());
    }

    @Test
    @Tag("report52-it")
    void IT_CFR_006_BlockAnonymousCenterRequestBeforeCreatingFeeHold() {
        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.createClassRequest(77L, classRequestCreateRequest()));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(classRequestStore, never()).create(any(), any(), any(), any(), any(), any());
        verify(centerRequestFeeService, never()).createPayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Tag("report52-it")
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

    @Test
    void SUPPORT_CFR_PreventClientFromCancellingAnotherClientsCenterRequestFeeHold() {
        User otherClient = user(333L);
        ClassRequestStore.ClassRequestData data = new ClassRequestStore.ClassRequestData(
                "REQ-CFR-008",
                CLIENT_USER_ID,
                77L,
                null,
                "Nhờ trung tâm tìm gia sư Toán",
                new BigDecimal("500000.00"),
                ClassRequestStore.STATUS_PAYMENT_PENDING,
                null,
                LocalDateTime.now().toString(),
                "{}",
                List.of(),
                null);

        when(authHelper.currentUserId()).thenReturn(otherClient.getUserId());
        when(userRepository.findById(otherClient.getUserId())).thenReturn(Optional.of(otherClient));
        when(classRequestStore.find("REQ-CFR-008")).thenReturn(Optional.of(data));

        ForbiddenException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ForbiddenException.class,
                () -> marketplaceService.cancelClassRequest("REQ-CFR-008"));

        assertEquals("Bạn không có quyền hủy yêu cầu này", exception.getMessage());
        verify(centerRequestFeeService, never()).cancelUnpaid(anyString());
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
