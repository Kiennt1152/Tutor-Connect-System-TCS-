package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    private TutoringClassRepository tutoringClassRepository;

    @Mock
    private ClassTerminationRequestRepository classTerminationRequestRepository;

    @Mock
    private EscrowService escrowService;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    @Test
    void createDisputeCreatesReportDisputeAndHoldsEscrow() {
        User reporter = new User();
        reporter.setUserId(USER_ID);

        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
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

    @Test
    void createClassIssueResolvesEscrowByClassAndHoldsIt() {
        User reporter = new User();
        reporter.setUserId(USER_ID);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(12L);
        classStudent.setTutoringClass(tutoringClass);

        EscrowTransaction escrow = escrow(11L, EscrowStatus.FUNDED);
        escrow.setClassStudent(classStudent);

        Report savedReport = report(21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.ABUSE, "Lớp có vấn đề");
        Dispute savedDispute = dispute(savedReport, escrow, 31L, DisputeStatus.OPEN);

        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setCategory(ReportCategory.ABUSE);
        request.setDescription("Lớp có vấn đề");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(tutoringClassRepository.existsById(99L)).thenReturn(true);
        when(escrowTransactionRepository.findByAssignment_Application_TutoringClass_ClassId(99L))
                .thenReturn(java.util.List.of());
        when(escrowTransactionRepository.findByClassStudent_TutoringClass_ClassId(99L))
                .thenReturn(java.util.List.of(escrow));
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
        when(escrowService.holdForDispute(11L, "Lớp có vấn đề")).thenReturn(escrow);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(savedDispute);

        DisputeResponse response = disputeService.createClassIssue(request);

        assertEquals(31L, response.getDisputeId());
        assertEquals(21L, response.getReportId());
        assertEquals(11L, response.getEscrowId());
        assertEquals(ReportTargetType.CLASS, response.getTargetType());
        verify(escrowService).holdForDispute(11L, "Lớp có vấn đề");
        verify(disputeRepository).save(any(Dispute.class));
    }

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

    @Test
    void listDisputesForAdminReturnsReviewConsoleItems() {
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
        assertEquals(101L, response.getEscrow().getPayerUserId());
        assertEquals("ESCROW_LOCK-A7", response.getEscrow().getPaymentReferenceCode());
        assertNotNull(response.getTutoringClass());
        assertEquals(99L, response.getTutoringClass().getClassId());
        assertEquals("tutor@tcs.com", response.getTutoringClass().getTutorEmail());
        assertNotNull(response.getTerminationRequest());
        assertEquals(88L, response.getTerminationRequest().getTerminationId());
        assertEquals(ClassTerminationStatus.PENDING, response.getTerminationRequest().getStatus());
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
    }

    @Test
    void getDisputeForAdminReturnsDetailById() {
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
        verify(authHelper).requireRole(UserRole.PLATFORM_ADMIN);
    }

    @Test
    void getDisputeForAdminRejectsMissingId() {
        assertThrows(IllegalArgumentException.class, () -> disputeService.getDisputeForAdmin(null));
        verify(disputeRepository, never()).findById(any());
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

    private Dispute dispute(Report report, EscrowTransaction escrow, Long disputeId, DisputeStatus status) {
        Dispute dispute = new Dispute();
        dispute.setDisputeId(disputeId);
        dispute.setReport(report);
        dispute.setEscrowTransaction(escrow);
        dispute.setStatus(status);
        dispute.setCreatedAt(LocalDateTime.of(2026, 7, 15, 20, 1));
        return dispute;
    }
}
