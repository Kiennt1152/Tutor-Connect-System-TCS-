package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52DisputeServiceITTest {

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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    void SUPPORT_DISPUTE_CreateClassIssueWithoutFinancialEscalationDoesNotHoldEscrow() {
        User reporter = user(USER_ID, "client.it@tcs.test");
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
        verify(escrowService, never()).holdForDispute(any(), any());
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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

    @Test
    @Tag("report52-it")
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
