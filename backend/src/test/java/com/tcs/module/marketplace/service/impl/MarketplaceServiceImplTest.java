package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.event.EscrowFunded;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonAttendance;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceImplTest {

    @Mock private PenaltyAccessService penaltyAccessService;

    private static final Long CLASS_ID = 5L;
    private static final Long ASSIGNMENT_ID = 7L;
    private static final Long CLASS_STUDENT_ID = 8L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;
    private static final Long CENTER_USER_ID = 33L;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private TutorCenterRepository tutorCenterRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractService contractService;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private EscrowService escrowService;

    @Mock
    private CenterRequestFeeService centerRequestFeeService;

    @Mock
    private TutoringClassRepository tutoringClassRepository;

    @Mock
    private ClassAssignmentRepository classAssignmentRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private ClassTerminationRequestRepository classTerminationRequestRepository;

    @Mock
    private TutorApplicationRepository tutorApplicationRepository;

    @Mock
    private FavoriteTutorRepository favoriteTutorRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonAttendanceRepository lessonAttendanceRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private CccdService cccdService;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    @Test
    void applyToClassRejectsVerifiedTutorWithoutWallet() {
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, new ApplyClassRequest()));

        assertEquals(
                "Bạn cần tạo ví trước khi tiếp tục. Vui lòng vào Ví của tôi để tạo ví.",
                ex.getMessage());
        verify(tutoringClassRepository, never()).findById(CLASS_ID);
        verify(tutorApplicationRepository, never()).save(any());
    }

    @Test
    void requestClassTerminationAutoSettlesPrivateClassByCompletedSessions() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setNumberOfSessions(10);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 10, 4);
        EscrowTransaction escrow = escrow(71L, new BigDecimal("1000000.00"));

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Gia sư cần dừng lớp sớm");
        request.setEffectiveDate(LocalDate.now().plusDays(2));
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                        CLASS_ID, com.tcs.module.marketplace.enums.ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(false);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(lessons);
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());
        when(classTerminationRequestRepository.save(any(ClassTerminationRequest.class)))
                .thenAnswer(invocation -> {
                    ClassTerminationRequest saved = invocation.getArgument(0);
                    saved.setTerminationId(31L);
                    saved.setCreatedAt(LocalDateTime.of(2026, 7, 16, 9, 0));
                    return saved;
                });

        ClassTerminationResponse response = marketplaceService.requestClassTermination(CLASS_ID, request);

        assertEquals(31L, response.getTerminationId());
        assertEquals(CLASS_ID, response.getClassId());
        assertEquals(ASSIGNMENT_ID, response.getAssignmentId());
        assertEquals(TUTOR_USER_ID, response.getRequestedByUserId());
        assertEquals(ClassTerminationStatus.COMPLETED, response.getStatus());
        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());

        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        ReleaseInstruction instruction = instructionCaptor.getValue();
        assertEquals(71L, instruction.escrowId());
        assertEquals(new BigDecimal("400000.00"), instruction.releaseToBeneficiary());
        assertEquals(new BigDecimal("600000.00"), instruction.refundToPayer());

        verify(tutoringClassRepository).save(tutoringClass);
        verify(classAssignmentRepository).save(assignment);
        verify(classTerminationRequestRepository).save(any(ClassTerminationRequest.class));
    }

    @Test
    void requestClassTerminationAutoSettlesCenterEnrollmentByPresentAttendances() {
        User centerUser = user(CLIENT_USER_ID);
        User enrolledUser = user(33L);
        TutoringClass tutoringClass = tutoringClass(centerUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setNumberOfSessions(10);
        ClassStudent classStudent = classStudent(tutoringClass, enrolledUser);
        Tutor tutor = tutor(user(TUTOR_USER_ID));
        List<Lesson> lessons = lessons(tutoringClass, tutor, 10, 0);
        EscrowTransaction escrow = escrow(81L, new BigDecimal("1000000.00"));

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setClassStudentId(CLASS_STUDENT_ID);
        request.setReason("Học viên cần dừng lớp trung tâm sớm");
        request.setEffectiveDate(LocalDate.now().plusDays(3));
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");

        when(authHelper.currentUserId()).thenReturn(enrolledUser.getUserId());
        when(userRepository.findById(enrolledUser.getUserId())).thenReturn(Optional.of(enrolledUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classStudentRepository.findById(CLASS_STUDENT_ID)).thenReturn(Optional.of(classStudent));
        when(classTerminationRequestRepository.existsByClassStudent_ClassStudentIdAndStatus(
                        CLASS_STUDENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(false);
        when(escrowTransactionRepository.findByClassStudent_ClassStudentId(CLASS_STUDENT_ID))
                .thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(lessons);
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(List.of(1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1010L)))
                .thenReturn(attendances(lessons, classStudent, 3));
        when(contractRepository.findByClassStudent_ClassStudentId(CLASS_STUDENT_ID)).thenReturn(Optional.empty());
        when(classTerminationRequestRepository.save(any(ClassTerminationRequest.class)))
                .thenAnswer(invocation -> {
                    ClassTerminationRequest saved = invocation.getArgument(0);
                    saved.setTerminationId(41L);
                    saved.setCreatedAt(LocalDateTime.of(2026, 7, 16, 10, 0));
                    return saved;
                });

        ClassTerminationResponse response = marketplaceService.requestClassTermination(CLASS_ID, request);

        assertEquals(41L, response.getTerminationId());
        assertEquals(CLASS_ID, response.getClassId());
        assertNull(response.getAssignmentId());
        assertEquals(CLASS_STUDENT_ID, response.getClassStudentId());
        assertEquals(enrolledUser.getUserId(), response.getRequestedByUserId());
        assertEquals(ClassTerminationStatus.COMPLETED, response.getStatus());
        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());
        assertEquals(ClassStudentStatus.DROPPED, classStudent.getStatus());

        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        ReleaseInstruction instruction = instructionCaptor.getValue();
        assertEquals(81L, instruction.escrowId());
        assertEquals(new BigDecimal("300000.00"), instruction.releaseToBeneficiary());
        assertEquals(new BigDecimal("700000.00"), instruction.refundToPayer());

        verify(tutoringClassRepository).save(tutoringClass);
        verify(classStudentRepository).save(classStudent);
        verify(classTerminationRequestRepository).save(any(ClassTerminationRequest.class));
    }

    @Test
    void requestClassTerminationWaitsForAdminWhenEscrowIsDisputed() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        EscrowTransaction escrow = escrow(91L, new BigDecimal("1000000.00"));
        escrow.setStatus(EscrowStatus.DISPUTED);

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Lớp đang có tranh chấp nên cần dừng");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(false);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(classTerminationRequestRepository.save(any(ClassTerminationRequest.class)))
                .thenAnswer(invocation -> {
                    ClassTerminationRequest saved = invocation.getArgument(0);
                    saved.setTerminationId(51L);
                    saved.setCreatedAt(LocalDateTime.of(2026, 7, 16, 11, 0));
                    return saved;
                });

        ClassTerminationResponse response = marketplaceService.requestClassTermination(CLASS_ID, request);

        assertEquals(51L, response.getTerminationId());
        assertEquals(ClassTerminationStatus.PENDING, response.getStatus());
        assertEquals(TutoringClassStatus.DISPUTED, tutoringClass.getStatus());
        assertEquals(ClassAssignmentStatus.ACTIVE, assignment.getStatus());

        verify(escrowService, never()).apply(any());
        verify(classAssignmentRepository, never()).save(any());
        verify(tutoringClassRepository).save(tutoringClass);
        verify(classTerminationRequestRepository).save(any(ClassTerminationRequest.class));
    }

    @Test
    void requestClassTerminationRejectsDuplicatePendingRequest() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, user(TUTOR_USER_ID));

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                        CLASS_ID, com.tcs.module.marketplace.enums.ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        verify(tutoringClassRepository, never()).save(any());
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    void requestClassTerminationRejectsDuplicateApprovedRequest() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, user(TUTOR_USER_ID));

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                        CLASS_ID, com.tcs.module.marketplace.enums.ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(false);
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.APPROVED))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> marketplaceService.requestClassTermination(CLASS_ID, request));

        assertEquals("Lớp học đã có yêu cầu chấm dứt sớm đang xử lý", exception.getMessage());
        verify(tutoringClassRepository, never()).save(any());
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    void requestClassTerminationRejectsCenterClassTutor() {
        User centerUser = user(CENTER_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(centerUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.CENTER);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setAssignmentId(ASSIGNMENT_ID);
        request.setReason("Gia sư muốn dừng lớp trung tâm");

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

        assertThrows(ForbiddenException.class, () -> marketplaceService.requestClassTermination(CLASS_ID, request));

        verify(escrowService, never()).apply(any());
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    void requestClassTerminationByCenterCancelsWholeCenterClassAndSettlesEachStudentEscrow() {
        User centerUser = user(CENTER_USER_ID);
        User clientOne = user(101L);
        User clientTwo = user(102L);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(centerUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setNumberOfSessions(5);
        TutorCenter center = new TutorCenter();
        center.setUser(centerUser);

        ClassStudent firstStudent = classStudent(tutoringClass, clientOne);
        firstStudent.setClassStudentId(81L);
        firstStudent.setStudentName("Nguyễn Minh An");
        firstStudent.setNotes(RefundPayoutInfoCodec.appendToReason(
                "Payout đã lưu",
                new RefundPayoutInfo("TPBank", "0123456789", "NGUYEN MINH AN")));
        ClassStudent secondStudent = classStudent(tutoringClass, clientTwo);
        secondStudent.setClassStudentId(82L);
        secondStudent.setStudentName("Trần Gia Bảo");
        secondStudent.setNotes(RefundPayoutInfoCodec.appendToReason(
                "Payout đã lưu",
                new RefundPayoutInfo("VPBank", "9876543210", "TRAN GIA BAO")));

        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 5, 2);
        List<LessonAttendance> attendances = new java.util.ArrayList<>();
        attendances.addAll(attendances(lessons, firstStudent, 2));
        attendances.addAll(attendances(lessons, secondStudent, 2));
        EscrowTransaction firstEscrow = escrow(201L, new BigDecimal("500000.00"));
        firstEscrow.setClassStudent(firstStudent);
        EscrowTransaction secondEscrow = escrow(202L, new BigDecimal("500000.00"));
        secondEscrow.setClassStudent(secondStudent);

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Trung tâm phải đóng lớp vì không đủ điều kiện vận hành");

        when(authHelper.currentUserId()).thenReturn(CENTER_USER_ID);
        when(userRepository.findById(CENTER_USER_ID)).thenReturn(Optional.of(centerUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorCenterRepository.findByUser_UserId(CENTER_USER_ID)).thenReturn(Optional.of(center));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(CLASS_ID, ClassStudentStatus.ENROLLED))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(escrowTransactionRepository.findByClassStudent_ClassStudentId(81L)).thenReturn(Optional.of(firstEscrow));
        when(escrowTransactionRepository.findByClassStudent_ClassStudentId(82L)).thenReturn(Optional.of(secondEscrow));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(lessons);
        when(lessonAttendanceRepository.findByLesson_LessonIdIn(any())).thenReturn(attendances);
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(contractRepository.findByClassStudent_ClassStudentId(any())).thenReturn(Optional.empty());
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());
        when(classTerminationRequestRepository.save(any(ClassTerminationRequest.class)))
                .thenAnswer(invocation -> {
                    ClassTerminationRequest saved = invocation.getArgument(0);
                    saved.setTerminationId(saved.getClassStudent().getClassStudentId());
                    saved.setCreatedAt(LocalDateTime.of(2026, 8, 23, 10, 0));
                    return saved;
                });

        ClassTerminationResponse response = marketplaceService.requestClassTermination(CLASS_ID, request);

        assertEquals(ClassTerminationStatus.COMPLETED, response.getStatus());
        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());
        assertEquals(ClassStudentStatus.DROPPED, firstStudent.getStatus());
        assertEquals(ClassStudentStatus.DROPPED, secondStudent.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());

        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService, times(2)).apply(instructionCaptor.capture());
        assertEquals(new BigDecimal("200000.00"), instructionCaptor.getAllValues().get(0).releaseToBeneficiary());
        assertEquals(new BigDecimal("300000.00"), instructionCaptor.getAllValues().get(0).refundToPayer());
        assertEquals(new BigDecimal("200000.00"), instructionCaptor.getAllValues().get(1).releaseToBeneficiary());
        assertEquals(new BigDecimal("300000.00"), instructionCaptor.getAllValues().get(1).refundToPayer());
    }

    @Test
    void requestClassTerminationRejectsNonParticipant() {
        User clientUser = user(CLIENT_USER_ID);
        User outsider = user(99L);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, user(TUTOR_USER_ID));

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");

        when(authHelper.currentUserId()).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(outsider));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(classStudentRepository.findByTutoringClass_ClassIdAndStatus(
                        CLASS_ID, com.tcs.module.marketplace.enums.ClassStudentStatus.ENROLLED))
                .thenReturn(List.of());

        assertThrows(ForbiddenException.class, () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    void requestClassTerminationRejectsClassThatIsNotInProgress() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");
        request.setBankName("TPBank");
        request.setAccountNo("0123456789");
        request.setAccountHolderName("Nguyen Van A");

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

        assertThrows(BusinessException.class, () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    void chooseApplicantReplacesPrivateClassDealRatesWithTutorProposal() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);
        tutoringClass.setNumberOfSessions(4);
        tutoringClass.setDetailsJson("""
                {"scheduleMode":"WEEKLY","repeatEveryWeeks":1,"subjectIds":["1","2","3"],
                 "subjectFees":{"1":"120000","2":"150000","3":"180000"},
                 "slots":[
                    {"subjectId":"1","day":"T2","start":"18:00","end":"19:00"},
                    {"subjectId":"2","day":"T3","start":"18:00","end":"19:00"},
                    {"subjectId":"3","day":"T4","start":"18:00","end":"19:00"}
                 ]}
                """);
        Tutor tutor = tutor(tutorUser);
        TutorApplication chosen = new TutorApplication();
        chosen.setApplicationId(55L);
        chosen.setTutoringClass(tutoringClass);
        chosen.setTutor(tutor);
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
        when(classAssignmentRepository.save(any(ClassAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tutoringClassRepository.save(any(TutoringClass.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        marketplaceService.chooseApplicant(CLASS_ID, 55L);

        assertEquals(BigDecimal.valueOf(140000), tutoringClass.getTuitionFee());
        Map<String, Object> parsed = null;
        try {
            parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(tutoringClass.getDetailsJson(), Map.class);
        } catch (Exception ignored) {
        }
        assertEquals("{1=140000}", parsed != null ? parsed.get("subjectFees").toString() : null);
        assertEquals("[1]", parsed != null ? parsed.get("subjectIds").toString() : null);
    }

    @Test
    void confirmClassCompletionReleasesEscrowAndCenterRequestFeeWhenClientAlreadyReviewed() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.PRIVATE);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 2, 2);
        lessons.get(0).setLessonDate(LocalDate.now().minusDays(7));
        lessons.get(1).setLessonDate(LocalDate.now());
        EscrowTransaction escrow = escrow(91L, new BigDecimal("100000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(lessonRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(lessons);
        when(contractService.hasClientReviewedClass(CLASS_ID)).thenReturn(true);
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        String message = marketplaceService.confirmClassCompletion(CLASS_ID);

        assertEquals("Lớp đã hoàn thành. Học phí ký quỹ đã được giải ngân cho gia sư.", message);
        assertEquals(TutoringClassStatus.COMPLETED, tutoringClass.getStatus());
        verify(escrowService).apply(any(ReleaseInstruction.class));
        verify(centerRequestFeeService).releaseForFulfilledAssignment(eq(ASSIGNMENT_ID), anyString());
        verify(tutoringClassRepository).save(tutoringClass);
    }

    @Test
    void completeClassAfterClientReviewClosesPrivateClassWhenTutorAlreadyConfirmed() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.PRIVATE);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        assignment.setTutorCompletedAt(LocalDateTime.now().minusMinutes(10));
        EscrowTransaction escrow = escrow(92L, new BigDecimal("100000.00"));

        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(contractRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID)).thenReturn(Optional.empty());

        marketplaceService.completeClassAfterClientReview(CLASS_ID);

        assertEquals(TutoringClassStatus.COMPLETED, tutoringClass.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(assignment.getClientCompletedAt());
        verify(classAssignmentRepository).save(assignment);
        verify(escrowService).apply(any(ReleaseInstruction.class));
        verify(tutoringClassRepository).save(tutoringClass);
    }

    @Test
    void checkOutLessonAutoReleasesPrivateFirstMonthEscrowWhenNoBlockingIssue() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStartDate(LocalDate.now().minusMonths(1).minusDays(3));
        tutoringClass.setEndDate(LocalDate.now().plusMonths(2));
        configurePrivateHourlyDeal(tutoringClass, new BigDecimal("50000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 4, 3);
        lessons.get(0).setLessonDate(tutoringClass.getStartDate().plusDays(7));
        lessons.get(1).setLessonDate(tutoringClass.getStartDate().plusDays(14));
        lessons.get(2).setLessonDate(tutoringClass.getStartDate().plusDays(21));
        lessons.get(3).setLessonDate(LocalDate.now());
        lessons.get(3).setTutorCheckInAt(LocalDateTime.now().minusMinutes(40));
        EscrowTransaction escrow = escrow(93L, new BigDecimal("100000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(lessonRepository.findById(lessons.get(3).getLessonId())).thenReturn(Optional.of(lessons.get(3)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(lessons);
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(false);
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.APPROVED))
                .thenReturn(false);

        marketplaceService.checkOutLesson(lessons.get(3).getLessonId());

        assertEquals(AttendanceStatus.COMPLETED, lessons.get(3).getAttendanceStatus());
        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        ReleaseInstruction instruction = instructionCaptor.getValue();
        assertEquals(93L, instruction.escrowId());
        assertEquals(new BigDecimal("100000.00"), instruction.releaseToBeneficiary());
        assertEquals(BigDecimal.ZERO, instruction.refundToPayer());
        assertEquals(
                "Đã hoàn tất đủ buổi của tháng đầu, giải ngân khoản ký quỹ cho gia sư.",
                instruction.reason());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(assignment.getTutor().getUser()),
                eq(NotificationType.CLASS),
                eq("MARKETPLACE_CLASS_EVENT"),
                any(),
                eq("Ký quỹ tháng đầu đã được giải ngân"),
                anyString(),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(clientUser),
                eq(NotificationType.CLASS),
                eq("MARKETPLACE_CLASS_EVENT"),
                any(),
                eq("Ký quỹ tháng đầu đã được giải ngân"),
                anyString(),
                eq("TUTORING_CLASS"),
                eq(CLASS_ID));
    }

    @Test
    void checkOutLessonAutoReleasesPrivateFirstMonthEscrowBeforeMonthlyAnniversaryWhenCompletedValueCoversEscrow() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStartDate(LocalDate.now().minusDays(10));
        tutoringClass.setEndDate(LocalDate.now().plusMonths(2));
        configurePrivateHourlyDeal(tutoringClass, new BigDecimal("50000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 3, 2);
        lessons.get(0).setLessonDate(LocalDate.now().minusDays(7));
        lessons.get(1).setLessonDate(LocalDate.now().minusDays(3));
        lessons.get(2).setLessonDate(LocalDate.now());
        lessons.get(2).setTutorCheckInAt(LocalDateTime.now().minusMinutes(40));
        EscrowTransaction escrow = escrow(95L, new BigDecimal("100000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(lessonRepository.findById(lessons.get(2).getLessonId())).thenReturn(Optional.of(lessons.get(2)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(lessons);

        marketplaceService.checkOutLesson(lessons.get(2).getLessonId());

        assertEquals(AttendanceStatus.COMPLETED, lessons.get(2).getAttendanceStatus());
        verify(escrowService).apply(any(ReleaseInstruction.class));
    }

    @Test
    void checkOutLessonDoesNotReleasePrivateFirstMonthEscrowWhenCompletedValueIsBelowPaidEscrow() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStartDate(LocalDate.now().minusDays(10));
        tutoringClass.setEndDate(LocalDate.now().plusMonths(2));
        configurePrivateHourlyDeal(tutoringClass, new BigDecimal("50000.00"));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 3, 2);
        lessons.get(0).setLessonDate(LocalDate.now().minusDays(7));
        lessons.get(1).setLessonDate(LocalDate.now().minusDays(3));
        lessons.get(2).setLessonDate(LocalDate.now());
        lessons.get(2).setTutorCheckInAt(LocalDateTime.now().minusMinutes(40));
        EscrowTransaction escrow = escrow(96L, new BigDecimal("200000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(lessonRepository.findById(lessons.get(2).getLessonId())).thenReturn(Optional.of(lessons.get(2)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(lessonRepository.findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(CLASS_ID))
                .thenReturn(lessons);

        marketplaceService.checkOutLesson(lessons.get(2).getLessonId());

        assertEquals(AttendanceStatus.COMPLETED, lessons.get(2).getAttendanceStatus());
        verify(escrowService, never()).apply(any());
    }

    @Test
    void checkOutLessonKeepsShortPrivateClassOnManualReviewCompletionFlow() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStartDate(LocalDate.now().minusDays(10));
        tutoringClass.setEndDate(tutoringClass.getStartDate().plusMonths(1));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 3, 2);
        lessons.get(0).setLessonDate(LocalDate.now().minusDays(7));
        lessons.get(1).setLessonDate(LocalDate.now().minusDays(3));
        lessons.get(2).setLessonDate(LocalDate.now());
        lessons.get(2).setTutorCheckInAt(LocalDateTime.now().minusMinutes(40));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(lessonRepository.findById(lessons.get(2).getLessonId())).thenReturn(Optional.of(lessons.get(2)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        marketplaceService.checkOutLesson(lessons.get(2).getLessonId());

        assertEquals(AttendanceStatus.COMPLETED, lessons.get(2).getAttendanceStatus());
        verify(escrowService, never()).apply(any());
        verify(classAssignmentRepository, never())
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(any(), any());
    }

    @Test
    void checkOutLessonDoesNotReleasePrivateFirstMonthEscrowWhenClassHasPendingReport() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStartDate(LocalDate.now().minusDays(10));
        tutoringClass.setEndDate(LocalDate.now().plusMonths(2));
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);
        List<Lesson> lessons = lessons(tutoringClass, assignment.getTutor(), 3, 2);
        lessons.get(0).setLessonDate(LocalDate.now().minusDays(7));
        lessons.get(1).setLessonDate(LocalDate.now().minusDays(3));
        lessons.get(2).setLessonDate(LocalDate.now());
        lessons.get(2).setTutorCheckInAt(LocalDateTime.now().minusMinutes(40));
        EscrowTransaction escrow = escrow(94L, new BigDecimal("100000.00"));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(assignment.getTutor()));
        when(lessonRepository.findById(lessons.get(2).getLessonId())).thenReturn(Optional.of(lessons.get(2)));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classAssignmentRepository.findFirstByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(escrowTransactionRepository.findByAssignment_AssignmentId(ASSIGNMENT_ID))
                .thenReturn(Optional.of(escrow));
        when(reportRepository.existsByTargetTypeAndTargetIdAndStatus(
                ReportTargetType.CLASS,
                CLASS_ID,
                ReportStatus.PENDING)).thenReturn(true);

        marketplaceService.checkOutLesson(lessons.get(2).getLessonId());

        assertEquals(AttendanceStatus.COMPLETED, lessons.get(2).getAttendanceStatus());
        verify(escrowService, never()).apply(any());
        verify(notificationDispatchService, never()).notifyUserFromTemplate(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onEscrowFundedEnrollsStudentAndNotifiesClient() {
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
                org.mockito.ArgumentMatchers.eq(clientUser),
                org.mockito.ArgumentMatchers.eq(NotificationType.CLASS),
                org.mockito.ArgumentMatchers.eq("MARKETPLACE_CLASS_EVENT"),
                any(),
                org.mockito.ArgumentMatchers.eq("Ghi danh thành công"),
                org.mockito.ArgumentMatchers.eq(
                        "Học viên test đã được ghi danh thành công vào lớp \"Lớp toán\" sau khi hệ thống xác nhận thanh toán."),
                org.mockito.ArgumentMatchers.eq("TUTORING_CLASS"),
                org.mockito.ArgumentMatchers.eq(CLASS_ID));
    }

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.com");
        return user;
    }

    private TutoringClass tutoringClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(creator);
        tutoringClass.setTitle("Lớp toán");
        tutoringClass.setDescription("Lớp toán test");
        tutoringClass.setStatus(status);
        return tutoringClass;
    }

    private ClassAssignment assignment(TutoringClass tutoringClass, User tutorUser) {
        Tutor tutor = tutor(tutorUser);

        TutorApplication application = new TutorApplication();
        application.setApplicationId(55L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);

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
        return tutor;
    }

    private ClassStudent classStudent(TutoringClass tutoringClass, User enrolledUser) {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(CLASS_STUDENT_ID);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(enrolledUser);
        classStudent.setStudentName("Học viên test");
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
        slot.setStartTime(java.time.LocalTime.of(18, 0));
        slot.setEndTime(java.time.LocalTime.of(19, 0));
        return java.util.stream.IntStream.rangeClosed(1, total)
                .mapToObj(sequence -> {
                    Lesson lesson = new Lesson();
                    lesson.setLessonId(1000L + sequence);
                    lesson.setTutoringClass(tutoringClass);
                    lesson.setTutor(tutor);
                    lesson.setSlot(slot);
                    lesson.setSequenceNo(sequence);
                    lesson.setAttendanceStatus(
                            sequence <= completed ? AttendanceStatus.COMPLETED : AttendanceStatus.PENDING);
                    return lesson;
                })
                .toList();
    }

    private List<LessonAttendance> attendances(List<Lesson> lessons, ClassStudent classStudent, int present) {
        return java.util.stream.IntStream.range(0, lessons.size())
                .mapToObj(index -> {
                    LessonAttendance attendance = new LessonAttendance();
                    attendance.setLesson(lessons.get(index));
                    attendance.setClassStudent(classStudent);
                    attendance.setStatus(index < present ? LessonAttendanceStatus.PRESENT : LessonAttendanceStatus.ABSENT);
                    return attendance;
                })
                .toList();
    }

    // =====================================================================================
    //  Bo sung theo Report_5.1_UnitTest: applyToClass UTCID08, chooseApplicant UTCID04,
    //  createClassRequest UTCID06, requestClassTermination UTCID07..UTCID10.
    // =====================================================================================


    /** Don ung tuyen kem hoc phi de xuat hop le (>= 50.000d/gio). */
    private ApplyClassRequest applyWithRate() {
        ApplyClassRequest request = new ApplyClassRequest();
        request.setProposedRate(new BigDecimal("150000"));
        return request;
    }

    /** Thong tin CCCD day du cua nguoi tao lop. */
    private CccdInfoDto cccdInfo() {
        return CccdInfoDto.builder()
                .fullName("Client Test")
                .cccdNumber("012345678901")
                .dateOfBirth("01/01/2000")
                .permanentAddress("Hà Nội")
                .complete(true)
                .build();
    }
    /** MarketplaceServiceImpl doc detailsJson bang ObjectMapper that -> can ban that de kiem thu trung lich. */
    private void useRealObjectMapper() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                marketplaceService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
    }

    /** Lop co dung 1 buoi CUSTOM vao ngay mai 18:00-20:00. */
    private TutoringClass classWithTomorrowSlot(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = tutoringClass(creator, status);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        tutoringClass.setStartDate(tomorrow);
        tutoringClass.setEndDate(tomorrow);
        tutoringClass.setDetailsJson("""
                {
                  "scheduleMode": "CUSTOM",
                  "slots": [{"date": "%s", "start": "18:00", "end": "20:00"}]
                }
                """.formatted(tomorrow));
        return tutoringClass;
    }

    /** Buoi day san co cua gia su, cung ngay mai va chong gio 18:30-20:30. */
    private Lesson busyLessonTomorrow(Tutor tutor) {
        TutoringClass otherClass = new TutoringClass();
        otherClass.setClassId(999L);
        otherClass.setTitle("Lớp lý");

        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(77L);
        slot.setStartTime(java.time.LocalTime.of(18, 30));
        slot.setEndTime(java.time.LocalTime.of(20, 30));

        Lesson lesson = new Lesson();
        lesson.setLessonId(7001L);
        lesson.setTutoringClass(otherClass);
        lesson.setTutor(tutor);
        lesson.setSlot(slot);
        lesson.setLessonDate(LocalDate.now().plusDays(1));
        return lesson;
    }

    /** Sheet applyToClass - UTCID08 (A): gia su da co buoi day chong gio voi lich lop. */
    @Test
    void applyToClassRejectsTutorWithClashingSchedule() {
        useRealObjectMapper();
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        tutor.setVerificationStatus(ProfileVerificationStatus.VERIFIED);
        TutoringClass tutoringClass = classWithTomorrowSlot(clientUser, TutoringClassStatus.OPEN);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(walletRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(new Wallet()));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.findFirstByTutoringClass_ClassIdAndTutor_TutorId(CLASS_ID, 44L))
                .thenReturn(Optional.empty());
        when(lessonRepository.findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(CLIENT_USER_ID))
                .thenReturn(List.of());
        when(lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(44L))
                .thenReturn(List.of(busyLessonTomorrow(tutor)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.applyToClass(CLASS_ID, applyWithRate()));

        assertTrue(ex.getMessage().startsWith("Bạn đã có lịch dạy "),
                "Phai bao trung lich day: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Lớp lý"),
                "Phai neu ro lop bi trung: " + ex.getMessage());
        verify(tutorApplicationRepository, never()).save(any());
    }

    /** Sheet chooseApplicant - UTCID04 (A): gia su duoc chon da ban day trung khung gio. */
    @Test
    void chooseApplicantRejectsTutorWithClashingSchedule() {
        useRealObjectMapper();
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        Tutor tutor = tutor(tutorUser);
        TutoringClass tutoringClass = classWithTomorrowSlot(clientUser, TutoringClassStatus.OPEN);

        TutorApplication chosen = new TutorApplication();
        chosen.setApplicationId(55L);
        chosen.setTutoringClass(tutoringClass);
        chosen.setTutor(tutor);
        chosen.setStatus(TutorApplicationStatus.SUBMITTED);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(cccdService.getByUserId(CLIENT_USER_ID)).thenReturn(cccdInfo());
        when(tutorApplicationRepository.findById(55L)).thenReturn(Optional.of(chosen));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(tutorApplicationRepository.findByTutoringClass_ClassId(CLASS_ID)).thenReturn(List.of(chosen));
        when(lessonRepository.findByTutoringClass_Creator_UserIdOrderByLessonDateAscSequenceNoAsc(CLIENT_USER_ID))
                .thenReturn(List.of());
        when(lessonRepository.findByTutor_TutorIdOrderByLessonDateAscSequenceNoAsc(44L))
                .thenReturn(List.of(busyLessonTomorrow(tutor)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.chooseApplicant(CLASS_ID, 55L));

        assertTrue(ex.getMessage().startsWith("Gia sư này đã bận dạy "),
                "Phai bao gia su ban: " + ex.getMessage());
        verify(classAssignmentRepository, never()).save(any());
    }

    /** Sheet createClassRequest - UTCID06 (A): centerId khong khop trung tam nao. */
    @Test
    void createClassRequestRejectsUnknownCenter() {
        User clientUser = user(CLIENT_USER_ID);

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(clientRepository.findByUser_UserId(CLIENT_USER_ID))
                .thenReturn(Optional.of(new com.tcs.module.profile.entity.Client()));
        when(tutorCenterRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> marketplaceService.createClassRequest(404L,
                        new com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest()));
        assertEquals("Không tìm thấy trung tâm", ex.getMessage());
    }

    /** Sheet requestClassTermination - UTCID07 (A): request = null. */
    @Test
    void requestClassTerminationRejectsNullRequest() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.requestClassTermination(CLASS_ID, null));
        assertEquals("Thiếu thông tin yêu cầu chấm dứt lớp", ex.getMessage());
        verify(tutoringClassRepository, never()).findById(any());
    }

    /** Sheet requestClassTermination - UTCID08 (A): reason rong hoac chi khoang trang. */
    @Test
    void requestClassTerminationRejectsBlankReason() {
        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        assertEquals("Lý do chấm dứt lớp là bắt buộc", ex.getMessage());
        verify(tutoringClassRepository, never()).findById(any());
    }

    /** Sheet requestClassTermination - UTCID09 (B): effectiveDate = hom qua (duoi can duoi). */
    @Test
    void requestClassTerminationRejectsPastEffectiveDate() {
        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Gia sư cần dừng lớp sớm");
        request.setEffectiveDate(LocalDate.now().minusDays(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        assertEquals("Ngày hiệu lực không được nằm trong quá khứ", ex.getMessage());
        verify(tutoringClassRepository, never()).findById(any());
    }

    /**
     * Sheet requestClassTermination - UTCID10 (B): effectiveDate = hom nay (dung can duoi) duoc chap nhan.
     *
     * <p>Kiem chung bang cach cho luong di QUA buoc kiem tra ngay: lop o trang thai khong cho
     * cham dut som nen loi nem ra la loi trang thai, khong phai loi ngay hieu luc.</p>
     */
    @Test
    void requestClassTerminationAcceptsTodayAsEffectiveDate() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.COMPLETED);

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Gia sư cần dừng lớp sớm");
        request.setEffectiveDate(LocalDate.now());

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        assertEquals("Chỉ lớp đang diễn ra mới có thể yêu cầu chấm dứt sớm", ex.getMessage(),
                "Ngay hieu luc = hom nay phai qua duoc buoc kiem tra ngay");
    }
}
