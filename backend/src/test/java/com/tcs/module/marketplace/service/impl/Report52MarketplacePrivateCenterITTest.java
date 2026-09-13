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
