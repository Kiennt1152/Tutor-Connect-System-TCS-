package com.tcs.module.finance.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.finance.dto.request.SubmitDisputeEvidenceRequest;
import com.tcs.module.finance.dto.request.WithdrawDisputeRequest;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.enums.RefundRequestStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.RefundRequestRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.AuditLog;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.MediaFileRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantDisputeServiceTest {
    @Mock AuthHelper authHelper;
    @Mock UserRepository userRepository;
    @Mock ReportRepository reportRepository;
    @Mock DisputeRepository disputeRepository;
    @Mock EscrowTransactionRepository escrowTransactionRepository;
    @Mock RefundRequestRepository refundRequestRepository;
    @Mock NotificationDispatchService notificationDispatchService;
    @Mock PlatformAdminRepository platformAdminRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock TutoringClassRepository tutoringClassRepository;
    @Mock ClassAssignmentRepository classAssignmentRepository;
    @Mock ClassStudentRepository classStudentRepository;
    @Mock ClassTerminationRequestRepository classTerminationRequestRepository;
    @Mock LessonRepository lessonRepository;
    @Mock LessonAttendanceRepository lessonAttendanceRepository;
    @Mock ContractRepository contractRepository;
    @Mock EscrowService escrowService;
    @Mock MediaFileRepository mediaFileRepository;
    @InjectMocks DisputeServiceImpl service;

    @Test
    void IT_DISP_003_TutorExplanationKeepsOriginalEvidenceAndRecordsAuthor() {
        Dispute d = fixture();
        User tutor = d.getEscrowTransaction().getAssignment().getTutor().getUser();
        loginParticipant(tutor, UserRole.TUTOR);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        when(authHelper.currentUserId()).thenReturn(tutor.getUserId());
        when(userRepository.findById(tutor.getUserId())).thenReturn(Optional.of(tutor));
        MediaFile file = new MediaFile();
        file.setUploadedBy(tutor); file.setMimeType("image/png");
        when(mediaFileRepository.findFirstByFileUrl("/uploads/private/reply.png")).thenReturn(Optional.of(file));
        SubmitDisputeEvidenceRequest request = explanation();
        request.setEvidenceUrls("/uploads/private/reply.png");
        var result = service.submitExplanation(31L, request);
        assertEquals(DisputeStatus.UNDER_INVESTIGATION, result.getStatus());
        assertEquals(List.of("/uploads/private/original.png", "/uploads/private/reply.png"), result.getEvidenceUrls());
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(audit.capture());
        assertEquals(tutor, audit.getValue().getActor());
        assertTrue(audit.getValue().getNewValue().contains(request.getNote()));
        verify(escrowService, never()).apply(any());
    }

    @Test
    void unrelatedTutorCannotSubmitExplanation() {
        loginParticipant(user(999L), UserRole.TUTOR);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(fixture()));
        assertThrows(ForbiddenException.class, () -> service.submitExplanation(31L, explanation()));
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void cannotAttachAnotherUsersPrivateFile() {
        Dispute d = fixture();
        loginParticipant(user(2L), UserRole.TUTOR);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        MediaFile file = new MediaFile(); file.setUploadedBy(user(99L)); file.setMimeType("image/png");
        when(mediaFileRepository.findFirstByFileUrl("/uploads/private/other.png")).thenReturn(Optional.of(file));
        SubmitDisputeEvidenceRequest request = explanation(); request.setEvidenceUrls("/uploads/private/other.png");
        assertThrows(ForbiddenException.class, () -> service.submitExplanation(31L, request));
        assertEquals("/uploads/private/original.png", d.getReport().getEvidenceUrls());
    }

    @Test
    void resolvedCaseRejectsNewExplanation() {
        Dispute d = fixture(); d.setStatus(DisputeStatus.RESOLVED);
        loginParticipant(user(2L), UserRole.TUTOR);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        assertThrows(BusinessException.class, () -> service.submitExplanation(31L, explanation()));
    }

    @Test
    void IT_DISP_006_ReporterWithdrawsWithoutTransferringMoney() {
        Dispute d = fixture(); loginClient(1L);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        var result = service.withdrawDispute(31L, withdrawal());
        assertEquals(DisputeStatus.RESOLVED, result.getStatus());
        assertFalse(result.isCanWithdraw());
        assertEquals(ReportStatus.RESOLVED, d.getReport().getStatus());
        assertEquals(EscrowStatus.FUNDED, d.getEscrowTransaction().getStatus());
        assertEquals(TutoringClassStatus.IN_PROGRESS, d.getEscrowTransaction().getAssignment().getApplication().getTutoringClass().getStatus());
        verify(escrowService, never()).apply(any());
        verify(refundRequestRepository, never()).save(any());
    }

    @Test
    void tutorReporterCanWithdrawDispute() {
        Dispute d = fixture();
        d.getReport().setReporter(d.getEscrowTransaction().getAssignment().getTutor().getUser());
        loginParticipant(d.getReport().getReporter(), UserRole.TUTOR);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));

        var result = service.withdrawDispute(31L, withdrawal());

        assertEquals(DisputeStatus.RESOLVED, result.getStatus());
        assertFalse(result.isCanWithdraw());
        assertEquals(ReportStatus.RESOLVED, d.getReport().getStatus());
        assertEquals(EscrowStatus.FUNDED, d.getEscrowTransaction().getStatus());
        verify(escrowService, never()).apply(any());
    }

    @Test
    void clientCannotWithdrawSomeoneElsesReportEvenIfPayer() {
        Dispute d = fixture(); d.getReport().setReporter(user(2L)); loginClient(1L);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        assertThrows(ForbiddenException.class, () -> service.withdrawDispute(31L, withdrawal()));
        assertEquals(EscrowStatus.DISPUTED, d.getEscrowTransaction().getStatus());
    }

    @Test
    void approvedRefundPreventsWithdrawal() {
        Dispute d = fixture(); loginClient(1L);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        when(refundRequestRepository.existsByEscrowTransaction_EscrowIdAndStatus(eq(11L), any()))
                .thenAnswer(call -> call.getArgument(1) == RefundRequestStatus.APPROVED);
        assertThrows(BusinessException.class, () -> service.withdrawDispute(31L, withdrawal()));
        assertEquals(DisputeStatus.OPEN, d.getStatus());
    }

    @Test
    void resolvedOrPaidCaseCannotBeWithdrawn() {
        Dispute d = fixture(); d.setStatus(DisputeStatus.RESOLVED); loginClient(1L);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        assertThrows(BusinessException.class, () -> service.withdrawDispute(31L, withdrawal()));
        d.setStatus(DisputeStatus.OPEN); d.getEscrowTransaction().setStatus(EscrowStatus.RELEASED);
        assertThrows(BusinessException.class, () -> service.withdrawDispute(31L, withdrawal()));
    }

    @Test
    void withdrawalDoesNotClearAnotherCaseHold() {
        Dispute d = fixture(); loginClient(1L);
        when(disputeRepository.findForUpdate(31L)).thenReturn(Optional.of(d));
        when(disputeRepository.existsByEscrowTransaction_EscrowIdAndStatusNot(11L, DisputeStatus.RESOLVED)).thenReturn(true);
        when(disputeRepository.existsOtherOpenForClass(99L, 31L)).thenReturn(true);
        service.withdrawDispute(31L, withdrawal());
        assertEquals(EscrowStatus.DISPUTED, d.getEscrowTransaction().getStatus());
        assertEquals(TutoringClassStatus.DISPUTED, d.getEscrowTransaction().getAssignment().getApplication().getTutoringClass().getStatus());
    }

    @Test
    void fileReadRequiresExactEvidenceUrlOfParticipantsCase() {
        MediaFile file = new MediaFile(); file.setUploadedBy(user(1L));
        when(mediaFileRepository.findFirstByFileUrl("/uploads/private/original.png")).thenReturn(Optional.of(file));
        when(disputeRepository.findForParticipant(null, 2L)).thenReturn(List.of(fixture()));
        assertTrue(service.canReadDisputeEvidence("/uploads/private/original.png", 2L));
        assertFalse(service.canReadDisputeEvidence("/uploads/private/original.png-secret", 2L));
        assertFalse(service.canReadDisputeEvidence("/uploads/private/original.png", 999L));
    }

    @Test
    void forgedEvidenceLinkCannotExposeAnotherParticipantsPrivateDocument() {
        MediaFile file = new MediaFile(); file.setUploadedBy(user(2L));
        when(mediaFileRepository.findFirstByFileUrl("/uploads/private/original.png")).thenReturn(Optional.of(file));
        when(disputeRepository.findForParticipant(null, 1L)).thenReturn(List.of(fixture()));
        assertFalse(service.canReadDisputeEvidence("/uploads/private/original.png", 1L));
    }

    private void loginParticipant(User user, UserRole role) {
        when(authHelper.requireRole(UserRole.CLIENT, UserRole.TUTOR, UserRole.TUTOR_CENTER)).thenReturn(new UserPrincipal(user, role));
    }
    private void loginClient(Long id) {
        loginParticipant(user(id), UserRole.CLIENT);
    }
    private SubmitDisputeEvidenceRequest explanation() {
        SubmitDisputeEvidenceRequest r = new SubmitDisputeEvidenceRequest();
        r.setNote("Toi da xin phep nghi va de xuat lich day bu."); return r;
    }
    private WithdrawDisputeRequest withdrawal() {
        WithdrawDisputeRequest r = new WithdrawDisputeRequest();
        r.setReason("Hai ben da thong nhat hoc tiep theo lich cu."); return r;
    }
    private User user(Long id) { User u = new User(); u.setUserId(id); u.setEmail("user" + id + "@tcs.test"); return u; }
    private Dispute fixture() {
        TutoringClass cls = new TutoringClass(); cls.setClassId(99L); cls.setTitle("Test class"); cls.setCreator(user(1L)); cls.setStatus(TutoringClassStatus.DISPUTED);
        TutorApplication app = new TutorApplication(); app.setTutoringClass(cls);
        Tutor tutor = new Tutor(); tutor.setUser(user(2L));
        ClassAssignment assignment = new ClassAssignment(); assignment.setAssignmentId(7L); assignment.setTutor(tutor); assignment.setApplication(app);
        EscrowTransaction escrow = new EscrowTransaction(); escrow.setEscrowId(11L); escrow.setAssignment(assignment); escrow.setStatus(EscrowStatus.DISPUTED);
        Report report = new Report(); report.setReportId(21L); report.setReporter(user(1L)); report.setTargetType(ReportTargetType.CLASS); report.setTargetId(99L); report.setStatus(ReportStatus.PENDING); report.setDescription("Test dispute report"); report.setEvidenceUrls("/uploads/private/original.png");
        Dispute d = new Dispute(); d.setDisputeId(31L); d.setReport(report); d.setEscrowTransaction(escrow); d.setStatus(DisputeStatus.OPEN); return d;
    }
}
