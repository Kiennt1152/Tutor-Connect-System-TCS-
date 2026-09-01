package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.request.AppealDisputeRequest;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.request.ResolveDisputeRequest;
import com.tcs.module.finance.dto.request.SubmitDisputeEvidenceRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.RefundRequest;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.ClassIssueRequestedAction;
import com.tcs.module.finance.enums.ClassIssueType;
import com.tcs.module.finance.enums.DisputeResolutionAction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisputeServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.tcs.module.messaging.service.NotificationDispatchService notificationDispatchService;

    @Mock
    private PlatformAdminRepository platformAdminRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TutoringClassRepository tutoringClassRepository;

    @Mock
    private ClassAssignmentRepository classAssignmentRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private ClassTerminationRequestRepository classTerminationRequestRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonAttendanceRepository lessonAttendanceRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private EscrowService escrowService;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    /** Sheet createDispute - UTCID01 (N): escrow thuộc về người mở tranh chấp -> tạo Report + Dispute và tạm giữ escrow */
    @Test
    void createDisputeCreatesReportDisputeAndHoldsEscrow() {
        User reporter = new User();
        reporter.setUserId(USER_ID);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(12L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(reporter);

        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setClassStudent(classStudent);
        Report savedReport = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute savedDispute = dispute(savedReport, escrow, 31L, DisputeStatus.OPEN);

        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTargetType(ReportTargetType.CLASS);
        request.setTargetId(99L);
        request.setCategory(ReportCategory.FRAUD);
        request.setDescription("Có gian lận");
        request.setEvidenceUrls("https://example.com/evidence");
        request.setEscrowId(11L);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(escrowTransactionRepository.findById(11L)).thenReturn(Optional.of(escrow));
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
        when(escrowService.holdForDispute(11L, "Có gian lận")).thenReturn(escrow);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(savedDispute);

        DisputeResponse response = disputeService.createDispute(request);

        assertEquals(31L, response.getDisputeId());
        assertEquals(21L, response.getReportId());
        assertEquals(11L, response.getEscrowId());
        assertEquals(DisputeStatus.OPEN, response.getDisputeStatus());
        assertEquals(EscrowStatus.FUNDED, response.getEscrowStatus());
        assertEquals(ReportTargetType.CLASS, response.getTargetType());
        verify(escrowService).holdForDispute(11L, "Có gian lận");
        verify(disputeRepository).save(any(Dispute.class));
    }

    /** Bổ sung ngoài các UTCID của sheet createDispute: escrow đã có tranh chấp đang mở -> chặn tạo trùng */
    @Test
    void createDisputeRejectsWhenEscrowAlreadyHasActiveDispute() {
        User reporter = new User();
        reporter.setUserId(USER_ID);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(12L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(reporter);

        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setClassStudent(classStudent);
        Report savedReport = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");

        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTargetType(ReportTargetType.CLASS);
        request.setTargetId(99L);
        request.setCategory(ReportCategory.FRAUD);
        request.setDescription("Có gian lận");
        request.setEscrowId(11L);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(escrowTransactionRepository.findById(11L)).thenReturn(Optional.of(escrow));
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
        when(disputeRepository.existsByEscrowTransaction_EscrowIdAndStatusNot(
                        11L, DisputeStatus.RESOLVED))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> disputeService.createDispute(request));

        assertEquals(
                "Khoản ký quỹ này đang có tranh chấp chưa xử lý. Vui lòng theo dõi hồ sơ hiện có.",
                exception.getMessage());
        verify(escrowService, never()).holdForDispute(any(), any());
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    /** Sheet createClassIssue - UTCID01 (N): requestedAction không leo thang -> chỉ tạo báo cáo, không giữ escrow */
    @Test
    void createClassIssueCreatesReportWithoutHoldingEscrow() {
        User reporter = new User();
        reporter.setUserId(USER_ID);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setCreator(reporter);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);

        Report savedReport = report(
                21L,
                reporter,
                ReportTargetType.CLASS,
                99L,
                ReportCategory.SPAM,
                "[UC-29] Báo cáo sự cố lớp học");

        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setIssueType(ClassIssueType.TUTOR_ABSENT);
        request.setRequestedAction(ClassIssueRequestedAction.RESCHEDULE);
        request.setLessonRef("Buổi 3");
        request.setOccurredAt(LocalDate.of(2026, 7, 18));
        request.setDescription("Gia sư không tham gia buổi học theo lịch đã hẹn");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(tutoringClassRepository.findById(99L)).thenReturn(Optional.of(tutoringClass));
        when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                ReportTargetType.CLASS,
                99L,
                ReportStatus.PENDING))
                .thenReturn(List.of());
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        DisputeResponse response = disputeService.createClassIssue(request);

        assertFalse(response.getEscalatedToDispute());
        assertNull(response.getDisputeId());
        assertEquals(21L, response.getReportId());
        assertNull(response.getEscrowId());
        assertEquals(ReportTargetType.CLASS, response.getTargetType());
        verify(escrowService, never()).holdForDispute(any(), any());
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    /** Sheet createClassIssue - UTCID14 (A): gia sư lớp trung tâm yêu cầu hành động leo thang */
    @Test
    void createClassIssueRejectsCenterTutorFinancialDisputeAction() {
        User tutorUser = new User();
        tutorUser.setUserId(USER_ID);
        User centerUser = new User();
        centerUser.setUserId(700L);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setCreator(centerUser);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);

        Tutor tutor = new Tutor();
        tutor.setTutorId(44L);
        tutor.setUser(tutorUser);
        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(77L);
        assignment.setTutor(tutor);
        assignment.setApplication(application);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);

        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setIssueType(ClassIssueType.TUTOR_ABSENT);
        request.setRequestedAction(ClassIssueRequestedAction.ESCALATE_DISPUTE);
        request.setDescription("Gia sư muốn chuyển sự cố lớp trung tâm thành tranh chấp tài chính");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(99L)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        99L, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));

        assertThrows(ForbiddenException.class, () -> disputeService.createClassIssue(request));

        verify(reportRepository, never()).save(any(Report.class));
        verify(escrowService, never()).holdForDispute(any(), any());
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    /** Sheet createClassIssue - UTCID02 (N): requestedAction = ESCALATE_DISPUTE -> leo thang thành tranh chấp và giữ escrow */
    @Test
    void createClassIssueEscalatesRefundReviewToDisputeAndHoldsEscrow() {
        User reporter = new User();
        reporter.setUserId(USER_ID);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setCreator(reporter);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(12L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(reporter);

        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setClassStudent(classStudent);

        Report savedReport = report(
                21L,
                reporter,
                ReportTargetType.CLASS,
                99L,
                ReportCategory.FRAUD,
                "[UC-29] Báo cáo sự cố lớp học");
        Dispute savedDispute = dispute(savedReport, escrow, 31L, DisputeStatus.OPEN);

        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setIssueType(ClassIssueType.PAYMENT_OR_REFUND);
        request.setRequestedAction(ClassIssueRequestedAction.REFUND_REVIEW);
        request.setDescription("Cần xem xét hoàn tiền vì lớp không diễn ra theo cam kết");
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(tutoringClassRepository.findById(99L)).thenReturn(Optional.of(tutoringClass));
        when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                ReportTargetType.CLASS,
                99L,
                ReportStatus.PENDING))
                .thenReturn(List.of());
        when(escrowTransactionRepository.findByAssignment_Application_TutoringClass_ClassId(99L))
                .thenReturn(List.of());
        when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(99L))
                .thenReturn(List.of(escrow));
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
        when(escrowService.holdForDispute(11L, "Cần xem xét hoàn tiền vì lớp không diễn ra theo cam kết"))
                .thenReturn(escrow);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(savedDispute);

        DisputeResponse response = disputeService.createClassIssue(request);

        assertEquals(Boolean.TRUE, response.getEscalatedToDispute());
        assertEquals(31L, response.getDisputeId());
        assertEquals(21L, response.getReportId());
        assertEquals(11L, response.getEscrowId());
        assertEquals(ReportTargetType.CLASS, response.getTargetType());
        verify(escrowService).holdForDispute(11L, "Cần xem xét hoàn tiền vì lớp không diễn ra theo cam kết");
        verify(disputeRepository).save(any(Dispute.class));
    }

    /** Bổ sung ngoài các UTCID của sheet createClassIssue: gia sư xin chấm dứt khi escrow đã có payout lưu sẵn */
    @Test
    void createClassIssueAllowsTutorTerminationWithoutSubmittedPayoutWhenEscrowHasSavedPayout() {
        User tutorUser = user(USER_ID, "tutor@tcs.com");
        User clientUser = user(101L, "client@tcs.com");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setCreator(clientUser);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);

        ClassAssignment assignment = assignment(77L, tutorUser, tutoringClass);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        assignment.setTermsB("""
                Gia sư dạy Toán 12, thanh toán qua ký quỹ.

                Thông tin nhận hoàn tiền:
                - Tên chủ tài khoản: Nguyễn Thu Hà
                - Ngân hàng: TPBank
                - Số tài khoản: 0123456789
                """);

        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setAssignment(assignment);
        escrow.setPayment(payment(55L, clientUser));

        Report savedReport = report(
                21L,
                tutorUser,
                ReportTargetType.CLASS,
                99L,
                ReportCategory.SPAM,
                "[UC-29] Báo cáo sự cố lớp học");
        Dispute savedDispute = dispute(savedReport, escrow, 31L, DisputeStatus.OPEN);

        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setAssignmentId(77L);
        request.setIssueType(ClassIssueType.SCHEDULE_CONFLICT);
        request.setRequestedAction(ClassIssueRequestedAction.TERMINATE_CLASS);
        request.setDescription("Gia sư đề nghị chấm dứt lớp vì lịch dạy không còn phù hợp");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(99L)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findById(77L)).thenReturn(Optional.of(assignment));
        when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                ReportTargetType.CLASS,
                99L,
                ReportStatus.PENDING))
                .thenReturn(List.of());
        when(escrowTransactionRepository.findByAssignment_AssignmentId(77L))
                .thenReturn(Optional.of(escrow));
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
        when(escrowService.holdForDispute(11L, "Gia sư đề nghị chấm dứt lớp vì lịch dạy không còn phù hợp"))
                .thenReturn(escrow);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(savedDispute);

        DisputeResponse response = disputeService.createClassIssue(request);

        assertEquals(Boolean.TRUE, response.getEscalatedToDispute());
        assertEquals(31L, response.getDisputeId());
        assertEquals(11L, response.getEscrowId());
        verify(escrowService)
                .holdForDispute(11L, "Gia sư đề nghị chấm dứt lớp vì lịch dạy không còn phù hợp");
        verify(disputeRepository).save(any(Dispute.class));
    }

    /** Sheet createDispute - UTCID03 (A): không truyền escrowId/assignmentId/classStudentId */
    @Test
    void createDisputeRejectsMissingEscrowSelector() {
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTargetType(ReportTargetType.USER);
        request.setTargetId(88L);
        request.setCategory(ReportCategory.FRAUD);
        request.setDescription("Test");

        assertThrows(IllegalArgumentException.class, () -> disputeService.createDispute(request));
        verify(reportRepository, never()).save(any());
        verify(escrowService, never()).holdForDispute(any(), any());
    }

    /** Sheet createDispute - UTCID02 (A): escrow không thuộc về người mở tranh chấp */
    @Test
    void createDisputeRejectsEscrowThatDoesNotBelongToReporter() {
        User reporter = user(USER_ID, "reporter@tcs.com");
        User payer = user(101L, "payer@tcs.com");
        User otherStudentOwner = user(202L, "owner@tcs.com");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(12L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(otherStudentOwner);

        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setPayment(payment(55L, payer));
        escrow.setClassStudent(classStudent);

        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTargetType(ReportTargetType.CLASS);
        request.setTargetId(99L);
        request.setCategory(ReportCategory.FRAUD);
        request.setDescription("Có gian lận");
        request.setEscrowId(11L);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(escrowTransactionRepository.findById(11L)).thenReturn(Optional.of(escrow));

        assertThrows(ForbiddenException.class, () -> disputeService.createDispute(request));
        verify(reportRepository, never()).save(any());
        verify(escrowService, never()).holdForDispute(any(), any());
    }

    /** Ngoài phạm vi Report 5.1 (MethodList không có listDisputesForAdmin) - test bổ sung */
    @Test
    void listDisputesForAdminReturnsReviewConsoleItems() {
        stubAdminReviewer();
        User reporter = user(USER_ID, "reporter@tcs.com");
        User payer = user(101L, "payer@tcs.com");
        User tutorUser = user(202L, "tutor@tcs.com");
        User creator = user(303L, "creator@tcs.com");

        TutoringClass tutoringClass = tutoringClass(99L, creator);
        ClassAssignment assignment = assignment(7L, tutorUser, tutoringClass);
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setAssignment(assignment);
        escrow.setPayment(payment(55L, payer));

        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        report.setEvidenceUrls("https://example.com/a\nhttps://example.com/b");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.UNDER_INVESTIGATION);

        ClassTerminationRequest termination = terminationRequest(88L, assignment, reporter);

        when(disputeRepository.findAll(any(Sort.class))).thenReturn(List.of(dispute));
        when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(termination));

        List<AdminDisputeReviewResponse> responses = disputeService.listDisputesForAdmin(null);

        assertEquals(1, responses.size());
        AdminDisputeReviewResponse response = responses.get(0);
        assertEquals(31L, response.getDisputeId());
        assertEquals(DisputeStatus.UNDER_INVESTIGATION, response.getDisputeStatus());
        assertEquals("reporter@tcs.com", response.getReporterEmail());
        assertEquals(List.of("https://example.com/a", "https://example.com/b"), response.getEvidenceUrlList());
        assertNotNull(response.getEscrow());
        assertEquals(303L, response.getEscrow().getPayerUserId());
        assertEquals("ESCROW_LOCK-A7", response.getEscrow().getPaymentReferenceCode());
        assertNotNull(response.getTutoringClass());
        assertEquals(99L, response.getTutoringClass().getClassId());
        assertEquals("tutor@tcs.com", response.getTutoringClass().getTutorEmail());
        assertNotNull(response.getTerminationRequest());
        assertEquals(88L, response.getTerminationRequest().getTerminationId());
        assertEquals(ClassTerminationStatus.PENDING, response.getTerminationRequest().getStatus());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
    }

    /** Ngoài phạm vi Report 5.1 (MethodList không có getDisputeForAdmin) - test bổ sung */
    @Test
    void getDisputeForAdminReturnsDetailById() {
        stubAdminReviewer();
        User reporter = user(USER_ID, "reporter@tcs.com");
        User payer = user(101L, "payer@tcs.com");

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setTitle("Lớp toán");
        tutoringClass.setStatus(TutoringClassStatus.DISPUTED);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(12L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(payer);
        classStudent.setStudentName("Học viên A");

        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setClassStudent(classStudent);
        escrow.setPayment(payment(55L, payer));

        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.ABUSE, "Lớp có vấn đề");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.OPEN);

        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        AdminDisputeReviewResponse response = disputeService.getDisputeForAdmin(31L);

        assertEquals(31L, response.getDisputeId());
        assertEquals(11L, response.getEscrow().getEscrowId());
        assertEquals(12L, response.getEscrow().getClassStudentId());
        assertEquals("Học viên A", response.getTutoringClass().getStudentName());
        assertEquals(101L, response.getTutoringClass().getEnrolledByUserId());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
    }

    /** Ngoài phạm vi Report 5.1 (MethodList không có getDisputeForAdmin) - test bổ sung */
    @Test
    void getDisputeForAdminRejectsMissingId() {
        assertThrows(IllegalArgumentException.class, () -> disputeService.getDisputeForAdmin(null));
        verify(disputeRepository, never()).findById(any());
    }

    /** Sheet resolveDispute - UTCID01 (N): đóng tranh chấp bằng kết luận -> đánh dấu dispute và report đã xử lý */
    @Test
    void resolveDisputeMarksDisputeAndReportResolved() {
        stubAdminReviewer();
        User reporter = user(USER_ID, "reporter@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.UNDER_INVESTIGATION);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setStatus(DisputeStatus.RESOLVED);
        request.setResolution("Chấp nhận khiếu nại và chuyển sang bước giải ngân");

        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        AdminDisputeReviewResponse response = disputeService.resolveDispute(31L, request);

        assertEquals(DisputeStatus.RESOLVED, response.getDisputeStatus());
        assertEquals(ReportStatus.RESOLVED, response.getReportStatus());
        assertEquals("Chấp nhận khiếu nại và chuyển sang bước giải ngân", response.getResolution());
        assertEquals(DisputeStatus.RESOLVED, dispute.getStatus());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER);
        verify(reportRepository).save(report);
        verify(escrowService, never()).apply(any());
        verify(disputeRepository).save(dispute);
    }

    /** Sheet resolveDispute - UTCID04 (N): chấm dứt lớp có nêu số tiền -> duyệt yêu cầu chấm dứt và kết thúc hợp đồng */
    @Test
    void resolveDisputeWithTerminationRequestApprovesTerminationAndTerminatesContract() {
        stubAdminReviewer();
        User reporter = user(USER_ID, "reporter@tcs.com");
        User tutorUser = user(202L, "tutor@tcs.com");
        User creator = user(303L, "creator@tcs.com");
        TutoringClass tutoringClass = tutoringClass(99L, creator);
        ClassAssignment assignment = assignment(7L, tutorUser, tutoringClass);
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setAssignment(assignment);
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.UNDER_INVESTIGATION);
        ClassTerminationRequest termination = terminationRequest(88L, assignment, reporter);
        Contract contract = new Contract();
        contract.setContractId(66L);
        contract.setAssignment(assignment);
        contract.setStatus(ContractStatus.ACTIVE);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setStatus(DisputeStatus.RESOLVED);
        request.setResolution("Đồng ý chấm dứt hợp đồng và giải ngân cho gia sư");

        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(termination));
        when(contractRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.of(contract));
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        disputeService.resolveDispute(31L, request);

        verify(escrowService, never()).apply(any());
        assertEquals(ClassTerminationStatus.APPROVED, termination.getStatus());
        assertNotNull(termination.getProcessedAt());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        verify(classTerminationRequestRepository).save(termination);
        verify(classAssignmentRepository).save(assignment);
        verify(contractRepository).save(contract);
    }

    /** Sheet resolveDispute - UTCID02 (N): tiếp tục lớp -> trả escrow đang giữ về trạng thái cũ */
    @Test
    void resolveDisputeContinueClassRestoresHeldEscrowAndClass() {
        stubAdminReviewer();
        User reporter = user(USER_ID, "reporter@tcs.com");
        User tutorUser = user(202L, "tutor@tcs.com");
        User creator = user(303L, "creator@tcs.com");
        TutoringClass tutoringClass = tutoringClass(99L, creator);
        ClassAssignment assignment = assignment(7L, tutorUser, tutoringClass);
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setAssignment(assignment);
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.ABUSE, "Lớp có vấn đề");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.UNDER_INVESTIGATION);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setAction(DisputeResolutionAction.CONTINUE_CLASS);
        request.setResolution("Sự cố đã được xử lý, lớp tiếp tục theo lịch học");

        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.empty());
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        AdminDisputeReviewResponse response = disputeService.resolveDispute(31L, request);

        assertEquals(DisputeStatus.RESOLVED, response.getDisputeStatus());
        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(EscrowStatus.FUNDED, escrow.getStatus());
        assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
        verify(escrowTransactionRepository).save(escrow);
        verify(tutoringClassRepository).save(tutoringClass);
        verify(escrowService, never()).apply(any());
    }

    /** Sheet resolveDispute - UTCID03 (N): hoàn tiền một phần -> tất toán escrow và hoàn tất chấm dứt */
    @Test
    void resolveDisputePartialRefundSettlesEscrowAndCompletesTermination() {
        stubAdminReviewer();
        User admin = user(900L, "admin@tcs.com");
        User reporter = user(USER_ID, "reporter@tcs.com");
        User tutorUser = user(202L, "tutor@tcs.com");
        User creator = user(303L, "creator@tcs.com");
        TutoringClass tutoringClass = tutoringClass(99L, creator);
        ClassAssignment assignment = assignment(7L, tutorUser, tutoringClass);
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setAssignment(assignment);
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.UNDER_INVESTIGATION);
        ClassTerminationRequest termination = terminationRequest(88L, assignment, reporter);
        Contract contract = new Contract();
        contract.setContractId(66L);
        contract.setAssignment(assignment);
        contract.setStatus(ContractStatus.ACTIVE);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setAction(DisputeResolutionAction.APPROVE_PARTIAL_REFUND);
        request.setReleaseToBeneficiary(new BigDecimal("70000.00"));
        request.setRefundToPayer(new BigDecimal("30000.00"));
        request.setResolution("Hoàn một phần theo số buổi chưa học và chấm dứt hợp đồng");
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));

        when(authHelper.currentUserId()).thenReturn(900L);
        when(userRepository.findById(900L)).thenReturn(Optional.of(admin));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(termination));
        when(contractRepository.findByAssignment_AssignmentId(7L)).thenReturn(Optional.of(contract));
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        disputeService.resolveDispute(31L, request);

        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        ReleaseInstruction instruction = instructionCaptor.getValue();
        assertEquals(11L, instruction.escrowId());
        assertEquals(new BigDecimal("70000.00"), instruction.releaseToBeneficiary());
        assertEquals(new BigDecimal("30000.00"), instruction.refundToPayer());
        assertEquals(ClassTerminationStatus.COMPLETED, termination.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());
        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());
        assertEquals(ContractStatus.TERMINATED, contract.getStatus());
        verify(classTerminationRequestRepository).save(termination);
        verify(classAssignmentRepository).save(assignment);
        verify(tutoringClassRepository).save(tutoringClass);
        verify(contractRepository).save(contract);
    }

    /** Sheet resolveDispute - UTCID05 (N): chấm dứt lớp không nêu số tiền -> tính pro rata theo số buổi đã học */
    @Test
    void resolveDisputeTerminateClassUsesProRataSettlementWhenAmountsAreBlank() {
        stubAdminReviewer();
        User admin = user(900L, "admin@tcs.com");
        User reporter = user(USER_ID, "reporter@tcs.com");
        User tutorUser = user(202L, "tutor@tcs.com");
        User creator = user(303L, "creator@tcs.com");
        TutoringClass tutoringClass = tutoringClass(99L, creator);
        tutoringClass.setNumberOfSessions(10);
        ClassAssignment assignment = assignment(7L, tutorUser, tutoringClass);
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setAssignment(assignment);
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.UNDER_INVESTIGATION);
        ClassTerminationRequest termination = terminationRequest(88L, assignment, reporter);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setAction(DisputeResolutionAction.TERMINATE_CLASS);
        request.setResolution("Chấm dứt lớp và chia tiền theo số buổi đã hoàn thành");
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));

        when(authHelper.currentUserId()).thenReturn(900L);
        when(userRepository.findById(900L)).thenReturn(Optional.of(admin));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(lessonRepository.findByTutoringClass_ClassId(99L))
                .thenReturn(List.of(
                        lesson(1L, AttendanceStatus.COMPLETED),
                        lesson(2L, AttendanceStatus.COMPLETED),
                        lesson(3L, AttendanceStatus.COMPLETED),
                        lesson(4L, AttendanceStatus.PENDING)));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(7L))
                .thenReturn(Optional.of(termination));
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        disputeService.resolveDispute(31L, request);

        ArgumentCaptor<ReleaseInstruction> instructionCaptor = ArgumentCaptor.forClass(ReleaseInstruction.class);
        verify(escrowService).apply(instructionCaptor.capture());
        ReleaseInstruction instruction = instructionCaptor.getValue();
        assertEquals(new BigDecimal("30000.00"), instruction.releaseToBeneficiary());
        assertEquals(new BigDecimal("70000.00"), instruction.refundToPayer());
        assertEquals(ClassTerminationStatus.COMPLETED, termination.getStatus());
        assertEquals(ClassAssignmentStatus.TERMINATED, assignment.getStatus());
        assertEquals(TutoringClassStatus.CANCELLED, tutoringClass.getStatus());
    }

    /** Sheet resolveDispute - UTCID06 (N): chuyển sang chờ bổ sung bằng chứng, chưa đóng report */
    @Test
    void resolveDisputeCanMoveToWaitingWithoutClosingReport() {
        stubAdminReviewer();
        User reporter = user(USER_ID, "reporter@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.OPEN);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setStatus(DisputeStatus.WAITING);
        request.setResolution("Cần người báo cáo bổ sung bằng chứng buổi học");

        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        AdminDisputeReviewResponse response = disputeService.resolveDispute(31L, request);

        assertEquals(DisputeStatus.WAITING, response.getDisputeStatus());
        assertEquals(ReportStatus.PENDING, response.getReportStatus());
        assertEquals("Cần người báo cáo bổ sung bằng chứng buổi học", response.getResolution());
        verify(reportRepository, never()).save(any(Report.class));
        verify(disputeRepository).save(dispute);
    }

    /** Ngoài phạm vi Report 5.1 (MethodList không có submitAdditionalEvidence) - test bổ sung */
    @Test
    void submitAdditionalEvidenceMovesWaitingDisputeBackToInvestigation() {
        User reporter = user(USER_ID, "client@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setPayment(payment(55L, reporter));
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        report.setEvidenceUrls("https://cdn.tcs.test/old-proof.png");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.WAITING);
        dispute.setResolution("Quyết định: Yêu cầu bổ sung bằng chứng");

        SubmitDisputeEvidenceRequest request = new SubmitDisputeEvidenceRequest();
        request.setEvidenceUrls("https://cdn.tcs.test/new-proof.png");
        request.setNote("Đã gửi thêm biên bản buổi học");

        when(authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN))
                .thenReturn(principal(reporter, UserRole.CLIENT));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        DisputeResponse response = disputeService.submitAdditionalEvidence(31L, request);

        assertEquals(DisputeStatus.UNDER_INVESTIGATION, response.getDisputeStatus());
        assertEquals(ReportStatus.PENDING, report.getStatus());
        assertEquals(
                "https://cdn.tcs.test/old-proof.png\nhttps://cdn.tcs.test/new-proof.png",
                report.getEvidenceUrls());
        assertEquals(
                "Quyết định: Yêu cầu bổ sung bằng chứng\n\nNgười dùng đã bổ sung bằng chứng: Đã gửi thêm biên bản buổi học",
                dispute.getResolution());
        verify(reportRepository).save(report);
        verify(disputeRepository).save(dispute);
        verify(platformAdminRepository).findAll();
    }

    /** Sheet resolveDispute - UTCID07 (A): tranh chấp đã được xử lý trước đó */
    @Test
    void resolveDisputeRejectsAlreadyResolvedDispute() {
        stubAdminReviewer();
        User reporter = user(USER_ID, "reporter@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.RELEASED);
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.RESOLVED);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setStatus(DisputeStatus.WAITING);
        request.setResolution("Mở lại để kiểm tra thêm bằng chứng");

        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        assertThrows(BusinessException.class, () -> disputeService.resolveDispute(31L, request));
        verify(disputeRepository, never()).save(any());
        verify(reportRepository, never()).save(any());
    }

    /** Sheet resolveDispute - UTCID08 (A): nội dung kết luận quá ngắn */
    @Test
    void resolveDisputeRejectsShortResolution() {
        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setStatus(DisputeStatus.RESOLVED);
        request.setResolution("Ngắn");

        assertThrows(IllegalArgumentException.class, () -> disputeService.resolveDispute(31L, request));
        verify(disputeRepository, never()).findById(any());
    }

    /** Sheet appealDispute - UTCID01 (N): tranh chấp đã xử lý, escrow chưa tất toán, đúng bên liên quan -> mở lại và giữ escrow */
    @Test
    void appealDisputeReopensResolvedDisputeAndHoldsEscrow() {
        User reporter = user(USER_ID, "client@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setPayment(payment(55L, reporter));
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        report.setStatus(ReportStatus.RESOLVED);
        report.setEvidenceUrls("https://cdn.tcs.test/old-proof.png");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.RESOLVED);
        dispute.setResolution("Đã chốt tranh chấp ban đầu");
        EscrowTransaction heldEscrow = escrow(11L, EscrowStatus.DISPUTED);
        heldEscrow.setPayment(escrow.getPayment());

        AppealDisputeRequest request = new AppealDisputeRequest();
        request.setReason("Bổ sung bằng chứng mới cần admin xem lại");
        request.setEvidenceUrls("https://cdn.tcs.test/new-proof.png");

        when(authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN))
                .thenReturn(principal(reporter, UserRole.CLIENT));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(escrowService.holdForDispute(11L, "Bổ sung bằng chứng mới cần admin xem lại")).thenReturn(heldEscrow);
        when(disputeRepository.save(dispute)).thenReturn(dispute);

        AdminDisputeReviewResponse response = disputeService.appealDispute(31L, request);

        assertEquals(DisputeStatus.UNDER_INVESTIGATION, response.getDisputeStatus());
        assertEquals(ReportStatus.PENDING, response.getReportStatus());
        assertEquals(EscrowStatus.DISPUTED, response.getEscrow().getStatus());
        assertEquals(
                "Đã chốt tranh chấp ban đầu\n\nMở lại tranh chấp: Bổ sung bằng chứng mới cần admin xem lại",
                response.getResolution());
        assertEquals(
                "https://cdn.tcs.test/old-proof.png\nhttps://cdn.tcs.test/new-proof.png",
                report.getEvidenceUrls());
        verify(escrowService).holdForDispute(11L, "Bổ sung bằng chứng mới cần admin xem lại");
        verify(reportRepository).save(report);
        verify(disputeRepository).save(dispute);
    }

    /** Sheet appealDispute - UTCID05 (A): người khiếu nại không phải bên liên quan */
    @Test
    void appealDisputeRejectsNonParticipant() {
        User reporter = user(USER_ID, "client@tcs.com");
        User otherUser = user(99L, "other@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setPayment(payment(55L, reporter));
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.RESOLVED);

        AppealDisputeRequest request = new AppealDisputeRequest();
        request.setReason("Tôi muốn mở lại tranh chấp này");

        when(authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN))
                .thenReturn(principal(otherUser, UserRole.CLIENT));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        assertThrows(ForbiddenException.class, () -> disputeService.appealDispute(31L, request));
        verify(escrowService, never()).holdForDispute(any(), any());
        verify(disputeRepository, never()).save(any());
        verify(reportRepository, never()).save(any());
    }

    /** Sheet appealDispute - UTCID09 (A): escrow đã tất toán */
    @Test
    void appealDisputeRejectsSettledEscrow() {
        User reporter = user(USER_ID, "client@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.RELEASED);
        escrow.setPayment(payment(55L, reporter));
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.RESOLVED);

        AppealDisputeRequest request = new AppealDisputeRequest();
        request.setReason("Bổ sung bằng chứng sau khi tranh chấp đã chốt");

        when(authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN))
                .thenReturn(principal(reporter, UserRole.CLIENT));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        assertThrows(BusinessException.class, () -> disputeService.appealDispute(31L, request));
        verify(escrowService, never()).holdForDispute(any(), any());
        verify(disputeRepository, never()).save(any());
        verify(reportRepository, never()).save(any());
    }

    private Report report(
            Long reportId,
            User reporter,
            ReportTargetType targetType,
            Long targetId,
            ReportCategory category,
            String description) {

        Report report = new Report();
        report.setReportId(reportId);
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setCategory(category);
        report.setDescription(description);
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(LocalDateTime.of(2026, 7, 15, 20, 0));
        return report;
    }

    private User user(Long userId, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        return user;
    }

    private UserPrincipal principal(User user, UserRole role) {
        return new UserPrincipal(user, role);
    }

    private void stubAdminReviewer() {
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(principal(user(900L, "admin@tcs.com"), UserRole.PLATFORM_ADMIN));
    }

    private TutoringClass tutoringClass(Long classId, User creator) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(classId);
        tutoringClass.setCreator(creator);
        tutoringClass.setTitle("Lớp toán");
        tutoringClass.setDescription("Lớp toán test");
        tutoringClass.setStatus(TutoringClassStatus.DISPUTED);
        return tutoringClass;
    }

    private ClassAssignment assignment(Long assignmentId, User tutorUser, TutoringClass tutoringClass) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(44L);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư test");

        TutorApplication application = new TutorApplication();
        application.setApplicationId(55L);
        application.setTutor(tutor);
        application.setTutoringClass(tutoringClass);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(assignmentId);
        assignment.setTutor(tutor);
        assignment.setApplication(application);
        return assignment;
    }

    private PaymentTransaction payment(Long transactionId, User payer) {
        Wallet wallet = new Wallet();
        wallet.setWalletId(payer.getUserId());
        wallet.setUser(payer);

        PaymentTransaction payment = new PaymentTransaction();
        payment.setTransactionId(transactionId);
        payment.setWallet(wallet);
        payment.setType(PaymentTransactionType.ESCROW_DEPOSIT);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        payment.setReferenceCode("ESCROW_LOCK-A7");
        return payment;
    }

    private ClassTerminationRequest terminationRequest(Long terminationId, ClassAssignment assignment, User requester) {
        ClassTerminationRequest request = new ClassTerminationRequest();
        request.setTerminationId(terminationId);
        request.setAssignment(assignment);
        request.setRequestedBy(requester);
        request.setReason("Dừng lớp sớm");
        request.setEffectiveDate(LocalDate.of(2026, 7, 20));
        request.setStatus(ClassTerminationStatus.PENDING);
        request.setCreatedAt(LocalDateTime.of(2026, 7, 17, 9, 0));
        return request;
    }

    private EscrowTransaction escrow(Long escrowId, EscrowStatus status) {
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setEscrowId(escrowId);
        escrow.setStatus(status);
        escrow.setAmount(new BigDecimal("100000.00"));
        return escrow;
    }

    private Lesson lesson(Long lessonId, AttendanceStatus status) {
        Lesson lesson = new Lesson();
        lesson.setLessonId(lessonId);
        lesson.setAttendanceStatus(status);
        return lesson;
    }

    private Dispute dispute(Report report, EscrowTransaction escrow, Long disputeId, DisputeStatus status) {
        Dispute dispute = new Dispute();
        dispute.setDisputeId(disputeId);
        dispute.setReport(report);
        dispute.setEscrowTransaction(escrow);
        dispute.setStatus(status);
        dispute.setCreatedAt(LocalDateTime.of(2026, 7, 15, 20, 1));
        return dispute;
    }

    // ===================================================================
    //  Sheet createDispute - UTCID04 (A) va sheet resolveDispute - UTCID09 (A)
    // ===================================================================

    /** Sheet createDispute - UTCID04 (A): request = null -> 'Thiếu thông tin tranh chấp'. */
    @Test
    void createDisputeRejectsNullRequest() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> disputeService.createDispute(null));
        assertEquals("Thiếu thông tin tranh chấp", ex.getMessage());
        verify(reportRepository, never()).save(any());
        verify(escrowService, never()).holdForDispute(any(), any());
    }

    /** Sheet resolveDispute - UTCID09 (A): request = null -> 'Thiếu thông tin quyết định xử lý tranh chấp'. */
    @Test
    void resolveDisputeRejectsNullRequest() {
        stubAdminReviewer();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> disputeService.resolveDispute(31L, null));
        assertEquals("Thiếu thông tin quyết định xử lý tranh chấp", ex.getMessage());
        verify(disputeRepository, never()).findById(any());
        verify(disputeRepository, never()).save(any());
    }

    // =====================================================================================
    //  Sheet: createClassIssue (bao cao su co lop hoc - UC-29)
    // =====================================================================================
    @org.junit.jupiter.api.Nested
    @org.junit.jupiter.api.DisplayName("createClassIssue")
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    class CreateClassIssue {

        private static final Long ISSUE_CLASS_ID = 99L;
        private static final String VALID_DESCRIPTION = "Gia su khong tham gia buoi hoc theo lich da hen";

        private User reporter;
        private TutoringClass tutoringClass;

        @org.junit.jupiter.api.BeforeEach
        void initClassIssue() {
            reporter = user(USER_ID, "nguoibaocao@tcs.com");
            tutoringClass = new TutoringClass();
            tutoringClass.setClassId(ISSUE_CLASS_ID);
            tutoringClass.setCreator(reporter);
            tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
            tutoringClass.setClassType(ClassType.PRIVATE);

            when(authHelper.currentUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
            when(tutoringClassRepository.findById(ISSUE_CLASS_ID)).thenReturn(Optional.of(tutoringClass));
            when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                    USER_ID, ReportTargetType.CLASS, ISSUE_CLASS_ID, ReportStatus.PENDING))
                    .thenReturn(List.of());
            when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
                Report r = invocation.getArgument(0);
                r.setReportId(21L);
                r.setCreatedAt(LocalDateTime.of(2026, 7, 15, 20, 0));
                return r;
            });
        }

        /** Yeu cau bao cao hop le, khong leo thang thanh tranh chap. */
        private CreateClassIssueRequest request() {
            CreateClassIssueRequest request = new CreateClassIssueRequest();
            request.setClassId(ISSUE_CLASS_ID);
            request.setIssueType(ClassIssueType.TUTOR_ABSENT);
            request.setRequestedAction(ClassIssueRequestedAction.RESCHEDULE);
            request.setLessonRef("Buoi 3");
            request.setOccurredAt(LocalDate.now().minusDays(1));
            request.setDescription(VALID_DESCRIPTION);
            return request;
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID01 (N) - Lop dang dien ra, nguoi bao cao la thanh vien, hanh dong khong leo thang -> chi tao Report")
        void utcid01_createReportOnly() {
            DisputeResponse response = disputeService.createClassIssue(request());

            assertFalse(response.getEscalatedToDispute());
            assertNull(response.getDisputeId());
            assertEquals(21L, response.getReportId());
            verify(escrowService, never()).holdForDispute(any(), any());
            verify(disputeRepository, never()).save(any(Dispute.class));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID02 (N) - requestedAction = ESCALATE_DISPUTE -> tao them tranh chap tren escrow")
        void utcid02_escalatesToDispute() {
            // Escrow gan voi ghi danh cua chinh nguoi bao cao trong lop nay.
            ClassStudent enrollment = new ClassStudent();
            enrollment.setClassStudentId(8L);
            enrollment.setTutoringClass(tutoringClass);
            enrollment.setEnrolledByUser(reporter);
            EscrowTransaction escrow = escrow(71L, EscrowStatus.FUNDED);
            escrow.setClassStudent(enrollment);

            CreateClassIssueRequest request = request();
            request.setRequestedAction(ClassIssueRequestedAction.ESCALATE_DISPUTE);
            request.setEscrowId(71L);

            when(escrowTransactionRepository.findById(71L)).thenReturn(Optional.of(escrow));
            when(escrowService.holdForDispute(org.mockito.ArgumentMatchers.eq(71L), any())).thenReturn(escrow);
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
                Dispute d = invocation.getArgument(0);
                d.setDisputeId(31L);
                return d;
            });

            DisputeResponse response = disputeService.createClassIssue(request);

            assertTrue(response.getEscalatedToDispute());
            assertEquals(31L, response.getDisputeId());
            verify(escrowService).holdForDispute(org.mockito.ArgumentMatchers.eq(71L), any());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID03 (A) - request = null -> 'Thiếu thông tin báo cáo lớp học'")
        void utcid03_nullRequest() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> disputeService.createClassIssue(null));
            assertEquals("Thiếu thông tin báo cáo lớp học", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID04 (A) - classId = null -> 'classId là bắt buộc'")
        void utcid04_nullClassId() {
            CreateClassIssueRequest request = request();
            request.setClassId(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> disputeService.createClassIssue(request));
            assertEquals("classId là bắt buộc", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID05 (A) - issueType = null -> 'Loại sự cố là bắt buộc'")
        void utcid05_nullIssueType() {
            CreateClassIssueRequest request = request();
            request.setIssueType(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> disputeService.createClassIssue(request));
            assertEquals("Loại sự cố là bắt buộc", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID06 (A) - requestedAction = null -> 'Hướng xử lý mong muốn là bắt buộc'")
        void utcid06_nullRequestedAction() {
            CreateClassIssueRequest request = request();
            request.setRequestedAction(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> disputeService.createClassIssue(request));
            assertEquals("Hướng xử lý mong muốn là bắt buộc", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID07 (A) - description rong -> 'Mô tả báo cáo là bắt buộc'")
        void utcid07_blankDescription() {
            CreateClassIssueRequest request = request();
            request.setDescription("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> disputeService.createClassIssue(request));
            assertEquals("Mô tả báo cáo là bắt buộc", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID08 (B) - description 19 ky tu (duoi can duoi) -> 'Mô tả báo cáo phải có ít nhất 20 ký tự'")
        void utcid08_descriptionOneCharTooShort() {
            CreateClassIssueRequest request = request();
            request.setDescription("a".repeat(19));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> disputeService.createClassIssue(request));
            assertEquals("Mô tả báo cáo phải có ít nhất 20 ký tự", ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID09 (B) - description dung 20 ky tu (dung can duoi) -> chap nhan")
        void utcid09_descriptionAtBoundary() {
            CreateClassIssueRequest request = request();
            request.setDescription("a".repeat(20));

            DisputeResponse response = disputeService.createClassIssue(request);

            assertEquals(21L, response.getReportId());
            verify(reportRepository).save(any(Report.class));
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID10 (A) - occurredAt o tuong lai -> 'Ngày xảy ra sự cố không được ở tương lai'")
        void utcid10_futureOccurredAt() {
            CreateClassIssueRequest request = request();
            request.setOccurredAt(LocalDate.now().plusDays(1));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> disputeService.createClassIssue(request));
            assertEquals("Ngày xảy ra sự cố không được ở tương lai", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID11 (A) - classId khong khop lop nao -> 'Không tìm thấy lớp học'")
        void utcid11_classNotFound() {
            when(tutoringClassRepository.findById(ISSUE_CLASS_ID)).thenReturn(Optional.empty());

            com.tcs.exception.ResourceNotFoundException ex = assertThrows(
                    com.tcs.exception.ResourceNotFoundException.class,
                    () -> disputeService.createClassIssue(request()));
            assertEquals("Không tìm thấy lớp học", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID12 (A) - Lop o trang thai DRAFT/OPEN/COMPLETED/CANCELLED -> khong cho bao cao")
        void utcid12_classStatusNotReportable() {
            tutoringClass.setStatus(TutoringClassStatus.COMPLETED);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createClassIssue(request()));
            assertEquals("Chỉ có thể báo cáo sự cố cho lớp đã ghép/đang diễn ra hoặc đang tranh chấp",
                    ex.getMessage());
            verify(reportRepository, never()).save(any());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID13 (A) - Nguoi bao cao khong phai thanh vien lop -> 'Bạn không có quyền báo cáo lớp học này'")
        void utcid13_reporterIsNotAParticipant() {
            User owner = user(555L, "chulop@tcs.com");
            tutoringClass.setCreator(owner);
            when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(
                    ISSUE_CLASS_ID, USER_ID)).thenReturn(false);
            when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                    org.mockito.ArgumentMatchers.eq(ISSUE_CLASS_ID), any())).thenReturn(List.of());

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> disputeService.createClassIssue(request()));
            assertEquals("Bạn không có quyền báo cáo lớp học này", ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID14 (A) - Gia su lop trung tam yeu cau hanh dong leo thang -> ForbiddenException")
        void utcid14_centerTutorCannotEscalate() {
            User centerUser = user(555L, "trungtam@tcs.com");
            tutoringClass.setCreator(centerUser);
            tutoringClass.setClassType(ClassType.CENTER);

            ClassAssignment tutorAssignment = assignment(7L, reporter, tutoringClass);
            tutorAssignment.setStatus(ClassAssignmentStatus.ACTIVE);
            when(classStudentRepository.existsByTutoringClass_ClassIdAndEnrolledByUser_UserId(
                    ISSUE_CLASS_ID, USER_ID)).thenReturn(false);
            when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                    org.mockito.ArgumentMatchers.eq(ISSUE_CLASS_ID), any()))
                    .thenReturn(List.of(tutorAssignment));

            CreateClassIssueRequest request = request();
            request.setRequestedAction(ClassIssueRequestedAction.ESCALATE_DISPUTE);
            request.setAssignmentId(7L);
            when(classAssignmentRepository.findById(7L)).thenReturn(Optional.of(tutorAssignment));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> disputeService.createClassIssue(request));
            assertEquals("Gia sư không thể tạo tranh chấp hoặc yêu cầu chấm dứt sớm cho lớp trung tâm",
                    ex.getMessage());
        }

        @Test
        @org.junit.jupiter.api.DisplayName("UTCID15 (A) - Da co bao cao PENDING cung loai su co -> chan tao trung")
        void utcid15_duplicatePendingReport() {
            Report existing = report(20L, reporter, ReportTargetType.CLASS, ISSUE_CLASS_ID,
                    ReportCategory.SPAM,
                    "[UC-29] Báo cáo sự cố lớp học\nMã loại sự cố: TUTOR_ABSENT\n");
            when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                    USER_ID, ReportTargetType.CLASS, ISSUE_CLASS_ID, ReportStatus.PENDING))
                    .thenReturn(List.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createClassIssue(request()));
            assertTrue(ex.getMessage().contains("đã có báo cáo sự cố cùng loại đang mở"),
                    "Phai bao trung loai su co: " + ex.getMessage());
            verify(reportRepository, never()).save(any());
        }
    }
    // =====================================================================
    //  Sheet: appealDispute - cac ca con lai
    // =====================================================================

    private AppealDisputeRequest appealRequest(String reason) {
        AppealDisputeRequest request = new AppealDisputeRequest();
        request.setReason(reason);
        return request;
    }

    private void givenAppealCaller() {
        User reporter = user(USER_ID, "client@tcs.com");
        when(authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN))
                .thenReturn(principal(reporter, UserRole.CLIENT));
    }

    /** Sheet appealDispute - UTCID02 (A): khong truyen disputeId -> 'disputeId là bắt buộc'. */
    @Test
    void appealDisputeRejectsMissingDisputeId() {
        givenAppealCaller();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> disputeService.appealDispute(null, appealRequest("Bổ sung bằng chứng mới")));
        assertEquals("disputeId là bắt buộc", ex.getMessage());
        verify(disputeRepository, never()).save(any());
    }

    /** Sheet appealDispute - UTCID03 (A): khong truyen noi dung khieu nai -> chan. */
    @Test
    void appealDisputeRejectsNullRequest() {
        givenAppealCaller();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> disputeService.appealDispute(31L, null));
        assertEquals("Thiếu thông tin khiếu nại/mở lại", ex.getMessage());
        verify(disputeRepository, never()).save(any());
    }

    /** Sheet appealDispute - UTCID04 (A): tranh chap chua duoc xu ly -> chua the khieu nai. */
    @Test
    void appealDisputeRejectsUnresolvedDispute() {
        givenAppealCaller();
        User reporter = user(USER_ID, "client@tcs.com");
        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setPayment(payment(55L, reporter));
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, escrow, 31L, DisputeStatus.UNDER_INVESTIGATION);
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.appealDispute(31L, appealRequest("Bổ sung bằng chứng mới cần xem lại")));
        assertEquals("Chỉ tranh chấp đã xử lý mới có thể khiếu nại/mở lại", ex.getMessage());
        verify(disputeRepository, never()).save(any());
    }

    /** Sheet appealDispute - UTCID06 (A): noi dung khieu nai rong. */
    @Test
    void appealDisputeRejectsBlankReason() {
        givenAppealCaller();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> disputeService.appealDispute(31L, appealRequest("   ")));
        assertEquals("Nội dung khiếu nại là bắt buộc", ex.getMessage());
        verify(disputeRepository, never()).save(any());
    }

    /** Sheet appealDispute - UTCID07 (B): ly do 9 ky tu (ngay duoi nguong 10). */
    @Test
    void appealDisputeRejectsTooShortReason() {
        givenAppealCaller();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> disputeService.appealDispute(31L, appealRequest("Khieu nai")));
        assertEquals("Nội dung khiếu nại phải có ít nhất 10 ký tự", ex.getMessage());
        verify(disputeRepository, never()).save(any());
    }

    /** Sheet appealDispute - UTCID08 (A): tranh chap khong gan voi escrow nao. */
    @Test
    void appealDisputeRejectsDisputeWithoutEscrow() {
        // Goi bang PLATFORM_ADMIN de bo qua buoc kiem tra ben lien quan (escrow = null),
        // nham cham dung nhanh ensureEscrowCanReopen.
        User adminCaller = user(USER_ID, "admin@tcs.com");
        when(authHelper.requireRole(
                UserRole.CLIENT,
                UserRole.TUTOR,
                UserRole.TUTOR_CENTER,
                UserRole.PLATFORM_ADMIN))
                .thenReturn(principal(adminCaller, UserRole.PLATFORM_ADMIN));
        User reporter = user(USER_ID, "client@tcs.com");
        Report report = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.FRAUD, "Có gian lận");
        Dispute dispute = dispute(report, null, 31L, DisputeStatus.RESOLVED);
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> disputeService.appealDispute(31L, appealRequest("Bổ sung bằng chứng mới cần xem lại")));
        assertEquals("Tranh chấp không có escrow để mở lại", ex.getMessage());
        verify(disputeRepository, never()).save(any());
    }
}
