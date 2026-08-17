package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.event.EscrowFunded;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.CenterRequestFeeService;
import com.tcs.module.finance.service.EscrowService;
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
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.dto.CccdInfoDto;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.ClientRepository;
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

    @Mock
    private AuthHelper authHelper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractService contractService;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

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

        assertEquals("Lớp đã hoàn thành. Học phí escrow đã được giải ngân cho gia sư.", message);
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

    private List<Lesson> lessons(TutoringClass tutoringClass, Tutor tutor, int total, int completed) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setSlotId(19L);
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
}
