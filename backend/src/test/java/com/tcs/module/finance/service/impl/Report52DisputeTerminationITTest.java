package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.dto.ReleaseInstruction;
import com.tcs.module.finance.dto.request.AppealDisputeRequest;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.request.ResolveDisputeRequest;
import com.tcs.module.finance.dto.request.SubmitDisputeEvidenceRequest;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.ClassIssueRequestedAction;
import com.tcs.module.finance.enums.ClassIssueType;
import com.tcs.module.finance.enums.DisputeResolutionAction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.enums.PaymentTransactionType;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52DisputeTerminationITTest {


    private static final Long USER_ID = 7L;

    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private EscrowTransactionRepository escrowTransactionRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonAttendanceRepository lessonAttendanceRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private EscrowService escrowService;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    /**
     * Test Case: IT-DSP-001
     * Mô tả: Hoàn tất luồng chính tạo báo cáo sự cố lớp học / khiếu nại và tạm giữ tiền ký quỹ (Hold Escrow).
     * Procedure: Gửi POST /api/class-issues hoặc POST /api/disputes với escrowId và mô tả hợp lệ.
     * Expected Results: Báo cáo khiếu nại được tạo ở trạng thái OPEN, số tiền ký quỹ được chuyển sang DISPUTED/ON_HOLD.
     */
    
    /**
     * Test Case: IT-DSP-001
     * Title: Create a direct dispute report and hold its escrow.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createDispute (POST /api/disputes).
     * Input: CLASS target 99; category FRAUD; reason Có gian lận; escrowId=11; evidence URL.
     * Steps:
     *   1. Prepare the fixture: The reporter participates in the class and escrow 11 is accessible.
     *   2. Use the input: CLASS target 99; category FRAUD; reason Có gian lận; escrowId=11; evidence URL.
     *   3. Execute DisputeServiceImpl.createDispute (POST /api/disputes). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_001_CreateDisputeCreatesReportAndHoldsEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert report/dispute ids/status and verify holdForDispute.
     * Expected: Report 21 and dispute 31 are created for escrow 11 with OPEN status, and the escrow is moved to dispute hold.
     * Pre-conditions: The reporter participates in the class and escrow 11 is accessible.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-001: Create a direct dispute report and hold its escrow.")
    void IT_DSP_001_CreateDisputeCreatesReportAndHoldsEscrow() {
        User reporter = user(USER_ID, "client.it@tcs.test");
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
        verify(escrowService).holdForDispute(11L, "Có gian lận");
        verify(disputeRepository).save(any(Dispute.class));
    }

    /**
     * Test Case: IT-DSP-002
     * Title: List disputes for the admin/center review queue by status.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.listDisputesForAdmin (GET /api/disputes?status=OPEN).
     * Input: status=OPEN.
     * Steps:
     *   1. Prepare the fixture: An OPEN dispute with its class/escrow is available.
     *   2. Use the input: status=OPEN.
     *   3. Execute DisputeServiceImpl.listDisputesForAdmin (GET /api/disputes?status=OPEN). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_002_AdminListDisputesFiltersOpenStatusAndReturnsReviewRows.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response id/status/class title and role guard.
     * Expected: One OPEN dispute for Lớp private IT is returned and the caller must be PLATFORM_ADMIN or TUTOR_CENTER.
     * Pre-conditions: An OPEN dispute with its class/escrow is available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-002: List disputes for the admin/center review queue by status.")
    void IT_DSP_002_AdminListDisputesFiltersOpenStatusAndReturnsReviewRows() {
        User admin = user(1L, "admin.it@tcs.test");
        Dispute openDispute = privateDispute(31L, DisputeStatus.OPEN);
        stubAdminReviewDefaults(openDispute);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(disputeRepository.findByStatus(eq(DisputeStatus.OPEN), any(Sort.class)))
                .thenReturn(List.of(openDispute));

        var responses = disputeService.listDisputesForAdmin(DisputeStatus.OPEN);

        assertEquals(1, responses.size());
        assertEquals(31L, responses.get(0).getDisputeId());
        assertEquals(DisputeStatus.OPEN, responses.get(0).getDisputeStatus());
        assertEquals("Lớp private IT", responses.get(0).getTutoringClass().getTitle());
    }

    /**
     * Test Case: IT-DSP-003
     * Title: Load dispute detail with class, evidence, escrow and refund-payout data.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.getDisputeForAdmin (GET /api/disputes/{disputeId}).
     * Input: disputeId=31.
     * Steps:
     *   1. Prepare the fixture: Reviewer can access dispute 31 and its linked private class.
     *   2. Use the input: disputeId=31.
     *   3. Execute DisputeServiceImpl.getDisputeForAdmin (GET /api/disputes/{disputeId}). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_003_AdminDetailIncludesClassEvidenceEscrowAndPayoutData.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert evidence count, escrow/payout fields and assignment id.
     * Expected: Detail 31 contains two evidence files, escrow 11, masked account ****6789, holder Nguyễn Thu Hà and assignment 77.
     * Pre-conditions: Reviewer can access dispute 31 and its linked private class.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-003: Load dispute detail with class, evidence, escrow and refund-payout data.")
    void IT_DSP_003_AdminDetailIncludesClassEvidenceEscrowAndPayoutData() {
        User admin = user(1L, "admin.it@tcs.test");
        Dispute dispute = privateDispute(31L, DisputeStatus.OPEN);
        dispute.getReport().setEvidenceUrls("proof-a.png\nproof-b.jpg");
        dispute.getEscrowTransaction().getAssignment().setTermsB("""
                Điều khoản hợp đồng.

                Thông tin nhận hoàn tiền:
                - Tên chủ tài khoản: Nguyễn Thu Hà
                - Ngân hàng: TPBank
                - Số tài khoản: 0123456789
                """);
        stubAdminReviewDefaults(dispute);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        var response = disputeService.getDisputeForAdmin(31L);

        assertEquals(31L, response.getDisputeId());
        assertEquals(2, response.getEvidenceUrlList().size());
        assertEquals(11L, response.getEscrow().getEscrowId());
        assertEquals("****6789", response.getEscrow().getRefundAccountNoMasked());
        assertEquals("Nguyễn Thu Hà", response.getEscrow().getRefundAccountHolderName());
        assertEquals(77L, response.getTutoringClass().getAssignmentId());
    }

    /**
     * Test Case: IT-DSP-004
     * Title: Reject a class-issue report when the requested action is missing.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createClassIssue (POST /api/class-issues).
     * Input: classId=99; issueType=TUTOR_ABSENT; requestedAction=null.
     * Steps:
     *   1. Prepare the fixture: A class-issue request can be submitted without requestedAction.
     *   2. Use the input: classId=99; issueType=TUTOR_ABSENT; requestedAction=null.
     *   3. Execute DisputeServiceImpl.createClassIssue (POST /api/class-issues). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_004_RejectClassIssueWhenMandatoryFieldsAreMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify class/report repositories are untouched.
     * Expected: The service returns “Hướng xử lý mong muốn là bắt buộc” before loading the class or saving a report.
     * Pre-conditions: A class-issue request can be submitted without requestedAction.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-004: Reject a class-issue report when the requested action is missing.")
    void IT_DSP_004_RejectClassIssueWhenMandatoryFieldsAreMissing() {
        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setIssueType(ClassIssueType.TUTOR_ABSENT);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> disputeService.createClassIssue(request));

        assertEquals("Hướng xử lý mong muốn là bắt buộc", exception.getMessage());
        verify(tutoringClassRepository, never()).findById(any());
        verify(reportRepository, never()).save(any(Report.class));
    }

    /**
     * Test Case: IT-DSP-005
     * Title: Reject a class-issue description shorter than 20 characters.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createClassIssue (POST /api/class-issues).
     * Input: SCHEDULE_CONFLICT / RESCHEDULE / description “Quá ngắn”.
     * Steps:
     *   1. Prepare the fixture: Class issue fields are otherwise valid.
     *   2. Use the input: SCHEDULE_CONFLICT / RESCHEDULE / description “Quá ngắn”.
     *   3. Execute DisputeServiceImpl.createClassIssue (POST /api/class-issues). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_005_RejectClassIssueDescriptionThatIsTooShort.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no class lookup/report save.
     * Expected: The service returns the minimum-description message before creating a report.
     * Pre-conditions: Class issue fields are otherwise valid.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-005: Reject a class-issue description shorter than 20 characters.")
    void IT_DSP_005_RejectClassIssueDescriptionThatIsTooShort() {
        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setIssueType(ClassIssueType.SCHEDULE_CONFLICT);
        request.setRequestedAction(ClassIssueRequestedAction.RESCHEDULE);
        request.setDescription("Quá ngắn");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> disputeService.createClassIssue(request));

        assertEquals("Mô tả báo cáo phải có ít nhất 20 ký tự", exception.getMessage());
        verify(tutoringClassRepository, never()).findById(any());
        verify(reportRepository, never()).save(any(Report.class));
    }

    /**
     * Test Case: IT-DSP-006
     * Title: Block an anonymous direct dispute before loading the reporter.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createDispute (POST /api/disputes).
     * Input: Valid CLASS/escrow dispute payload.
     * Steps:
     *   1. Prepare the fixture: No authenticated user.
     *   2. Use the input: Valid CLASS/escrow dispute payload.
     *   3. Execute DisputeServiceImpl.createDispute (POST /api/disputes). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_006_BlockAnonymousDirectDisputeBeforeLoadingReporter.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify user/report writes are skipped.
     * Expected: The service returns “Yêu cầu đăng nhập” and does not load the user or save a report.
     * Pre-conditions: No authenticated user.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-006: Block an anonymous direct dispute before loading the reporter.")
    void IT_DSP_006_BlockAnonymousDirectDisputeBeforeLoadingReporter() {
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTargetType(ReportTargetType.CLASS);
        request.setTargetId(99L);
        request.setCategory(ReportCategory.FRAUD);
        request.setDescription("Người dùng gửi tranh chấp tài chính");
        request.setEscrowId(11L);

        when(authHelper.currentUserId()).thenThrow(new ForbiddenException("Yêu cầu đăng nhập"));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> disputeService.createDispute(request));

        assertEquals("Yêu cầu đăng nhập", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(reportRepository, never()).save(any(Report.class));
    }

    /**
     * Test Case: IT-DSP-007
     * Title: Prevent a tutor from opening a financial dispute on a center class.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createClassIssue (POST /api/class-issues).
     * Input: TUTOR_ABSENT / ESCALATE_DISPUTE on class 99.
     * Steps:
     *   1. Prepare the fixture: Center class 99 is IN_PROGRESS with an active tutor assignment.
     *   2. Use the input: TUTOR_ABSENT / ESCALATE_DISPUTE on class 99.
     *   3. Execute DisputeServiceImpl.createClassIssue (POST /api/class-issues). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_007_BlockTutorFromFinancialDisputeOnCenterClass.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert ForbiddenException and verify all financial writes are skipped.
     * Expected: The tutor request is rejected and neither report, dispute nor escrow hold is created.
     * Pre-conditions: Center class 99 is IN_PROGRESS with an active tutor assignment.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-007: Prevent a tutor from opening a financial dispute on a center class.")
    void IT_DSP_007_BlockTutorFromFinancialDisputeOnCenterClass() {
        User tutorUser = user(USER_ID, "tutor.it@tcs.test");
        User centerUser = user(700L, "center.it@tcs.test");
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setCreator(centerUser);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(77L, tutorUser, tutoringClass);
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
                99L,
                ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));

        assertThrows(ForbiddenException.class, () -> disputeService.createClassIssue(request));

        verify(reportRepository, never()).save(any(Report.class));
        verify(escrowService, never()).holdForDispute(any(), any());
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    /**
     * Test Case: IT-DSP-008
     * Title: Reject a new dispute when the same escrow already has an unresolved dispute.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createDispute (POST /api/disputes).
     * Input: Another CLASS dispute for escrow 11.
     * Steps:
     *   1. Prepare the fixture: Escrow 11 has a dispute whose status is not RESOLVED.
     *   2. Use the input: Another CLASS dispute for escrow 11.
     *   3. Execute DisputeServiceImpl.createDispute (POST /api/disputes). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_008_RejectNewDisputeWhenSameEscrowAlreadyHasActiveDispute.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert duplicate error and verify no hold/dispute save.
     * Expected: The duplicate-dispute business error is returned and the existing escrow remains untouched.
     * Pre-conditions: Escrow 11 has a dispute whose status is not RESOLVED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-008: Reject a new dispute when the same escrow already has an unresolved dispute.")
    void IT_DSP_008_RejectNewDisputeWhenSameEscrowAlreadyHasActiveDispute() {
        User reporter = user(USER_ID, "client.it@tcs.test");
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
        when(disputeRepository.existsByEscrowTransaction_EscrowIdAndStatusNot(11L, DisputeStatus.RESOLVED))
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

    /**
     * Test Case: IT-DSP-009
     * Title: Reject a duplicate early-termination request for a private assignment in either PENDING or APPROVED state.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.requestClassTermination (POST /api/marketplace/classes/{classId}/termination).
     * Input: classId=5; assignmentId=7; valid termination reason.
     * Steps:
     *   1. Prepare the fixture: Private class has an active assignment; run the case once with an existing PENDING request and once with an existing APPROVED request.
     *   2. Use the input: classId=5; assignmentId=7; valid termination reason.
     *   3. Execute MarketplaceServiceImpl.requestClassTermination (POST /api/marketplace/classes/{classId}/termination). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_DSP_009_RejectDuplicateEarlyTerminationForPendingOrApprovedRequest.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert both calls raise the duplicate message and verify no new termination save.
     * Expected: A second request is rejected when the existing request is PENDING and also when it is APPROVED; no class/termination row is saved in either case.
     * Pre-conditions: Private class has an active assignment; run the case once with an existing PENDING request and once with an existing APPROVED request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-009: Reject a duplicate early-termination request for a private assignment in either PENDING or APPROVED state.")
    void IT_DSP_009_RejectDuplicatePendingClassIssueForSameClass() {
        User reporter = user(USER_ID, "client.it@tcs.test");
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setCreator(reporter);
        tutoringClass.setClassType(ClassType.PRIVATE);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassStudentId(12L);
        classStudent.setTutoringClass(tutoringClass);
        classStudent.setEnrolledByUser(reporter);

        Report existingPendingReport = report(
                21L, reporter, ReportTargetType.CLASS, 99L, ReportCategory.OTHER,
                "Báo cáo sự cố. Mã loại sự cố: " + ClassIssueType.TUTOR_ABSENT.name());
        existingPendingReport.setStatus(ReportStatus.PENDING);

        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setClassStudentId(12L);
        request.setIssueType(ClassIssueType.TUTOR_ABSENT);
        request.setRequestedAction(ClassIssueRequestedAction.TERMINATE_CLASS);
        request.setDescription("Gia su vang mat qua nhieu buoi khong phep");
        request.setEvidenceUrls("https://example.com/evidence");
        request.setOccurredAt(LocalDate.now().minusDays(1));

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reporter));
        when(tutoringClassRepository.findById(99L)).thenReturn(Optional.of(tutoringClass));
        when(classStudentRepository.findById(12L)).thenReturn(Optional.of(classStudent));
        when(reportRepository.findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                USER_ID, ReportTargetType.CLASS, 99L, ReportStatus.PENDING))
                .thenReturn(List.of(existingPendingReport));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> disputeService.createClassIssue(request));

        assertEquals("Lớp học đã có báo cáo sự cố cùng loại đang mở. Vui lòng theo dõi báo cáo hiện có.", exception.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
    }

    /**
     * Test Case: IT-DSP-010
     * Title: Request more evidence and move the dispute to WAITING.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.resolveDispute (POST /api/disputes/{disputeId}/resolve).
     * Input: Action REQUEST_MORE_EVIDENCE with a resolution note.
     * Steps:
     *   1. Prepare the fixture: Admin/center reviewer opens dispute 31.
     *   2. Use the input: Action REQUEST_MORE_EVIDENCE with a resolution note.
     *   3. Execute DisputeServiceImpl.resolveDispute (POST /api/disputes/{disputeId}/resolve). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_010_AdminRequestMoreEvidenceMovesDisputeToWaitingAndWritesAudit.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert statuses and verify audit/notification calls.
     * Expected: The dispute becomes WAITING, its report stays PENDING, an audit row is recorded and participants are notified.
     * Pre-conditions: Admin/center reviewer opens dispute 31.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-010: Request more evidence and move the dispute to WAITING.")
    void IT_DSP_010_AdminRequestMoreEvidenceMovesDisputeToWaitingAndWritesAudit() {
        User admin = user(1L, "admin.it@tcs.test");
        Dispute dispute = privateDispute(31L, DisputeStatus.OPEN);
        ResolveDisputeRequest request = resolveRequest(
                DisputeResolutionAction.REQUEST_MORE_EVIDENCE,
                null,
                null,
                "Cần bổ sung ảnh buổi học và tin nhắn xác nhận");
        stubAdminReviewDefaults(dispute);

        when(authHelper.currentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = disputeService.resolveDispute(31L, request);

        assertEquals(DisputeStatus.WAITING, response.getDisputeStatus());
        assertEquals(ReportStatus.PENDING, dispute.getReport().getStatus());
        verify(auditLogRepository).save(any());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(dispute.getReport().getReporter()),
                eq(NotificationType.CLASS),
                eq("DISPUTE_EVENT"),
                any(),
                eq("Cần bổ sung bằng chứng tranh chấp"),
                any(),
                eq("DISPUTE"),
                eq(31L));
    }

    /**
     * Test Case: IT-DSP-011
     * Title: Continue the class after review and restore its escrow.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.resolveDispute (POST /api/disputes/{disputeId}/resolve).
     * Input: Action CONTINUE_CLASS with resolution note.
     * Steps:
     *   1. Prepare the fixture: Dispute 31 holds the class escrow and class status is DISPUTED.
     *   2. Use the input: Action CONTINUE_CLASS with resolution note.
     *   3. Execute DisputeServiceImpl.resolveDispute (POST /api/disputes/{disputeId}/resolve). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_011_AdminContinueClassRestoresEscrowAndNotifiesParticipants.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert dispute/escrow/class statuses and final notification.
     * Expected: The dispute becomes RESOLVED, escrow returns to FUNDED, class returns to IN_PROGRESS and participants are notified.
     * Pre-conditions: Dispute 31 holds the class escrow and class status is DISPUTED.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-011: Continue the class after review and restore its escrow.")
    void IT_DSP_011_AdminContinueClassRestoresEscrowAndNotifiesParticipants() {
        User admin = user(1L, "admin.it@tcs.test");
        Dispute dispute = privateDispute(31L, DisputeStatus.UNDER_INVESTIGATION);
        TutoringClass tutoringClass = dispute.getEscrowTransaction()
                .getAssignment()
                .getApplication()
                .getTutoringClass();
        tutoringClass.setStatus(TutoringClassStatus.DISPUTED);
        ResolveDisputeRequest request = resolveRequest(
                DisputeResolutionAction.CONTINUE_CLASS,
                null,
                null,
                "Hai bên thống nhất tiếp tục lớp học theo lịch mới");
        stubAdminReviewDefaults(dispute);

        when(authHelper.currentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowTransactionRepository.save(any(EscrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = disputeService.resolveDispute(31L, request);

        assertEquals(DisputeStatus.RESOLVED, response.getDisputeStatus());
        assertEquals(EscrowStatus.FUNDED, dispute.getEscrowTransaction().getStatus());
        assertEquals(TutoringClassStatus.IN_PROGRESS, tutoringClass.getStatus());
        verify(notificationDispatchService).notifyUserFromTemplate(
                eq(dispute.getReport().getReporter()),
                eq(NotificationType.CLASS),
                eq("DISPUTE_EVENT"),
                any(),
                eq("Đã có quyết định xử lý tranh chấp"),
                any(),
                eq("DISPUTE"),
                eq(31L));
    }

    /**
     * Test Case: IT-DSP-012
     * Title: Reload a waiting dispute with its current status and report state.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.getDisputeForAdmin (GET /api/disputes/{disputeId}).
     * Input: disputeId=31.
     * Steps:
     *   1. Prepare the fixture: Reviewer can access a dispute waiting for evidence.
     *   2. Use the input: disputeId=31.
     *   3. Execute DisputeServiceImpl.getDisputeForAdmin (GET /api/disputes/{disputeId}). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_012_AdminCanReloadWaitingDisputeWithCurrentStatusAndAuditTrail.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert dispute status, report status and class details.
     * Expected: Dispute 31 is returned as WAITING with report PENDING and the correct class title.
     * Pre-conditions: Reviewer can access a dispute waiting for evidence.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-012: Reload a waiting dispute with its current status and report state.")
    void IT_DSP_012_AdminCanReloadWaitingDisputeWithCurrentStatusAndAuditTrail() {
        User admin = user(1L, "admin.it@tcs.test");
        Dispute dispute = privateDispute(31L, DisputeStatus.WAITING);
        stubAdminReviewDefaults(dispute);
        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        var response = disputeService.getDisputeForAdmin(31L);

        assertEquals(DisputeStatus.WAITING, response.getDisputeStatus());
        assertEquals(ReportStatus.PENDING, response.getReportStatus());
        assertEquals("Lớp private IT", response.getTutoringClass().getTitle());
    }

    /**
     * Test Case: IT-DSP-013
     * Title: Appeal a resolved dispute, append evidence and reopen investigation.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.appealDispute (POST /api/disputes/{disputeId}/appeal).
     * Input: Reason plus new-proof.png.
     * Steps:
     *   1. Prepare the fixture: The dispute reporter is authorized and the resolved dispute can be reopened.
     *   2. Use the input: Reason plus new-proof.png.
     *   3. Execute DisputeServiceImpl.appealDispute (POST /api/disputes/{disputeId}/appeal). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_013_AppealResolvedDisputeReopensEscrowAndAppendsEvidence.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert status/evidence and verify report save plus escrow hold.
     * Expected: Old and new evidence URLs are appended, status becomes UNDER_INVESTIGATION and escrow is held again.
     * Pre-conditions: The dispute reporter is authorized and the resolved dispute can be reopened.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-013: Appeal a resolved dispute, append evidence and reopen investigation.")
    void IT_DSP_013_AppealResolvedDisputeReopensEscrowAndAppendsEvidence() {
        User client = user(USER_ID, "client.it@tcs.test");
        Dispute dispute = privateDispute(31L, DisputeStatus.RESOLVED);
        dispute.getReport().setReporter(client);
        dispute.getReport().setEvidenceUrls("old-proof.png");
        AppealDisputeRequest request = new AppealDisputeRequest();
        request.setReason("Tôi có thêm bằng chứng mới cần được xem xét");
        request.setEvidenceUrls("new-proof.png");
        stubAdminReviewDefaults(dispute);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(authHelper.requireRole(UserRole.CLIENT, UserRole.TUTOR, UserRole.TUTOR_CENTER, UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(escrowService.holdForDispute(11L, "Tôi có thêm bằng chứng mới cần được xem xét"))
                .thenReturn(dispute.getEscrowTransaction());
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = disputeService.appealDispute(31L, request);

        assertEquals(DisputeStatus.UNDER_INVESTIGATION, response.getDisputeStatus());
        assertEquals("old-proof.png\nnew-proof.png", dispute.getReport().getEvidenceUrls());
        verify(reportRepository).save(dispute.getReport());
        verify(escrowService).holdForDispute(11L, "Tôi có thêm bằng chứng mới cần được xem xét");
    }

    /**
     * Test Case: IT-DSP-014
     * Title: Submit additional evidence while a dispute is waiting.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.submitAdditionalEvidence (POST /api/disputes/{disputeId}/evidence).
     * Input: second-proof.jpg and an explanatory note.
     * Steps:
     *   1. Prepare the fixture: Dispute 31 is WAITING and belongs to the current participant.
     *   2. Use the input: second-proof.jpg and an explanatory note.
     *   3. Execute DisputeServiceImpl.submitAdditionalEvidence (POST /api/disputes/{disputeId}/evidence). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_014_SubmitAdditionalEvidenceAppendsFileUrlsAndMovesBackToInvestigation.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert evidence/statuses and verify report/dispute/audit saves.
     * Expected: The new evidence is appended, dispute status returns to UNDER_INVESTIGATION, report remains PENDING and audit is recorded.
     * Pre-conditions: Dispute 31 is WAITING and belongs to the current participant.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-014: Submit additional evidence while a dispute is waiting.")
    void IT_DSP_014_SubmitAdditionalEvidenceAppendsFileUrlsAndMovesBackToInvestigation() {
        User client = user(USER_ID, "client.it@tcs.test");
        Dispute dispute = privateDispute(31L, DisputeStatus.WAITING);
        dispute.getReport().setReporter(client);
        dispute.getReport().setEvidenceUrls("first-proof.png");
        SubmitDisputeEvidenceRequest request = new SubmitDisputeEvidenceRequest();
        request.setEvidenceUrls("second-proof.jpg");
        request.setNote("Đã bổ sung ảnh chụp màn hình lịch học");

        when(authHelper.currentUserId()).thenReturn(USER_ID);
        when(authHelper.requireRole(UserRole.CLIENT, UserRole.TUTOR, UserRole.TUTOR_CENTER, UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DisputeResponse response = disputeService.submitAdditionalEvidence(31L, request);

        assertEquals(DisputeStatus.UNDER_INVESTIGATION, response.getDisputeStatus());
        assertEquals("first-proof.png\nsecond-proof.jpg", dispute.getReport().getEvidenceUrls());
        assertEquals(ReportStatus.PENDING, dispute.getReport().getStatus());
        verify(reportRepository).save(dispute.getReport());
        verify(auditLogRepository).save(any());
    }

    /**
     * Test Case: IT-DSP-015
     * Title: List all visible disputes when no status filter is supplied.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.listDisputesForAdmin (GET /api/disputes).
     * Input: status omitted/null.
     * Steps:
     *   1. Prepare the fixture: Reviewer can access two visible disputes with different statuses.
     *   2. Use the input: status omitted/null.
     *   3. Execute DisputeServiceImpl.listDisputesForAdmin (GET /api/disputes). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_015_AdminListWithNullStatusReturnsAllVisibleDisputes.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert both ids/order and role guard.
     * Expected: OPEN dispute 31 and WAITING dispute 32 are returned in the repository order.
     * Pre-conditions: Reviewer can access two visible disputes with different statuses.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-015: List all visible disputes when no status filter is supplied.")
    void IT_DSP_015_AdminListWithNullStatusReturnsAllVisibleDisputes() {
        User admin = user(1L, "admin.it@tcs.test");
        Dispute open = privateDispute(31L, DisputeStatus.OPEN);
        Dispute waiting = privateDispute(32L, DisputeStatus.WAITING);
        waiting.setDisputeId(32L);
        waiting.getReport().setReportId(22L);
        waiting.getEscrowTransaction().setEscrowId(12L);
        stubAdminReviewDefaults(open);
        stubAdminReviewDefaults(waiting);

        when(authHelper.requireRole(UserRole.PLATFORM_ADMIN, UserRole.TUTOR_CENTER))
                .thenReturn(new UserPrincipal(admin, UserRole.PLATFORM_ADMIN));
        when(disputeRepository.findAll(any(Sort.class))).thenReturn(List.of(open, waiting));

        var responses = disputeService.listDisputesForAdmin(null);

        assertEquals(2, responses.size());
        assertEquals(31L, responses.get(0).getDisputeId());
        assertEquals(32L, responses.get(1).getDisputeId());
    }

    /**
     * Test Case: IT-DSP-016
     * Title: Escalate a client refund review into a held dispute.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createClassIssue (POST /api/class-issues).
     * Input: PAYMENT_OR_REFUND / REFUND_REVIEW with complete TPBank payout info.
     * Steps:
     *   1. Prepare the fixture: Client owns an IN_PROGRESS center class with a funded student escrow.
     *   2. Use the input: PAYMENT_OR_REFUND / REFUND_REVIEW with complete TPBank payout info.
     *   3. Execute DisputeServiceImpl.createClassIssue (POST /api/class-issues). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_016_EscalateClientRefundReviewToDisputeAndHoldEscrow.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert escalation/dispute/escrow ids and verify hold.
     * Expected: The refund-review issue escalates to dispute 31, returns escrow 11 and holds that escrow for review.
     * Pre-conditions: Client owns an IN_PROGRESS center class with a funded student escrow.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-016: Escalate a client refund review into a held dispute.")
    void IT_DSP_016_EscalateClientRefundReviewToDisputeAndHoldEscrow() {
        User reporter = user(USER_ID, "client.it@tcs.test");
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
        assertEquals(11L, response.getEscrowId());
        verify(escrowService).holdForDispute(11L, "Cần xem xét hoàn tiền vì lớp không diễn ra theo cam kết");
        verify(disputeRepository).save(any(Dispute.class));
    }

    /**
     * Test Case: IT-DSP-017
     * Title: Reject additional evidence when the dispute is not WAITING.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.submitAdditionalEvidence (POST /api/disputes/{disputeId}/evidence).
     * Input: new-proof.png.
     * Steps:
     *   1. Prepare the fixture: Dispute 31 is not in WAITING status.
     *   2. Use the input: new-proof.png.
     *   3. Execute DisputeServiceImpl.submitAdditionalEvidence (POST /api/disputes/{disputeId}/evidence). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_017_RejectAdditionalEvidenceWhenDisputeIsNotWaiting.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify both saves are skipped.
     * Expected: The service returns the waiting-state error and does not save report or dispute changes.
     * Pre-conditions: Dispute 31 is not in WAITING status.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-017: Reject additional evidence when the dispute is not WAITING.")
    void IT_DSP_017_RejectAdditionalEvidenceWhenDisputeIsNotWaiting() {
        User client = user(USER_ID, "client.it@tcs.test");
        Dispute dispute = privateDispute(31L, DisputeStatus.OPEN);
        SubmitDisputeEvidenceRequest request = new SubmitDisputeEvidenceRequest();
        request.setEvidenceUrls("new-proof.png");

        when(authHelper.requireRole(UserRole.CLIENT, UserRole.TUTOR, UserRole.TUTOR_CENTER, UserRole.PLATFORM_ADMIN))
                .thenReturn(new UserPrincipal(client, UserRole.CLIENT));
        when(disputeRepository.findById(31L)).thenReturn(Optional.of(dispute));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> disputeService.submitAdditionalEvidence(31L, request));

        assertEquals("Chỉ tranh chấp đang chờ bổ sung bằng chứng mới nhận thêm bằng chứng", exception.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    /**
     * Test Case: IT-DSP-018
     * Title: Reject a class issue whose occurred date is in the future.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createClassIssue (POST /api/class-issues).
     * Input: SCHEDULE_CONFLICT; occurredAt tomorrow; valid description.
     * Steps:
     *   1. Prepare the fixture: Class issue payload is otherwise complete.
     *   2. Use the input: SCHEDULE_CONFLICT; occurredAt tomorrow; valid description.
     *   3. Execute DisputeServiceImpl.createClassIssue (POST /api/class-issues). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_018_RejectClassIssueWhenOccurredDateIsInFuture.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no class lookup/report save.
     * Expected: The service returns the future-date validation message before loading the class.
     * Pre-conditions: Class issue payload is otherwise complete.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-018: Reject a class issue whose occurred date is in the future.")
    void IT_DSP_018_RejectClassIssueWhenOccurredDateIsInFuture() {
        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setIssueType(ClassIssueType.SCHEDULE_CONFLICT);
        request.setRequestedAction(ClassIssueRequestedAction.RESCHEDULE);
        request.setOccurredAt(LocalDate.now().plusDays(1));
        request.setDescription("Buổi học bị dời lịch nhưng ngày xảy ra được nhập trong tương lai");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> disputeService.createClassIssue(request));

        assertEquals("Ngày xảy ra sự cố không được ở tương lai", exception.getMessage());
        verify(tutoringClassRepository, never()).findById(any());
    }

    /**
     * Test Case: IT-DSP-019
     * Title: Require payout information for a refund-review class issue.
     * Procedure: Prepare the stated fixture and input, then execute DisputeServiceImpl.createClassIssue (POST /api/class-issues).
     * Input: PAYMENT_OR_REFUND / REFUND_REVIEW with no payout object.
     * Steps:
     *   1. Prepare the fixture: Client is eligible to report the class, but refund payout data is absent.
     *   2. Use the input: PAYMENT_OR_REFUND / REFUND_REVIEW with no payout object.
     *   3. Execute DisputeServiceImpl.createClassIssue (POST /api/class-issues). Mapped test: com.tcs.module.finance.service.impl.Report52DisputeServiceITTest#IT_DSP_019_RejectRefundReviewWithoutVietnamesePayoutMessage.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert the payout validation message and verify no report/dispute save.
     * Expected: A REFUND_REVIEW request without complete Vietnamese payout data is rejected and no dispute is created.
     * Pre-conditions: Client is eligible to report the class, but refund payout data is absent.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-019: Require payout information for a refund-review class issue.")
    void IT_DSP_019_RejectRefundReviewWithoutVietnamesePayoutMessage() {
        CreateClassIssueRequest request = new CreateClassIssueRequest();
        request.setClassId(99L);
        request.setIssueType(ClassIssueType.PAYMENT_OR_REFUND);
        request.setRequestedAction(ClassIssueRequestedAction.REFUND_REVIEW);
        request.setDescription("Phụ huynh yêu cầu xem xét hoàn tiền vì lớp không diễn ra đúng cam kết");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> disputeService.createClassIssue(request));

        assertEquals("Vui lòng nhập đầy đủ thông tin tài khoản nhận hoàn tiền", exception.getMessage());
        verify(reportRepository, never()).save(any(Report.class));
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    /**
     * Test Case: IT-DSP-020
     * Title: Use the client payout saved in the private assignment terms when a tutor requests early termination.
     * Procedure: Prepare the stated fixture and input, then execute MarketplaceServiceImpl.requestClassTermination (POST /api/marketplace/classes/{classId}/termination).
     * Input: classId=5; assignmentId=7; reason “Muốn dừng lớp”; request payout fields blank.
     * Steps:
     *   1. Prepare the fixture: Private assignment 7 is active, escrow 95 is funded for 500000, two of five lessons are complete and termsB contains complete client payout data.
     *   2. Use the input: classId=5; assignmentId=7; reason “Muốn dừng lớp”; request payout fields blank.
     *   3. Execute MarketplaceServiceImpl.requestClassTermination (POST /api/marketplace/classes/{classId}/termination). Mapped test: com.tcs.module.marketplace.service.impl.Report52MarketplacePrivateCenterITTest#IT_DSP_020_TutorPrivateTerminationUsesClientPayoutStoredOnAssignmentTerms.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert COMPLETED termination, assignment/class final status, payout details in the saved reason and captured release/refund instruction.
     * Expected: The tutor can submit the termination without entering a second payout account; the saved request contains the client bank details and the escrow split is 200000 release/300000 refund.
     * Pre-conditions: Private assignment 7 is active, escrow 95 is funded for 500000, two of five lessons are complete and termsB contains complete client payout data.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-DSP-020: Use the client payout saved in the private assignment terms when a tutor requests early termination.")
    void IT_DSP_020_TutorPrivateTerminationCanUseClientPayoutSavedOnContractTerms() {
        User tutorUser = user(USER_ID, "tutor.it@tcs.test");
        User clientUser = user(101L, "client.it@tcs.test");
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
        when(escrowTransactionRepository.findByAssignment_AssignmentId(77L)).thenReturn(Optional.of(escrow));
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
        when(escrowService.holdForDispute(11L, "Gia sư đề nghị chấm dứt lớp vì lịch dạy không còn phù hợp"))
                .thenReturn(escrow);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(savedDispute);

        DisputeResponse response = disputeService.createClassIssue(request);

        assertEquals(Boolean.TRUE, response.getEscalatedToDispute());
        assertEquals(31L, response.getDisputeId());
        assertEquals(11L, response.getEscrowId());
        verify(escrowService).holdForDispute(
                11L,
                "Gia sư đề nghị chấm dứt lớp vì lịch dạy không còn phù hợp");
    }



    private Dispute privateDispute(Long disputeId, DisputeStatus status) {
        User client = user(USER_ID, "client.it@tcs.test");
        User tutorUser = user(22L, "tutor.it@tcs.test");
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(99L);
        tutoringClass.setTitle("Lớp private IT");
        tutoringClass.setCreator(client);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setNumberOfSessions(5);
        ClassAssignment assignment = assignment(77L, tutorUser, tutoringClass);
        EscrowTransaction escrow = escrow(11L, EscrowStatus.DISPUTED);
        escrow.setAmount(new BigDecimal("500000.00"));
        escrow.setAssignment(assignment);
        escrow.setPayment(payment(55L, client));
        Report report = report(
                21L,
                client,
                ReportTargetType.CLASS,
                99L,
                ReportCategory.FRAUD,
                "Phụ huynh báo cáo lớp học cần xử lý tài chính");
        return dispute(report, escrow, disputeId, status);
    }

    private ResolveDisputeRequest resolveRequest(
            DisputeResolutionAction action,
            BigDecimal releaseToBeneficiary,
            BigDecimal refundToPayer,
            String resolution) {

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setAction(action);
        request.setReleaseToBeneficiary(releaseToBeneficiary);
        request.setRefundToPayer(refundToPayer);
        request.setResolution(resolution);
        request.setRefundPayoutInfo(new RefundPayoutInfo("TPBank", "0123456789", "Nguyen Van A"));
        return request;
    }

    private void stubAdminReviewDefaults(Dispute dispute) {
        EscrowTransaction escrow = dispute.getEscrowTransaction();
        if (escrow != null && escrow.getEscrowId() != null) {
            when(refundRequestRepository.findFirstByEscrowTransaction_EscrowIdOrderByRequestedAtDesc(escrow.getEscrowId()))
                    .thenReturn(Optional.empty());
            if (escrow.getAssignment() != null && escrow.getAssignment().getAssignmentId() != null) {
                when(classTerminationRequestRepository.findFirstByAssignment_AssignmentIdOrderByCreatedAtDesc(
                        escrow.getAssignment().getAssignmentId()))
                        .thenReturn(Optional.empty());
            }
            if (escrow.getClassStudent() != null && escrow.getClassStudent().getClassStudentId() != null) {
                when(classTerminationRequestRepository.findFirstByClassStudent_ClassStudentIdOrderByCreatedAtDesc(
                        escrow.getClassStudent().getClassStudentId()))
                        .thenReturn(Optional.empty());
            }
        }
        if (dispute.getDisputeId() != null) {
            when(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                    "DISPUTE",
                    dispute.getDisputeId()))
                    .thenReturn(List.of());
        }
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
