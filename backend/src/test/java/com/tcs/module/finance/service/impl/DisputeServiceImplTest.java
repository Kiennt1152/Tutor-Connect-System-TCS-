package com.tcs.module.finance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.entity.Dispute;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
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
